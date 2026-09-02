package com.workhive.security;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.auth.dto.AuthDtos.*;
import com.workhive.module.auth.repository.RefreshTokenRepository;
import com.workhive.module.auth.service.AuthService;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.workhive.module.user.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuditService auditService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(tenantRepository, userRepository, refreshTokenRepository,
                invitationRepository, passwordEncoder, jwtTokenProvider, auditService);
    }

    @Test
    void testCreateWorkspace_Success() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setCompanyName("Acme Corp");
        req.setCompanyCode("ACM");
        req.setAdminFullName("Alice Admin");
        req.setAdminEmail("alice@acme.com");
        req.setAdminPassword("Password123!");

        when(tenantRepository.existsBySlug(any())).thenReturn(false);
        when(tenantRepository.existsByCode("ACM")).thenReturn(false);
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed_pwd");

        Tenant savedTenant = Tenant.builder().id(UUID.randomUUID()).name("Acme Corp").code("ACM").slug("acme-corp").build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);

        User savedUser = User.builder().id(UUID.randomUUID()).tenantId(savedTenant.getId()).email("alice@acme.com")
                .fullName("Alice Admin").role("TENANT_ADMIN").employeeCode("ACM-ADM-001").build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh_token");

        AuthResponse res = authService.createWorkspace(req);

        assertNotNull(res);
        assertEquals("access_token", res.getAccessToken());
        assertEquals("ACM-ADM-001", res.getUser().getEmployeeCode());
        assertEquals("ACM", res.getTenant().getCode());
        verify(auditService).log(eq(savedTenant.getId()), eq(savedUser.getId()), eq("WORKSPACE_CREATED"), any(), any(), any(), any());
    }

    @Test
    void testCreateWorkspace_DuplicateCode_ThrowsException() {
        CreateWorkspaceRequest req = new CreateWorkspaceRequest();
        req.setCompanyName("Acme Corp");
        req.setCompanyCode("ACM");
        req.setAdminFullName("Alice Admin");
        req.setAdminEmail("alice@acme.com");
        req.setAdminPassword("Password123!");

        when(tenantRepository.existsBySlug(any())).thenReturn(false);
        when(tenantRepository.existsByCode("ACM")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.createWorkspace(req));
    }

    @Test
    void testLogin_AutomaticTenantResolution() {
        LoginRequest req = new LoginRequest();
        req.setEmail("bob@acme.com");
        req.setPassword("Secret123!");

        UUID tenantId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).tenantId(tenantId).email("bob@acme.com")
                .passwordHash("hashed").fullName("Bob Builder").role("EMPLOYEE").employeeCode("ACM-EMP-001").status("ACTIVE").build();

        Tenant tenant = Tenant.builder().id(tenantId).name("Acme Corp").code("ACM").status("ACTIVE").build();

        when(userRepository.findByEmail("bob@acme.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123!", "hashed")).thenReturn(true);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access_token");
        when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("refresh_token");

        AuthResponse res = authService.login(req);

        assertNotNull(res);
        assertEquals("Acme Corp", res.getTenant().getName());
        assertEquals("bob@acme.com", res.getUser().getEmail());
    }
}
