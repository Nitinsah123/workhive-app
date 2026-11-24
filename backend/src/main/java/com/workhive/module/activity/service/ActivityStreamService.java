package com.workhive.module.activity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhive.module.activity.entity.EmployeeActivityEvent;
import com.workhive.module.activity.repository.EmployeeActivityEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ActivityStreamService {

    private static final Logger log = LoggerFactory.getLogger(ActivityStreamService.class);
    private static final long SSE_TIMEOUT = 10 * 60 * 1000L; // 10 minutes

    private final EmployeeActivityEventRepository employeeActivityEventRepository;
    private final ObjectMapper objectMapper;

    // TenantId -> List of active SseEmitters
    private final Map<UUID, List<SseEmitter>> tenantEmitters = new ConcurrentHashMap<>();

    public ActivityStreamService(EmployeeActivityEventRepository employeeActivityEventRepository,
                                 ObjectMapper objectMapper) {
        this.employeeActivityEventRepository = employeeActivityEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribes a client to SSE updates for their tenant.
     */
    public SseEmitter subscribe(UUID tenantId, UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        tenantEmitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(tenantId, emitter));
        emitter.onTimeout(() -> removeEmitter(tenantId, emitter));
        emitter.onError((ex) -> removeEmitter(tenantId, emitter));

        // Send initial connected payload
        try {
            Map<String, Object> init = new HashMap<>();
            init.put("type", "CONNECTED");
            init.put("tenantId", tenantId);
            init.put("userId", userId);
            init.put("timestamp", Instant.now().toString());
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(objectMapper.writeValueAsString(init)));
        } catch (IOException e) {
            removeEmitter(tenantId, emitter);
        }

        return emitter;
    }

    private void removeEmitter(UUID tenantId, SseEmitter emitter) {
        List<SseEmitter> list = tenantEmitters.get(tenantId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                tenantEmitters.remove(tenantId);
            }
        }
    }

    /**
     * Records an event in database and broadcasts to all active tenant SSE listeners.
     */
    public EmployeeActivityEvent publishActivity(UUID tenantId, UUID userId, UUID departmentId,
                                                UUID teamId, String eventType, String title,
                                                String metadataJson) {
        EmployeeActivityEvent event = EmployeeActivityEvent.builder()
                .tenantId(tenantId)
                .userId(userId)
                .departmentId(departmentId)
                .teamId(teamId)
                .eventType(eventType != null ? eventType.toUpperCase() : "HEARTBEAT")
                .title(title)
                .metadataJson(metadataJson)
                .createdAt(Instant.now())
                .build();

        EmployeeActivityEvent saved = employeeActivityEventRepository.save(event);

        broadcast(tenantId, saved);
        return saved;
    }

    public void broadcast(UUID tenantId, EmployeeActivityEvent event) {
        List<SseEmitter> emitters = tenantEmitters.get(tenantId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("activity")
                        .data(objectMapper.writeValueAsString(event)));
            } catch (Exception ex) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
        }
    }

    /**
     * Keeps SSE connections alive through reverse proxies (Cloudflare, Nginx, Render)
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeats() {
        for (Map.Entry<UUID, List<SseEmitter>> entry : tenantEmitters.entrySet()) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : entry.getValue()) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception ex) {
                    deadEmitters.add(emitter);
                }
            }
            if (!deadEmitters.isEmpty()) {
                entry.getValue().removeAll(deadEmitters);
            }
        }
    }
}
