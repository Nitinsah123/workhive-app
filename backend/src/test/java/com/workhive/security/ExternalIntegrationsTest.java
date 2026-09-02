package com.workhive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhive.common.exception.BadRequestException;
import com.workhive.common.util.CryptoUtils;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.integration.dto.IntegrationDtos.*;
import com.workhive.module.integration.entity.Integration;
import com.workhive.module.integration.repository.IntegrationMappingRepository;
import com.workhive.module.integration.repository.IntegrationRepository;
import com.workhive.module.integration.service.IntegrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ExternalIntegrationsTest {

    @Mock private IntegrationRepository integrationRepository;
    @Mock private IntegrationMappingRepository integrationMappingRepository;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;

    private IntegrationService integrationService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(
                integrationRepository,
                integrationMappingRepository,
                workActivityService,
                auditService,
                new ObjectMapper()
        );
        TenantContext.setContext(USER_ID, TENANT_ID, "TENANT_ADMIN");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // =========================================================================
    // JIRA TESTS
    // =========================================================================

    @Test
    @DisplayName("Jira: Connect with valid development token succeeds and masks access token in response")
    void testConnectJira_Success() {
        when(integrationRepository.findByTenantIdAndProvider(TENANT_ID, "JIRA")).thenReturn(Optional.empty());
        when(integrationRepository.save(any(Integration.class))).thenAnswer(invocation -> {
            Integration saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ConnectIntegrationRequest request = ConnectIntegrationRequest.builder()
                .provider("JIRA")
                .instanceUrl("https://acme-corp.atlassian.net")
                .externalUsername("lead@acme.com")
                .accessToken("jira_dev_token_12345")
                .build();

        Integration result = integrationService.connect(request);

        assertNotNull(result);
        assertEquals("JIRA", result.getProvider());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("********", result.getAccessTokenEnc());
        assertEquals("lead@acme.com", result.getExternalUsername());
        assertTrue(result.getExternalUserId().contains("https://acme-corp.atlassian.net"));

        verify(workActivityService).recordActivity(eq(TENANT_ID), eq(USER_ID), isNull(), isNull(),
                eq("JIRA"), eq("INTEGRATION_CONNECTED"), anyString(), anyString(), isNull(), isNull());
    }

    @Test
    @DisplayName("Jira: Connect fails if instance URL or account email is missing")
    void testConnectJira_MissingFields_ThrowsBadRequest() {
        ConnectIntegrationRequest request = ConnectIntegrationRequest.builder()
                .provider("JIRA")
                .instanceUrl("")
                .externalUsername("lead@acme.com")
                .accessToken("jira_dev_token_12345")
                .build();

        assertThrows(BadRequestException.class, () -> integrationService.connect(request));
    }

    @Test
    @DisplayName("Jira: Fetch projects and issues returns structured DTOs")
    void testGetJiraProjectsAndIssues() {
        UUID integrationId = UUID.randomUUID();
        Integration integration = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_ID)
                .provider("JIRA")
                .status("CONNECTED")
                .externalUsername("lead@acme.com")
                .externalUserId("https://acme.atlassian.net")
                .accessTokenEnc(CryptoUtils.encrypt("jira_dev_token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_ID)).thenReturn(Optional.of(integration));

        List<JiraProjectDto> projects = integrationService.getJiraProjects(integrationId);
        assertNotNull(projects);
        assertFalse(projects.isEmpty());
        assertTrue(projects.stream().anyMatch(p -> "WH".equals(p.getKey())));

        List<JiraIssueDto> issues = integrationService.getJiraIssues(integrationId, "WH");
        assertNotNull(issues);
        assertFalse(issues.isEmpty());
        assertTrue(issues.stream().anyMatch(i -> "WH-101".equals(i.getKey())));
    }

    // =========================================================================
    // GITLAB TESTS
    // =========================================================================

    @Test
    @DisplayName("GitLab: Connect with Personal Access Token succeeds and stores encrypted credentials")
    void testConnectGitLab_Success() {
        when(integrationRepository.findByTenantIdAndProvider(TENANT_ID, "GITLAB")).thenReturn(Optional.empty());
        when(integrationRepository.save(any(Integration.class))).thenAnswer(invocation -> {
            Integration saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ConnectIntegrationRequest request = ConnectIntegrationRequest.builder()
                .provider("GITLAB")
                .instanceUrl("https://gitlab.com")
                .externalUsername("gitlab-dev")
                .accessToken("glpat_dev_secret_token_12345")
                .build();

        Integration result = integrationService.connect(request);

        assertNotNull(result);
        assertEquals("GITLAB", result.getProvider());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("********", result.getAccessTokenEnc());
        assertEquals("https://gitlab.com", result.getExternalUserId());
    }

    @Test
    @DisplayName("GitLab: Fetch repositories, commits, and merge requests returns populated lists")
    void testGetGitLabRepositoriesCommitsAndMRs() {
        UUID integrationId = UUID.randomUUID();
        Integration integration = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_ID)
                .provider("GITLAB")
                .status("CONNECTED")
                .externalUsername("devops-lead")
                .externalUserId("https://gitlab.com")
                .accessTokenEnc(CryptoUtils.encrypt("glpat_dev_token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_ID)).thenReturn(Optional.of(integration));

        List<GitLabProjectDto> projects = integrationService.getGitLabProjects(integrationId);
        assertNotNull(projects);
        assertFalse(projects.isEmpty());
        assertTrue(projects.stream().anyMatch(p -> p.getName().contains("infrastructure")));

        List<GitLabCommitDto> commits = integrationService.getGitLabCommits(integrationId, 3001L);
        assertNotNull(commits);
        assertFalse(commits.isEmpty());

        List<GitLabMergeRequestDto> mrs = integrationService.getGitLabMergeRequests(integrationId, 3001L);
        assertNotNull(mrs);
        assertFalse(mrs.isEmpty());
    }

    // =========================================================================
    // SLACK TESTS
    // =========================================================================

    @Test
    @DisplayName("Slack: Connect with bot token sets CONNECTED status and masks secrets")
    void testConnectSlack_Success() {
        when(integrationRepository.findByTenantIdAndProvider(TENANT_ID, "SLACK")).thenReturn(Optional.empty());
        when(integrationRepository.save(any(Integration.class))).thenAnswer(invocation -> {
            Integration saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ConnectIntegrationRequest request = ConnectIntegrationRequest.builder()
                .provider("SLACK")
                .externalUsername("Acme Engineering")
                .accessToken("xoxb-dev_mock_token_12345")
                .build();

        Integration result = integrationService.connect(request);

        assertNotNull(result);
        assertEquals("SLACK", result.getProvider());
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("********", result.getAccessTokenEnc());
    }

    @Test
    @DisplayName("Slack: Fetch channels and dispatch test message")
    void testGetSlackChannelsAndPostMessage() {
        UUID integrationId = UUID.randomUUID();
        Integration integration = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_ID)
                .provider("SLACK")
                .status("CONNECTED")
                .externalUsername("Acme Engineering")
                .accessTokenEnc(CryptoUtils.encrypt("xoxb-dev_token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_ID)).thenReturn(Optional.of(integration));

        List<SlackChannelDto> channels = integrationService.getSlackChannels(integrationId);
        assertNotNull(channels);
        assertFalse(channels.isEmpty());
        assertTrue(channels.stream().anyMatch(c -> "general".equals(c.getName())));

        SlackPostMessageRequest msgReq = SlackPostMessageRequest.builder()
                .channel("general")
                .text("Task approved in Action Center")
                .build();

        SlackPostMessageResponse resp = integrationService.postSlackMessage(integrationId, msgReq);
        assertNotNull(resp);
        assertTrue(resp.isOk());
        assertEquals("general", resp.getChannel());

        verify(workActivityService).recordActivity(eq(TENANT_ID), eq(USER_ID), isNull(), isNull(),
                eq("SLACK"), eq("NOTIFICATION_SENT"), anyString(), anyString(), isNull(), isNull());
    }

    // =========================================================================
    // MULTI-PROVIDER SYNC & DISCONNECT TESTS
    // =========================================================================

    @Test
    @DisplayName("Sync: Synchronizing connected Jira integration updates lastSyncAt and records work activities")
    void testSyncJiraIntegration() {
        UUID integrationId = UUID.randomUUID();
        Integration integration = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_ID)
                .provider("JIRA")
                .status("CONNECTED")
                .externalUsername("admin@acme.com")
                .externalUserId("https://acme.atlassian.net")
                .accessTokenEnc(CryptoUtils.encrypt("jira_dev_token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_ID)).thenReturn(Optional.of(integration));
        when(integrationRepository.save(any(Integration.class))).thenAnswer(i -> i.getArgument(0));

        IntegrationSyncResponse syncResp = integrationService.syncIntegration(integrationId);

        assertNotNull(syncResp);
        assertEquals("SYNCED", syncResp.getStatus());
        assertEquals("JIRA", syncResp.getProvider());
        assertTrue(syncResp.getItemsSynced() > 0);

        verify(workActivityService, atLeastOnce()).recordActivity(eq(TENANT_ID), eq(USER_ID), any(), any(),
                eq("JIRA"), eq("JIRA_ISSUE_SYNC"), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("Disconnect: Disconnecting integration invalidates token and sets status to DISCONNECTED")
    void testDisconnectIntegration() {
        UUID integrationId = UUID.randomUUID();
        Integration integration = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_ID)
                .provider("GITLAB")
                .status("CONNECTED")
                .accessTokenEnc(CryptoUtils.encrypt("secret_token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_ID)).thenReturn(Optional.of(integration));

        integrationService.disconnect(integrationId);

        assertEquals("DISCONNECTED", integration.getStatus());
        assertNull(integration.getAccessTokenEnc());
        verify(integrationRepository).save(integration);
    }
}
