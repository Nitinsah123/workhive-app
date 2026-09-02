package com.workhive.module.user.service;

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
import com.workhive.security.JwtTokenProvider;
import com.workhive.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InvitationService {

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             TenantRepository tenantRepository,
                             DepartmentRepository departmentRepository,
                             TeamRepository teamRepository,
                             UserService userService,
                             EmailService emailService,
                             PasswordEncoder passwordEncoder,
                             JwtTokenProvider jwtTokenProvider,
                             AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.userService = userService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
    }

    public Page<Invitation> getInvitations(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return invitationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional
    public InviteUserResponse inviteUser(InviteUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID invitedBy = TenantContext.requireUserId();

        String email = request.getEmail().toLowerCase().trim();

        // 1. Check if already a member
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new DuplicateResourceException("User with email '" + email + "' is already a member of this organization");
        }

        // 2. Check duplicate pending invitation
        if (invitationRepository.existsByTenantIdAndEmailAndStatus(tenantId, email, "PENDING")) {
            throw new DuplicateResourceException("A pending invitation already exists for email '" + email + "'");
        }

        // 3. Validate department, team, manager belong strictly to this tenant
        String deptName = null;
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this organization"));
            deptName = dept.getName();
        }

        String teamName = null;
        if (request.getTeamId() != null) {
            Team team = teamRepository.findByIdAndTenantId(request.getTeamId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found in this organization"));
            teamName = team.getName();
        }

        if (request.getManagerId() != null) {
            userRepository.findByIdAndTenantId(request.getManagerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found in this organization"));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // 4. Generate unique secure token
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);

        // 5. Create and save Invitation DB record
        Invitation invitation = Invitation.builder()
                .tenantId(tenantId)
                .email(email)
                .name(request.getName() != null ? request.getName().trim() : null)
                .role(request.getRole() != null ? request.getRole() : "EMPLOYEE")
                .departmentId(request.getDepartmentId())
                .teamId(request.getTeamId())
                .managerId(request.getManagerId())
                .token(token)
                .invitedBy(invitedBy)
                .expiresAt(expiresAt)
                .status("PENDING")
                .emailStatus("EMAIL_PENDING")
                .build();

        String adminEmail = null;
        String adminName = null;
        if (invitedBy != null) {
            User admin = userRepository.findById(invitedBy).orElse(null);
            if (admin != null) {
                adminEmail = admin.getEmail();
                adminName = admin.getFullName();
            }
        }

        // Fallback to active TENANT_ADMIN of this tenant if adminEmail is not resolved
        if (adminEmail == null || adminEmail.isBlank()) {
            List<User> admins = userRepository.findByTenantIdAndRole(tenantId, "TENANT_ADMIN");
            if (!admins.isEmpty()) {
                adminEmail = admins.get(0).getEmail();
                adminName = admins.get(0).getFullName();
                if (invitedBy == null) {
                    invitedBy = admins.get(0).getId();
                }
            }
        }

        // 6. Dispatch email to exact entered recipient
        String emailStatus = emailService.sendInvitationEmail(
                tenantId,
                tenant.getName(),
                email,
                invitation.getName(),
                invitation.getRole(),
                deptName,
                teamName,
                token,
                expiresAt,
                invitation.getId(),
                adminEmail,
                adminName
        );

        invitation.setEmailStatus(emailStatus);
        invitation.setSentAt(Instant.now());
        invitation = invitationRepository.save(invitation);

        String inviteUrl = emailService.getFrontendUrl().replaceAll("/+$", "") + "/accept-invitation?token=" + token;

        InviteUserResponse response = new InviteUserResponse();
        response.setId(invitation.getId());
        response.setEmail(email);
        response.setName(invitation.getName());
        response.setRole(invitation.getRole());
        response.setToken(token);
        response.setInviteUrl(inviteUrl);
        response.setEmailStatus(emailStatus);
        response.setDepartmentId(invitation.getDepartmentId());
        response.setTeamId(invitation.getTeamId());
        response.setManagerId(invitation.getManagerId());

        if ("EMAIL_SENT".equals(emailStatus)) {
            response.setMessage("Invitation created and email automatically dispatched to " + email);
        } else {
            response.setMessage("Invitation created. Email delivery was recorded locally; use the invitation link as backup.");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public InvitationDetailsResponse getInvitationDetails(String token) {
        InvitationDetailsResponse response = new InvitationDetailsResponse();
        if (token == null || token.isBlank()) {
            response.setValid(false);
            response.setMessage("Invalid or missing invitation token");
            return response;
        }

        Invitation invitation = invitationRepository.findByToken(token).orElse(null);
        if (invitation == null) {
            response.setValid(false);
            response.setMessage("Invitation not found or invalid token");
            return response;
        }

        response.setId(invitation.getId());
        response.setEmail(invitation.getEmail());
        response.setName(invitation.getName());
        response.setRole(invitation.getRole());
        response.setStatus(invitation.getStatus());

        if ("ACCEPTED".equals(invitation.getStatus())) {
            response.setValid(false);
            response.setMessage("This invitation has already been accepted and activated");
            return response;
        }

        if ("REVOKED".equals(invitation.getStatus())) {
            response.setValid(false);
            response.setMessage("This invitation has been revoked by an organization administrator");
            return response;
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            response.setValid(false);
            response.setMessage("This invitation expired on " + invitation.getExpiresAt().toString());
            return response;
        }

        Tenant tenant = tenantRepository.findById(invitation.getTenantId()).orElse(null);
        if (tenant != null) {
            response.setTenantName(tenant.getName());
            response.setTenantCode(tenant.getCode());
        }

        if (invitation.getDepartmentId() != null) {
            departmentRepository.findById(invitation.getDepartmentId()).ifPresent(d -> response.setDepartmentName(d.getName()));
        }
        if (invitation.getTeamId() != null) {
            teamRepository.findById(invitation.getTeamId()).ifPresent(t -> response.setTeamName(t.getName()));
        }
        if (invitation.getManagerId() != null) {
            userRepository.findById(invitation.getManagerId()).ifPresent(u -> response.setManagerName(u.getFullName()));
        }

        response.setValid(true);
        response.setMessage("Valid workspace invitation for " + (tenant != null ? tenant.getName() : "WorkHive"));
        return response;
    }

    @Transactional
    public AuthResponse acceptInvitation(AcceptInvitationRequest request) {
        Invitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BadRequestException("Invitation has already been used or revoked");
        }

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus("EXPIRED");
            invitationRepository.save(invitation);
            throw new BadRequestException("Invitation has expired");
        }

        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        // Check if user already exists
        if (userRepository.existsByEmailAndTenantId(invitation.getEmail(), tenant.getId())) {
            throw new DuplicateResourceException("Account with this email already exists in this workspace");
        }

        // Generate employee code
        String employeeCode = userService.generateEmployeeCode(tenant.getId(), tenant.getCode(), invitation.getRole());

        // Use name from request or fallback to invitation name
        String fullName = request.getFullName().trim();
        if (fullName.isBlank() && invitation.getName() != null) {
            fullName = invitation.getName();
        }

        // Create new user with preserved department, team, manager
        User user = User.builder()
                .tenantId(tenant.getId())
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(fullName)
                .employeeCode(employeeCode)
                .role(invitation.getRole())
                .departmentId(invitation.getDepartmentId())
                .teamId(invitation.getTeamId())
                .managerId(invitation.getManagerId())
                .phone(request.getPhone())
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);

        // Mark invitation accepted
        invitation.setStatus("ACCEPTED");
        invitationRepository.save(invitation);

        auditService.log(tenant.getId(), user.getId(), "INVITATION_ACCEPTED", "USER", user.getId(), null, null);

        // Generate auth tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), tenant.getId(), user.getEmail(), user.getRole(), user.getFullName());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId(), tenant.getId());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo();
        userInfo.setId(user.getId().toString());
        userInfo.setEmail(user.getEmail());
        userInfo.setFullName(user.getFullName());
        userInfo.setRole(user.getRole());
        userInfo.setEmployeeCode(user.getEmployeeCode());
        userInfo.setDepartmentId(user.getDepartmentId() != null ? user.getDepartmentId().toString() : null);
        userInfo.setTeamId(user.getTeamId() != null ? user.getTeamId().toString() : null);
        userInfo.setManagerId(user.getManagerId() != null ? user.getManagerId().toString() : null);
        response.setUser(userInfo);

        AuthResponse.TenantInfo tenantInfo = new AuthResponse.TenantInfo();
        tenantInfo.setId(tenant.getId().toString());
        tenantInfo.setName(tenant.getName());
        tenantInfo.setCode(tenant.getCode());
        tenantInfo.setLogoUrl(tenant.getLogoUrl());
        tenantInfo.setTimezone(tenant.getTimezone());
        response.setTenant(tenantInfo);

        return response;
    }

    @Transactional
    public InviteUserResponse retryInvitationEmail(UUID invitationId) {
        UUID tenantId = TenantContext.requireTenantId();
        Invitation invitation = invitationRepository.findByIdAndTenantId(invitationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new BadRequestException("Can only resend emails for pending invitations");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        String deptName = invitation.getDepartmentId() != null
                ? departmentRepository.findById(invitation.getDepartmentId()).map(Department::getName).orElse(null)
                : null;
        String teamName = invitation.getTeamId() != null
                ? teamRepository.findById(invitation.getTeamId()).map(Team::getName).orElse(null)
                : null;

        UUID currentUserId = TenantContext.getUserId();
        String adminEmail = null;
        String adminName = null;
        if (currentUserId != null) {
            User admin = userRepository.findById(currentUserId).orElse(null);
            if (admin != null) {
                adminEmail = admin.getEmail();
                adminName = admin.getFullName();
            }
        }

        String emailStatus = emailService.sendInvitationEmail(
                tenantId,
                tenant.getName(),
                invitation.getEmail(),
                invitation.getName(),
                invitation.getRole(),
                deptName,
                teamName,
                invitation.getToken(),
                invitation.getExpiresAt(),
                invitation.getId(),
                adminEmail,
                adminName
        );

        invitation.setEmailStatus(emailStatus);
        invitation.setSentAt(Instant.now());
        invitationRepository.save(invitation);

        String inviteUrl = emailService.getFrontendUrl().replaceAll("/+$", "") + "/accept-invitation?token=" + invitation.getToken();

        InviteUserResponse response = new InviteUserResponse();
        response.setId(invitation.getId());
        response.setEmail(invitation.getEmail());
        response.setName(invitation.getName());
        response.setRole(invitation.getRole());
        response.setToken(invitation.getToken());
        response.setInviteUrl(inviteUrl);
        response.setEmailStatus(emailStatus);
        response.setMessage("Email re-sent to " + invitation.getEmail());
        return response;
    }

    @Transactional
    public void revokeInvitation(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Invitation invitation = invitationRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", id));
        invitation.setStatus("REVOKED");
        invitationRepository.save(invitation);
        auditService.log(tenantId, userId, "INVITATION_REVOKED", "INVITATION", invitation.getId(), null, null);
    }
}
