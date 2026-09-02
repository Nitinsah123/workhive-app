package com.workhive.security;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.auth.dto.AuthDtos.AuthResponse;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.dto.UserDtos.*;
import com.workhive.module.user.entity.Invitation;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.InvitationRepository;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.module.user.service.EmailService;
import com.workhive.module.user.service.InvitationService;
import com.workhive.module.user.service.MailHogServer;
import com.workhive.module.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationEmailFlowTest {

    @Mock private InvitationRepository invitationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private UserService userService;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditService auditService;

    private InvitationService invitationService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setContext(adminId, tenantId, "TENANT_ADMIN");
        invitationService = new InvitationService(
                invitationRepository,
                userRepository,
                tenantRepository,
                departmentRepository,
                teamRepository,
                userService,
                emailService,
                passwordEncoder,
                jwtTokenProvider,
                auditService
        );
        MailHogServer.clearAllMessages();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MailHogServer.clearAllMessages();
    }

    @Test
    void test1_InviteEmployee1_SuccessExactRecipient() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("employee1@gmail.com");
        req.setName("Alice Engineer");
        req.setRole("EMPLOYEE");

        when(userRepository.existsByEmailAndTenantId("employee1@gmail.com", tenantId)).thenReturn(false);
        when(invitationRepository.existsByTenantIdAndEmailAndStatus(tenantId, "employee1@gmail.com", "PENDING")).thenReturn(false);

        Tenant tenant = Tenant.builder().id(tenantId).name("Apex Cloud").code("APEX").build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        when(emailService.sendInvitationEmail(eq(tenantId), eq("Apex Cloud"), eq("employee1@gmail.com"),
                eq("Alice Engineer"), eq("EMPLOYEE"), isNull(), isNull(), any(), any(), any(), any(), any()))
                .thenReturn("EMAIL_SENT");
        when(emailService.getFrontendUrl()).thenReturn("http://localhost:3000");

        InviteUserResponse res = invitationService.inviteUser(req);

        assertNotNull(res);
        assertEquals("employee1@gmail.com", res.getEmail());
        assertEquals("Alice Engineer", res.getName());
        assertEquals("EMAIL_SENT", res.getEmailStatus());
        assertTrue(res.getInviteUrl().contains("/accept-invitation?token="));

        verify(emailService).sendInvitationEmail(
                eq(tenantId),
                eq("Apex Cloud"),
                eq("employee1@gmail.com"),
                eq("Alice Engineer"),
                eq("EMPLOYEE"),
                isNull(),
                isNull(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void test2_InviteEmployee2_WithDepartmentTeamManager() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("employee2@gmail.com");
        req.setName("Bob Platform");
        req.setRole("MANAGER");
        req.setDepartmentId(deptId);
        req.setTeamId(teamId);
        req.setManagerId(managerId);

        when(userRepository.existsByEmailAndTenantId("employee2@gmail.com", tenantId)).thenReturn(false);
        when(invitationRepository.existsByTenantIdAndEmailAndStatus(tenantId, "employee2@gmail.com", "PENDING")).thenReturn(false);

        Department dept = Department.builder().id(deptId).tenantId(tenantId).name("Engineering").build();
        when(departmentRepository.findByIdAndTenantId(deptId, tenantId)).thenReturn(Optional.of(dept));

        Team team = Team.builder().id(teamId).tenantId(tenantId).name("Core Team").build();
        when(teamRepository.findByIdAndTenantId(teamId, tenantId)).thenReturn(Optional.of(team));

        User manager = User.builder().id(managerId).tenantId(tenantId).fullName("Marcus Vance").build();
        when(userRepository.findByIdAndTenantId(managerId, tenantId)).thenReturn(Optional.of(manager));

        Tenant tenant = Tenant.builder().id(tenantId).name("Apex Cloud").code("APEX").build();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> {
            Invitation inv = i.getArgument(0);
            inv.setId(UUID.randomUUID());
            return inv;
        });

        when(emailService.sendInvitationEmail(eq(tenantId), eq("Apex Cloud"), eq("employee2@gmail.com"),
                eq("Bob Platform"), eq("MANAGER"), eq("Engineering"), eq("Core Team"), any(), any(), any(), any(), any()))
                .thenReturn("EMAIL_SENT");
        when(emailService.getFrontendUrl()).thenReturn("http://localhost:3000");

        InviteUserResponse res = invitationService.inviteUser(req);

        assertNotNull(res);
        assertEquals("employee2@gmail.com", res.getEmail());
        assertEquals("Bob Platform", res.getName());
        assertEquals(deptId, res.getDepartmentId());
        assertEquals(teamId, res.getTeamId());
        assertEquals(managerId, res.getManagerId());
    }

    @Test
    void test3_InviteDuplicateActiveUser_ThrowsException() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("existing@gmail.com");

        when(userRepository.existsByEmailAndTenantId("existing@gmail.com", tenantId)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> invitationService.inviteUser(req));
    }

    @Test
    void test4_InviteDuplicatePending_ThrowsException() {
        InviteUserRequest req = new InviteUserRequest();
        req.setEmail("pending@gmail.com");

        when(userRepository.existsByEmailAndTenantId("pending@gmail.com", tenantId)).thenReturn(false);
        when(invitationRepository.existsByTenantIdAndEmailAndStatus(tenantId, "pending@gmail.com", "PENDING")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> invitationService.inviteUser(req));
    }

    @Test
    void test5_AcceptInvitation_PreservesDepartmentTeamManager() {
        String token = "valid-secret-token-12345";
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken(token);
        req.setFullName("Charlie Cloud");
        req.setPassword("Password123!");
        req.setPhone("+15551234");

        Invitation inv = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("charlie@gmail.com")
                .role("EMPLOYEE")
                .departmentId(deptId)
                .teamId(teamId)
                .managerId(managerId)
                .token(token)
                .status("PENDING")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        Tenant tenant = Tenant.builder().id(tenantId).name("Apex Cloud").code("APEX").build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(inv));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userService.generateEmployeeCode(tenantId, "APEX", "EMPLOYEE")).thenReturn("APEX-EMP-005");
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed_pwd");

        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        when(jwtTokenProvider.generateAccessToken(any(), eq(tenantId), eq("charlie@gmail.com"), eq("EMPLOYEE"), eq("Charlie Cloud")))
                .thenReturn("jwt_access_token");
        when(jwtTokenProvider.generateRefreshToken(any(), eq(tenantId))).thenReturn("jwt_refresh_token");

        AuthResponse res = invitationService.acceptInvitation(req);

        assertNotNull(res);
        assertEquals("jwt_access_token", res.getAccessToken());
        assertEquals(deptId.toString(), res.getUser().getDepartmentId());
        assertEquals(teamId.toString(), res.getUser().getTeamId());
        assertEquals(managerId.toString(), res.getUser().getManagerId());
        assertEquals("ACCEPTED", inv.getStatus());
    }

    @Test
    void test6_AcceptExpiredInvitation_ThrowsException() {
        String token = "expired-token-123";
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken(token);
        req.setFullName("David Doe");
        req.setPassword("Password123!");

        Invitation inv = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("david@gmail.com")
                .status("PENDING")
                .expiresAt(Instant.now().minus(2, ChronoUnit.DAYS)) // Expired!
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(inv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(req));
        assertEquals("EXPIRED", inv.getStatus());
    }

    @Test
    void test7_AcceptRevokedInvitation_ThrowsException() {
        String token = "revoked-token-123";
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken(token);
        req.setFullName("Eve Evans");
        req.setPassword("Password123!");

        Invitation inv = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("eve@gmail.com")
                .status("REVOKED")
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(inv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(req));
    }

    @Test
    void test8_AcceptReusedInvitation_ThrowsException() {
        String token = "reused-token-123";
        AcceptInvitationRequest req = new AcceptInvitationRequest();
        req.setToken(token);
        req.setFullName("Frank Foster");
        req.setPassword("Password123!");

        Invitation inv = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("frank@gmail.com")
                .status("ACCEPTED") // Already accepted!
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build();

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(inv));

        assertThrows(BadRequestException.class, () -> invitationService.acceptInvitation(req));
    }

    @Test
    void test9_RetryInvitationEmail_DispatchesToExactRecipient() {
        UUID invId = UUID.randomUUID();
        Invitation inv = Invitation.builder()
                .id(invId)
                .tenantId(tenantId)
                .email("retry.employee@gmail.com")
                .name("Grace Hopper")
                .role("EMPLOYEE")
                .token("retry-token-123")
                .status("PENDING")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();

        Tenant tenant = Tenant.builder().id(tenantId).name("Apex Cloud").build();

        when(invitationRepository.findByIdAndTenantId(invId, tenantId)).thenReturn(Optional.of(inv));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(emailService.sendInvitationEmail(eq(tenantId), eq("Apex Cloud"), eq("retry.employee@gmail.com"),
                eq("Grace Hopper"), eq("EMPLOYEE"), isNull(), isNull(), any(), any(), eq(invId), any(), any()))
                .thenReturn("EMAIL_SENT");
        when(emailService.getFrontendUrl()).thenReturn("http://localhost:3000");

        InviteUserResponse res = invitationService.retryInvitationEmail(invId);

        assertNotNull(res);
        assertEquals("retry.employee@gmail.com", res.getEmail());
        assertEquals("EMAIL_SENT", res.getEmailStatus());
        verify(emailService).sendInvitationEmail(
                eq(tenantId), eq("Apex Cloud"), eq("retry.employee@gmail.com"),
                eq("Grace Hopper"), eq("EMPLOYEE"), isNull(), isNull(), any(), any(), eq(invId), any(), any()
        );
    }

    @Test
    void test10_MailHogServer_LocalMessageRecording() {
        MailHogServer.MailMessage msg = MailHogServer.MailMessage.builder()
                .id(UUID.randomUUID().toString())
                .from("noreply@workhive.internal")
                .to(List.of("qa.test.recipient@gmail.com"))
                .subject("You've been invited to join Acme Corp on WorkHive")
                .bodyText("Hello QA Tester,\nClick here to join: http://localhost:3000/accept-invitation?token=test12345")
                .timestamp(Instant.now())
                .build();

        MailHogServer.recordMessage(msg);

        List<MailHogServer.MailMessage> received = MailHogServer.getReceivedMessages();
        assertEquals(1, received.size());
        assertEquals("qa.test.recipient@gmail.com", received.get(0).getTo().get(0));
        assertTrue(received.get(0).getBodyText().contains("token=test12345"));
    }
}
