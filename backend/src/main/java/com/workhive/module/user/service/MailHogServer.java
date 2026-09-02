package com.workhive.module.user.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-process Mock MailHog Server:
 * 1. Listens on TCP port 1025 for standard SMTP commands.
 * 2. Listens on HTTP port 8025 for MailHog Web UI and REST API (/api/v2/messages).
 * 3. Gracefully detects if an external MailHog container is already running.
 */
@Component
public class MailHogServer {

    private static final Logger log = LoggerFactory.getLogger(MailHogServer.class);

    @Value("${mailhog.smtp.port:1025}")
    private int smtpPort;

    @Value("${mailhog.http.port:8025}")
    private int httpPort;

    private ServerSocket smtpServerSocket;
    private HttpServer httpServer;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    private static final List<MailMessage> messages = new CopyOnWriteArrayList<>();

    @Data
    @Builder
    public static class MailMessage {
        private String id;
        private String from;
        private List<String> to;
        private String subject;
        private String bodyText;
        private String bodyHtml;
        private String rawContent;
        private Instant timestamp;
        private Map<String, String> headers;
    }

    @PostConstruct
    public void start() {
        threadPool = Executors.newCachedThreadPool();
        running = true;

        startSmtpServer();
        startHttpServer();
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            if (smtpServerSocket != null && !smtpServerSocket.isClosed()) {
                smtpServerSocket.close();
            }
        } catch (Exception ignored) {}

        if (httpServer != null) {
            httpServer.stop(0);
        }

