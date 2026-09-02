package com.workhive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.integration.dto.IntegrationDtos.*;
import com.workhive.module.integration.entity.Integration;
import com.workhive.module.integration.entity.IntegrationMapping;
import com.workhive.module.integration.repository.IntegrationMappingRepository;
import com.workhive.module.integration.repository.IntegrationRepository;
import com.workhive.module.integration.service.IntegrationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class GitHubIntegrationTest {

    @Mock private IntegrationRepository integrationRepository;
    @Mock private IntegrationMappingRepository integrationMappingRepository;
    @Mock private WorkActivityService workActivityService;
    @Mock private AuditService auditService;

    private IntegrationService integrationService;

    private final UUID TENANT_ID = UUID.randomUUID();
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(integrationRepository, integrationMappingRepository,
                workActivityService, auditService, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetGitHubOAuthUrl_GeneratesValidUrlWithState() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        GitHubOAuthUrlResponse response = integrationService.getGitHubOAuthUrl();

        assertNotNull(response);
        assertNotNull(response.getAuthorizationUrl());
        assertTrue(response.getAuthorizationUrl().contains("github.com/login/oauth/authorize"));
        assertNotNull(response.getState());
    }

    @Test
    void testConnectIntegration_SavesEncryptedTokenAndLogsActivity() {
        TenantContext.setTenantId(TENANT_ID);
        TenantContext.setUserId(USER_ID);

        ConnectIntegrationRequest req = new ConnectIntegrationRequest();
        req.setProvider("GITHUB");
        req.setAccessToken("gho_test_secret_token_12345");
        req.setExternalUsername("octocat");
        req.setScopes("repo,read:user");

        when(integrationRepository.findByTenantIdAndProvider(TENANT_ID, "GITHUB")).thenReturn(Optional.empty());
        when(integrationRepository.save(any(Integration.class))).thenAnswer(i -> {
            Integration saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        Integration result = integrationService.connect(req);

        assertNotNull(result);
        assertEquals("CONNECTED", result.getStatus());
        assertEquals("octocat", result.getExternalUsername());
        assertEquals("********", result.getAccessTokenEnc()); // Masked in response

        verify(workActivityService).recordActivity(eq(TENANT_ID), eq(USER_ID), isNull(), isNull(),
                eq("GITHUB"), eq("INTEGRATION_CONNECTED"), anyString(), anyString(), isNull(), isNull());
    }

    @Test
    void testIngestGitHubWebhook_CreatesWorkActivity() {
        GitHubWebhookPayload payload = new GitHubWebhookPayload();
        payload.setRepositoryName("workhive/backend");
        payload.setSender("developer");
        payload.setCommitMessage("feat: implement multi-tenant isolation");
        payload.setCommitId("a1b2c3d4");

        integrationService.ingestGitHubActivity(TENANT_ID, USER_ID, payload);

        verify(workActivityService).recordActivity(eq(TENANT_ID), eq(USER_ID), isNull(), isNull(),
                eq("GITHUB"), eq("COMMIT_CREATED"), contains("feat: implement multi-tenant isolation"),
                contains("workhive/backend"), eq("a1b2c3d4"), isNull());
    }

    @Test
    void testMapEntity_MapsGitHubRepoToWorkHiveProject() {
        TenantContext.setTenantId(TENANT_ID);
        UUID integrationId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();

        MapExternalEntityRequest req = new MapExternalEntityRequest();
        req.setExternalId("10101");
        req.setExternalName("workhive/backend");
        req.setExternalType("REPOSITORY");
        req.setWorkhiveEntityType("PROJECT");
        req.setWorkhiveEntityId(projectId);

        when(integrationMappingRepository.save(any(IntegrationMapping.class))).thenAnswer(i -> {
            IntegrationMapping m = i.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        IntegrationMapping result = integrationService.mapEntity(integrationId, req);

        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals("REPOSITORY", result.getExternalType());
        assertEquals(projectId, result.getWorkhiveEntityId());
    }
}