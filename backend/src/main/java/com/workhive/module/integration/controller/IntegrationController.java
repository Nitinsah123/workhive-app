package com.workhive.module.integration.controller;

import com.workhive.module.integration.dto.IntegrationDtos.*;
import com.workhive.module.integration.entity.Integration;
import com.workhive.module.integration.entity.IntegrationMapping;
import com.workhive.module.integration.service.IntegrationService;
import com.workhive.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @GetMapping
    public ResponseEntity<List<Integration>> getIntegrations() {
        return ResponseEntity.ok(integrationService.getIntegrations());
    }

    @PostMapping("/connect")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Integration> connect(@Valid @RequestBody ConnectIntegrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.connect(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Map<String, String>> disconnect(@PathVariable UUID id) {
        integrationService.disconnect(id);
        return ResponseEntity.ok(Map.of("message", "Integration disconnected successfully"));
    }

    @PostMapping({"/{id}/sync", "/github/{id}/sync", "/jira/{id}/sync", "/gitlab/{id}/sync", "/slack/{id}/sync"})
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<IntegrationSyncResponse> syncIntegration(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.syncIntegration(id));
    }

    // ==========================================
    // JIRA ENDPOINTS
    // ==========================================
    @GetMapping("/jira/{id}/projects")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<JiraProjectDto>> getJiraProjects(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getJiraProjects(id));
    }

    @GetMapping("/jira/{id}/issues")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<JiraIssueDto>> getJiraIssues(
            @PathVariable UUID id,
            @RequestParam(required = false) String projectKey) {
        return ResponseEntity.ok(integrationService.getJiraIssues(id, projectKey));
    }

    // ==========================================
    // GITLAB ENDPOINTS
    // ==========================================
    @GetMapping("/gitlab/{id}/projects")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitLabProjectDto>> getGitLabProjects(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getGitLabProjects(id));
    }

    @GetMapping("/gitlab/{id}/projects/{projectId}/commits")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitLabCommitDto>> getGitLabCommits(
            @PathVariable UUID id,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(integrationService.getGitLabCommits(id, projectId));
    }

    @GetMapping("/gitlab/{id}/projects/{projectId}/merge-requests")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitLabMergeRequestDto>> getGitLabMergeRequests(
            @PathVariable UUID id,
            @PathVariable Long projectId) {
        return ResponseEntity.ok(integrationService.getGitLabMergeRequests(id, projectId));
    }

    // ==========================================
    // SLACK ENDPOINTS
    // ==========================================
    @GetMapping("/slack/{id}/channels")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<SlackChannelDto>> getSlackChannels(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getSlackChannels(id));
    }

    @PostMapping("/slack/{id}/test-message")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<SlackPostMessageResponse> postSlackMessage(
            @PathVariable UUID id,
            @Valid @RequestBody SlackPostMessageRequest request) {
        return ResponseEntity.ok(integrationService.postSlackMessage(id, request));
    }

    // ==========================================
    // GITHUB ENDPOINTS & WEBHOOKS
    // ==========================================
    @GetMapping("/github/oauth/url")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<GitHubOAuthUrlResponse> getGitHubOAuthUrl() {
        return ResponseEntity.ok(integrationService.getGitHubOAuthUrl());
    }

    @PostMapping("/github/oauth/exchange")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<Integration> exchangeGitHubCode(@Valid @RequestBody GitHubOAuthExchangeRequest request) {
        return ResponseEntity.ok(integrationService.exchangeGitHubCode(request));
    }

    @GetMapping("/github/{id}/repositories")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitHubRepositoryDto>> getGitHubRepositories(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getGitHubRepositories(id));
    }

    @GetMapping("/github/{id}/commits")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitHubCommitDto>> getGitHubCommits(
            @PathVariable UUID id,
            @RequestParam(required = false) String repo) {
        return ResponseEntity.ok(integrationService.getGitHubCommits(id, repo));
    }

    @GetMapping("/github/{id}/pull-requests")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitHubPullRequestDto>> getGitHubPullRequests(
            @PathVariable UUID id,
            @RequestParam(required = false) String repo) {
        return ResponseEntity.ok(integrationService.getGitHubPullRequests(id, repo));
    }

    @GetMapping("/github/{id}/issues")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<GitHubIssueDto>> getGitHubIssues(
            @PathVariable UUID id,
            @RequestParam(required = false) String repo) {
        return ResponseEntity.ok(integrationService.getGitHubIssues(id, repo));
    }

    @GetMapping("/github/{id}/overview")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<GitHubOverviewDto> getGitHubOverview(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getGitHubOverview(id));
    }

    @PostMapping("/{id}/mappings")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<IntegrationMapping> mapEntity(
            @PathVariable UUID id,
            @Valid @RequestBody MapExternalEntityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(integrationService.mapEntity(id, request));
    }

    @GetMapping("/{id}/mappings")
    public ResponseEntity<List<IntegrationMapping>> getMappings(@PathVariable UUID id) {
        return ResponseEntity.ok(integrationService.getMappings(id));
    }

    @PostMapping("/webhook/github")
    public ResponseEntity<Map<String, String>> githubWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "push") String eventType,
            @RequestHeader(value = "X-GitHub-Delivery", required = false) String deliveryId,
            @RequestBody(required = false) String rawPayload) {
        integrationService.processGitHubWebhook(signature, eventType, deliveryId, rawPayload);
        return ResponseEntity.ok(Map.of("status", "received", "message", "GitHub webhook processed successfully"));
    }
}