        if (threadPool != null) {
            threadPool.shutdownNow();
        }
    }

    public static List<MailMessage> getReceivedMessages() {
        return Collections.unmodifiableList(messages);
    }

    public static void recordMessage(MailMessage message) {
        messages.add(0, message);
    }

    public static void clearAllMessages() {
        messages.clear();
    }

    private void startSmtpServer() {
        try {
            smtpServerSocket = new ServerSocket(smtpPort);
            log.info("📧 WorkHive MailHog SMTP Server started on port {}", smtpPort);

            threadPool.submit(() -> {
                while (running && !smtpServerSocket.isClosed()) {
                    try {
                        Socket client = smtpServerSocket.accept();
                        threadPool.submit(() -> handleSmtpSession(client));
                    } catch (Exception e) {
                        if (!running) break;
                    }
                }
            });
        } catch (Exception e) {
            log.info("Port {} in use (external MailHog / SMTP active): {}", smtpPort, e.getMessage());
        }
    }

    private void handleSmtpSession(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {

            writer.write("220 WorkHive MailHog SMTP Ready\r\n");
            writer.flush();

            String from = null;
            List<String> to = new ArrayList<>();
            StringBuilder data = new StringBuilder();
            boolean inData = false;

            String line;
            while ((line = reader.readLine()) != null) {
                if (inData) {
                    if (line.equals(".")) {
                        inData = false;
                        // Save message
                        MailMessage msg = parseRawMessage(from, to, data.toString());
                        messages.add(0, msg);
                        log.info("📬 Email received via SMTP for: {} | Subject: {}", to, msg.getSubject());
                        writer.write("250 OK: queued as " + msg.getId() + "\r\n");
                        writer.flush();
                        to = new ArrayList<>();
                        data = new StringBuilder();
                    } else {
                        data.append(line).append("\r\n");
                    }
                } else {
                    String upper = line.toUpperCase().trim();
                    if (upper.startsWith("HELO") || upper.startsWith("EHLO")) {
                        writer.write("250-localhost\r\n250-PIPELINING\r\n250 8BITMIME\r\n");
                        writer.flush();
                    } else if (upper.startsWith("MAIL FROM:")) {
                        from = extractEmail(line);
                        writer.write("250 OK\r\n");
                        writer.flush();
                    } else if (upper.startsWith("RCPT TO:")) {
                        String recipient = extractEmail(line);
                        to.add(recipient);
                        writer.write("250 OK\r\n");
                        writer.flush();
                    } else if (upper.equals("DATA")) {
                        inData = true;
                        writer.write("354 Start mail input; end with <CRLF>.<CRLF>\r\n");
                        writer.flush();
                    } else if (upper.equals("RSET")) {
                        to.clear();
                        data = new StringBuilder();
                        writer.write("250 OK\r\n");
                        writer.flush();
                    } else if (upper.equals("QUIT")) {
                        writer.write("221 Bye\r\n");
                        writer.flush();
                        break;
                    } else if (upper.equals("NOOP")) {
                        writer.write("250 OK\r\n");
                        writer.flush();
                    } else {
                        writer.write("250 OK\r\n");
                        writer.flush();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("SMTP session closed: {}", e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    private String extractEmail(String line) {
        int start = line.indexOf('<');
        int end = line.indexOf('>');
        if (start >= 0 && end > start) {
            return line.substring(start + 1, end).trim();
        }
        int colon = line.indexOf(':');
        if (colon >= 0) {
            return line.substring(colon + 1).trim();
        }
        return line.trim();
    }

    public static MailMessage parseRawMessage(String from, List<String> to, String raw) {
        Map<String, String> headers = new HashMap<>();
        String subject = "(No Subject)";
        String body = raw;

        int headerEnd = raw.indexOf("\r\n\r\n");
        if (headerEnd == -1) {
            headerEnd = raw.indexOf("\n\n");
        }

        if (headerEnd >= 0) {
            String headerPart = raw.substring(0, headerEnd);
            body = raw.substring(headerEnd).trim();

            for (String hLine : headerPart.split("\r?\n")) {
                int col = hLine.indexOf(':');
                if (col > 0) {
                    String k = hLine.substring(0, col).trim().toLowerCase();
                    String v = hLine.substring(col + 1).trim();
                    headers.put(k, v);
                    if (k.equals("subject")) {
                        subject = v;
                    }
                    if (k.equals("from") && from == null) {
                        from = v;
                    }
                    if (k.equals("to") && to.isEmpty()) {
                        to.add(v);
                    }
                }
            }
        }

        String html = body.contains("<html") || body.contains("<!DOCTYPE") ? body : null;
        String text = body;

        return MailMessage.builder()
                .id(UUID.randomUUID().toString())
                .from(from != null ? from : "noreply@workhive.internal")
                .to(to != null && !to.isEmpty() ? to : List.of("unknown@workhive.internal"))
                .subject(subject)
                .bodyText(text)
                .bodyHtml(html)
                .rawContent(raw)
                .timestamp(Instant.now())
                .headers(headers)
                .build();
    }

    private void startHttpServer() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);

            // 1. MailHog Web UI
            httpServer.createContext("/", new WebUiHandler());

            // 2. MailHog REST API v2 & v1
            httpServer.createContext("/api/v2/messages", new ApiMessagesHandler());
            httpServer.createContext("/api/v1/messages", new ApiMessagesHandler());
            httpServer.createContext("/api/v2/messages/delete", new ApiDeleteHandler());

            httpServer.setExecutor(threadPool);
            httpServer.start();
            log.info("📬 WorkHive MailHog Web UI running at http://localhost:{}", httpPort);
        } catch (Exception e) {
            log.info("Port {} in use (external MailHog Web UI active): {}", httpPort, e.getMessage());
        }
    }

    private static class WebUiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                messages.clear();
                sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
                return;
            }

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("<title>MailHog — WorkHive Local Email Testing</title>\n");
            html.append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">\n");
            html.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=Outfit:wght@600;700;800&family=JetBrains+Mono:wght@400;600&display=swap\" rel=\"stylesheet\">\n");
            html.append("<style>\n");
            html.append("* { box-sizing: border-box; margin: 0; padding: 0; }\n");
            html.append("body { font-family: 'Inter', sans-serif; background: #090d16; color: #e2e8f0; height: 100vh; display: flex; flex-direction: column; overflow: hidden; }\n");
            html.append(".navbar { background: #0f172a; border-bottom: 1px solid #1e293b; padding: 14px 24px; display: flex; justify-content: space-between; align-items: center; }\n");
            html.append(".brand { display: flex; align-items: center; gap: 10px; font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #fff; }\n");
            html.append(".badge { background: #6366f1; color: #fff; font-size: 11px; padding: 3px 8px; border-radius: 9999px; font-weight: 700; }\n");
            html.append(".actions { display: flex; gap: 10px; align-items: center; }\n");
            html.append(".btn { background: #1e293b; color: #94a3b8; border: 1px solid #334155; padding: 8px 14px; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; transition: all 0.15s; }\n");
            html.append(".btn:hover { background: #334155; color: #fff; }\n");
            html.append(".btn-danger { background: rgba(239, 68, 68, 0.15); color: #f87171; border-color: rgba(239, 68, 68, 0.3); }\n");
            html.append(".btn-danger:hover { background: #ef4444; color: #fff; }\n");
            html.append(".layout { display: flex; flex: 1; overflow: hidden; }\n");
            html.append(".sidebar { width: 380px; border-right: 1px solid #1e293b; background: #090d16; overflow-y: auto; display: flex; flex-direction: column; }\n");
            html.append(".search-box { padding: 12px 16px; border-bottom: 1px solid #1e293b; background: #0f172a; }\n");
            html.append(".search-input { width: 100%; background: #1e293b; border: 1px solid #334155; border-radius: 8px; padding: 8px 12px; color: #fff; font-size: 12px; outline: none; }\n");
            html.append(".email-list { list-style: none; flex: 1; overflow-y: auto; }\n");
            html.append(".email-item { padding: 14px 16px; border-bottom: 1px solid #1e293b; cursor: pointer; transition: background 0.15s; }\n");
            html.append(".email-item:hover, .email-item.active { background: #1e293b; }\n");
            html.append(".email-to { font-size: 13px; font-weight: 700; color: #38bdf8; display: flex; justify-content: space-between; margin-bottom: 4px; }\n");
            html.append(".email-time { font-size: 11px; color: #64748b; font-family: 'JetBrains Mono', monospace; font-weight: normal; }\n");
            html.append(".email-subject { font-size: 12px; color: #f1f5f9; font-weight: 600; margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n");
            html.append(".email-from { font-size: 11px; color: #94a3b8; }\n");
            html.append(".preview-panel { flex: 1; background: #090d16; overflow-y: auto; padding: 24px; display: flex; flex-direction: column; }\n");
            html.append(".preview-empty { margin: auto; text-align: center; color: #64748b; font-size: 14px; }\n");
            html.append(".preview-header { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 18px 24px; margin-bottom: 20px; }\n");
            html.append(".header-row { display: flex; margin-bottom: 8px; font-size: 13px; }\n");
            html.append(".header-label { width: 90px; color: #64748b; font-weight: 600; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }\n");
            html.append(".header-value { color: #f1f5f9; font-weight: 500; word-break: break-all; }\n");
            html.append(".preview-body { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 24px; flex: 1; overflow: auto; }\n");
            html.append(".raw-view { font-family: 'JetBrains Mono', monospace; font-size: 12px; line-height: 1.6; white-space: pre-wrap; color: #cbd5e1; }\n");
            html.append(".cta-box { background: rgba(99, 102, 241, 0.1); border: 1px solid rgba(99, 102, 241, 0.3); border-radius: 8px; padding: 14px; margin-top: 16px; }\n");
            html.append(".cta-btn { display: inline-block; background: #6366f1; color: #fff; text-decoration: none; padding: 10px 18px; border-radius: 8px; font-weight: 700; font-size: 13px; margin-top: 8px; }\n");
            html.append("</style>\n");
            html.append("</head>\n<body>\n");

            // Navbar
            html.append("<div class=\"navbar\">\n");
            html.append("  <div class=\"brand\"><span>📧 MailHog</span> <span class=\"badge\">WorkHive QA Inbox (" + messages.size() + ")</span></div>\n");
            html.append("  <div class=\"actions\">\n");
            html.append("    <button class=\"btn\" onclick=\"location.reload()\">🔄 Refresh</button>\n");
            html.append("    <button class=\"btn btn-danger\" onclick=\"clearEmails()\">🗑️ Clear Inbox</button>\n");
            html.append("  </div>\n");
            html.append("</div>\n");

            // Layout
            html.append("<div class=\"layout\">\n");
            html.append("  <div class=\"sidebar\">\n");
            html.append("    <div class=\"search-box\"><input type=\"text\" class=\"search-input\" id=\"search\" placeholder=\"Search by recipient, subject...\" oninput=\"filterEmails()\"></div>\n");
            html.append("    <ul class=\"email-list\" id=\"list\">\n");

            if (messages.isEmpty()) {
                html.append("      <li style=\"padding: 30px; text-align: center; color: #64748b; font-size: 13px;\">No emails received yet.<br><br>Send an employee invitation from WorkHive to see it appear here!</li>\n");
            } else {
                for (int i = 0; i < messages.size(); i++) {
                    MailMessage m = messages.get(i);
                    String toStr = String.join(", ", m.getTo());
                    html.append("      <li class=\"email-item ").append(i == 0 ? "active" : "").append("\" onclick=\"selectEmail(").append(i).append(")\" data-search=\"").append(toStr.toLowerCase()).append(" ").append(m.getSubject().toLowerCase()).append("\">\n");
                    html.append("        <div class=\"email-to\"><span>").append(escapeHtml(toStr)).append("</span><span class=\"email-time\">").append(m.getTimestamp().toString().substring(11, 19)).append("</span></div>\n");
                    html.append("        <div class=\"email-subject\">").append(escapeHtml(m.getSubject())).append("</div>\n");
                    html.append("        <div class=\"email-from\">From: ").append(escapeHtml(m.getFrom())).append("</div>\n");
                    html.append("      </li>\n");
                }
            }

            html.append("    </ul>\n");
            html.append("  </div>\n");

            // Preview Panel
            html.append("  <div class=\"preview-panel\" id=\"preview\">\n");
            if (messages.isEmpty()) {
                html.append("    <div class=\"preview-empty\">📬 Select an email from the left sidebar to preview its full content and invitation link.</div>\n");
            } else {
                MailMessage first = messages.get(0);
                renderEmailPreview(html, first);
            }
            html.append("  </div>\n");
            html.append("</div>\n");

            // JavaScript
            html.append("<script>\n");
            html.append("const emailData = ").append(buildJsonArray()).append(";\n");
            html.append("function selectEmail(idx) {\n");
            html.append("  document.querySelectorAll('.email-item').forEach((el, i) => {\n");
            html.append("    el.classList.toggle('active', i === idx);\n");
            html.append("  });\n");
            html.append("  const m = emailData[idx];\n");
            html.append("  if (!m) return;\n");
            html.append("  let html = `\n");
            html.append("    <div class='preview-header'>\n");
            html.append("      <div class='header-row'><div class='header-label'>To:</div><div class='header-value' style='color:#38bdf8; font-weight:700;'>${escapeHtml(m.to.join(', '))}</div></div>\n");
            html.append("      <div class='header-row'><div class='header-label'>From:</div><div class='header-value'>${escapeHtml(m.from)}</div></div>\n");
            html.append("      <div class='header-row'><div class='header-label'>Subject:</div><div class='header-value' style='font-weight:700; color:#fff;'>${escapeHtml(m.subject)}</div></div>\n");
            html.append("      <div class='header-row'><div class='header-label'>Received:</div><div class='header-value'>${m.timestamp}</div></div>\n");
            html.append("    </div>\n");
            html.append("    <div class='preview-body'>\n");
            html.append("      <pre class='raw-view'>${escapeHtml(m.bodyText)}</pre>\n");
            html.append("    </div>\n");
            html.append("  `;\n");
            html.append("  document.getElementById('preview').innerHTML = html;\n");
            html.append("}\n");
            html.append("function filterEmails() {\n");
            html.append("  const q = document.getElementById('search').value.toLowerCase();\n");
            html.append("  document.querySelectorAll('.email-item').forEach(el => {\n");
            html.append("    const txt = el.getAttribute('data-search') || '';\n");
            html.append("    el.style.display = txt.includes(q) ? 'block' : 'none';\n");
            html.append("  });\n");
            html.append("}\n");
            html.append("function clearEmails() {\n");
            html.append("  if (!confirm('Are you sure you want to clear all emails in MailHog?')) return;\n");
            html.append("  fetch('/api/v1/messages', { method: 'DELETE' }).then(() => location.reload());\n");
            html.append("}\n");
            html.append("function escapeHtml(str) {\n");
            html.append("  if (!str) return '';\n");
            html.append("  return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/\"/g, '&quot;');\n");
            html.append("}\n");
            html.append("// Auto refresh every 3 seconds if empty or on user idle\n");
            html.append("setInterval(() => {\n");
            html.append("  fetch('/api/v2/messages').then(r => r.json()).then(data => {\n");
            html.append("    if (data.total !== emailData.length) location.reload();\n");
            html.append("  });\n");
            html.append("}, 3000);\n");
            html.append("</script>\n");
            html.append("</body>\n</html>");

            sendResponse(exchange, 200, "text/html; charset=UTF-8", html.toString());
        }

        private void renderEmailPreview(StringBuilder html, MailMessage m) {
            html.append("    <div class=\"preview-header\">\n");
            html.append("      <div class=\"header-row\"><div class=\"header-label\">To:</div><div class=\"header-value\" style=\"color:#38bdf8; font-weight:700;\">").append(escapeHtml(String.join(", ", m.getTo()))).append("</div></div>\n");
            html.append("      <div class=\"header-row\"><div class=\"header-label\">From:</div><div class=\"header-value\">").append(escapeHtml(m.getFrom())).append("</div></div>\n");
            html.append("      <div class=\"header-row\"><div class=\"header-label\">Subject:</div><div class=\"header-value\" style=\"font-weight:700; color:#fff;\">").append(escapeHtml(m.getSubject())).append("</div></div>\n");
            html.append("      <div class=\"header-row\"><div class=\"header-label\">Received:</div><div class=\"header-value\">").append(m.getTimestamp().toString()).append("</div></div>\n");
            html.append("    </div>\n");
            html.append("    <div class=\"preview-body\">\n");
            html.append("      <pre class=\"raw-view\">").append(escapeHtml(m.getBodyText())).append("</pre>\n");
            html.append("    </div>\n");
        }

        private String buildJsonArray() {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < messages.size(); i++) {
                MailMessage m = messages.get(i);
                if (i > 0) json.append(",");
                json.append("{")
                        .append("\"id\":\"").append(m.getId()).append("\",")
                        .append("\"from\":\"").append(escapeJson(m.getFrom())).append("\",")
                        .append("\"to\":[\"").append(escapeJson(String.join("\",\"", m.getTo()))).append("\"],")
                        .append("\"subject\":\"").append(escapeJson(m.getSubject())).append("\",")
                        .append("\"bodyText\":\"").append(escapeJson(m.getBodyText())).append("\",")
                        .append("\"timestamp\":\"").append(m.getTimestamp().toString()).append("\"")
                        .append("}");
            }
            json.append("]");
            return json.toString();
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }

        private String escapeHtml(String s) {
            if (s == null) return "";
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }
    }

    private static class ApiMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                messages.clear();
                sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
                return;
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"total\": ").append(messages.size()).append(",\n");
            json.append("  \"count\": ").append(messages.size()).append(",\n");
            json.append("  \"start\": 0,\n");
            json.append("  \"items\": [\n");

            for (int i = 0; i < messages.size(); i++) {
                MailMessage m = messages.get(i);
                if (i > 0) json.append(",\n");
                json.append("    {\n");
                json.append("      \"ID\": \"").append(m.getId()).append("\",\n");
                json.append("      \"From\": { \"Address\": \"").append(escapeJson(m.getFrom())).append("\" },\n");
                json.append("      \"To\": [ { \"Address\": \"").append(escapeJson(String.join(",", m.getTo()))).append("\" } ],\n");
                json.append("      \"Content\": {\n");
                json.append("        \"Headers\": {\n");
                json.append("          \"Subject\": [\"").append(escapeJson(m.getSubject())).append("\"],\n");
                json.append("          \"To\": [\"").append(escapeJson(String.join(",", m.getTo()))).append("\"],\n");
                json.append("          \"From\": [\"").append(escapeJson(m.getFrom())).append("\"]\n");
                json.append("        },\n");
                json.append("        \"Body\": \"").append(escapeJson(m.getBodyText())).append("\"\n");
                json.append("      },\n");
                json.append("      \"Created\": \"").append(m.getTimestamp().toString()).append("\"\n");
                json.append("    }");
            }

            json.append("\n  ]\n}");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());
        }

        private String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    private static class ApiDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            messages.clear();
            sendResponse(exchange, 200, "application/json", "{\"status\":\"ok\"}");
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
