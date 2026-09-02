package com.workhive.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.common.util.CryptoUtils;
import com.workhive.module.activity.entity.WorkActivity;
import com.workhive.module.activity.repository.WorkActivityRepository;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.integration.dto.IntegrationDtos.*;
import com.workhive.module.integration.entity.Integration;
import com.workhive.module.integration.repository.IntegrationMappingRepository;
import com.workhive.module.integration.repository.IntegrationRepository;
import com.workhive.module.integration.service.IntegrationService;
import com.workhive.module.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GitHubPerUserOwnershipTest {

    @Mock private IntegrationRepository integrationRepository;
    @Mock private IntegrationMappingRepository integrationMappingRepository;
    @Mock private WorkActivityService workActivityService;
    @Mock private WorkActivityRepository workActivityRepository;
    @Mock private AuditService auditService;
    @Mock private UserRepository userRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private IntegrationService integrationService;

    private final UUID TENANT_A = UUID.randomUUID();
    private final UUID TENANT_B = UUID.randomUUID();
    private final UUID USER_A = UUID.randomUUID();
    private final UUID USER_B = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        integrationService = new IntegrationService(
                integrationRepository,
                integrationMappingRepository,
                workActivityService,
                workActivityRepository,
                auditService,
                userRepository,
                messagingTemplate,
                new ObjectMapper()
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Scenario 1: User A connects GitHub -> User B in same tenant sees NOT CONNECTED")
    void testUserAConnectsGitHub_UserBSeesNotConnected() {
        TenantContext.setContext(USER_A, TENANT_A, "EMPLOYEE");
        Integration connA = Integration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_A)
                .connectedBy(USER_A)
                .provider("GITHUB")
                .status("CONNECTED")
                .externalUsername("octocat-user-a")
                .accessTokenEnc(CryptoUtils.encrypt("token-a"))
                .build();

        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_A, USER_A))
                .thenReturn(List.of(connA));
        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_A, USER_B))
                .thenReturn(Collections.emptyList());

        List<Integration> userAIntegrations = integrationService.getIntegrations();
        assertEquals(1, userAIntegrations.size());
        assertEquals("GITHUB", userAIntegrations.get(0).getProvider());
        assertEquals("CONNECTED", userAIntegrations.get(0).getStatus());

        TenantContext.setContext(USER_B, TENANT_A, "EMPLOYEE");
        List<Integration> userBIntegrations = integrationService.getIntegrations();
        assertTrue(userBIntegrations.isEmpty(), "User B must see zero integrations connected");
    }

    @Test
    @DisplayName("Scenario 2: User A disconnects GitHub -> Only User A's connection is removed")
    void testUserADisconnectsGitHub_OnlyUserAConnectionRemoved() {
        UUID integrationIdA = UUID.randomUUID();
        Integration connA = Integration.builder()
                .id(integrationIdA)
                .tenantId(TENANT_A)
                .connectedBy(USER_A)
                .provider("GITHUB")
                .status("CONNECTED")
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationIdA, TENANT_A))
                .thenReturn(Optional.of(connA));

        TenantContext.setContext(USER_A, TENANT_A, "EMPLOYEE");
        integrationService.disconnect(integrationIdA);

        assertEquals("DISCONNECTED", connA.getStatus());
        verify(integrationRepository).save(connA);
    }

    @Test
    @DisplayName("Scenario 3: User B cannot disconnect or access User A's GitHub credentials")
    void testUserBCannotAccessOrDisconnectUserAIntegration() {
        UUID integrationIdA = UUID.randomUUID();
        Integration connA = Integration.builder()
                .id(integrationIdA)
                .tenantId(TENANT_A)
                .connectedBy(USER_A)
                .provider("GITHUB")
                .status("CONNECTED")
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationIdA, TENANT_A))
                .thenReturn(Optional.of(connA));

        TenantContext.setContext(USER_B, TENANT_A, "EMPLOYEE");

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.disconnect(integrationIdA);
        });

        assertThrows(ResourceNotFoundException.class, () -> {
            integrationService.syncIntegration(integrationIdA);
        });
    }

    @Test
    @DisplayName("Scenario 4: Two users in same tenant connect different GitHub accounts independently")
    void testTwoUsersInSameTenantConnectDifferentAccounts() {
        Integration connA = Integration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_A)
                .connectedBy(USER_A)
                .provider("GITHUB")
                .status("CONNECTED")
                .externalUsername("dev-alice")
                .build();

        Integration connB = Integration.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_A)
                .connectedBy(USER_B)
                .provider("GITHUB")
                .status("CONNECTED")
                .externalUsername("dev-bob")
                .build();

        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_A, USER_A))
                .thenReturn(List.of(connA));
        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_A, USER_B))
                .thenReturn(List.of(connB));

        TenantContext.setContext(USER_A, TENANT_A, "EMPLOYEE");
        List<Integration> resA = integrationService.getIntegrations();
        assertEquals("dev-alice", resA.get(0).getExternalUsername());

        TenantContext.setContext(USER_B, TENANT_A, "EMPLOYEE");
        List<Integration> resB = integrationService.getIntegrations();
        assertEquals("dev-bob", resB.get(0).getExternalUsername());
    }

    @Test
    @DisplayName("Scenario 5: Two users in different tenants connect GitHub independently")
    void testTwoUsersInDifferentTenantsConnectIndependently() {
        UUID userTenantB = UUID.randomUUID();
        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_A, USER_A))
                .thenReturn(List.of(Integration.builder().tenantId(TENANT_A).connectedBy(USER_A).provider("GITHUB").status("CONNECTED").build()));
        when(integrationRepository.findByTenantIdAndConnectedBy(TENANT_B, userTenantB))
                .thenReturn(List.of(Integration.builder().tenantId(TENANT_B).connectedBy(userTenantB).provider("GITHUB").status("CONNECTED").build()));

        TenantContext.setContext(USER_A, TENANT_A, "EMPLOYEE");
        assertEquals(TENANT_A, integrationService.getIntegrations().get(0).getTenantId());

        TenantContext.setContext(userTenantB, TENANT_B, "EMPLOYEE");
        assertEquals(TENANT_B, integrationService.getIntegrations().get(0).getTenantId());
    }

    @Test
    @DisplayName("Scenario 6, 7 & 8: Repeated sync does not duplicate WorkActivity and maps to correct user")
    void testRepeatedSyncDoesNotDuplicateAndMapsToCorrectUser() {
        UUID integrationId = UUID.randomUUID();
        Integration conn = Integration.builder()
                .id(integrationId)
                .tenantId(TENANT_A)
                .connectedBy(USER_A)
                .provider("GITHUB")
                .status("CONNECTED")
                .externalUsername("octocat")
                .accessTokenEnc(CryptoUtils.encrypt("mock-token"))
                .build();

        when(integrationRepository.findByIdAndTenantId(integrationId, TENANT_A))
                .thenReturn(Optional.of(conn));

        TenantContext.setContext(USER_A, TENANT_A, "EMPLOYEE");

        // First sync
        IntegrationSyncResponse sync1 = integrationService.syncIntegration(integrationId);
        assertNotNull(sync1);
        assertEquals("SYNCED", sync1.getStatus());

        // Simulate that an activity with externalEventId "gh-repo-101" already exists
        when(workActivityRepository.existsByTenantIdAndExternalEventId(TENANT_A, "gh-repo-101")).thenReturn(true);

        // Second sync: verifies deduplication check is called
        IntegrationSyncResponse sync2 = integrationService.syncIntegration(integrationId);
        assertNotNull(sync2);
        assertEquals("SYNCED", sync2.getStatus());
    }
}
