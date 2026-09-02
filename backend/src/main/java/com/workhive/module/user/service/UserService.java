package com.workhive.module.user.service;

import com.workhive.common.exception.BadRequestException;
import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.module.user.dto.UserDtos.*;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final com.workhive.module.department.repository.DepartmentRepository departmentRepository;
    private final com.workhive.module.team.repository.TeamRepository teamRepository;
    private final com.workhive.module.user.repository.EmailConnectionRepository emailConnectionRepository;
    private final com.workhive.module.integration.repository.IntegrationRepository integrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository,
                       com.workhive.module.department.repository.DepartmentRepository departmentRepository,
                       com.workhive.module.team.repository.TeamRepository teamRepository,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) com.workhive.module.user.repository.EmailConnectionRepository emailConnectionRepository,
                       @org.springframework.beans.factory.annotation.Autowired(required = false) com.workhive.module.integration.repository.IntegrationRepository integrationRepository,
                       PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.emailConnectionRepository = emailConnectionRepository;
        this.integrationRepository = integrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public Page<User> getUsers(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return userRepository.findByTenantId(tenantId, pageable);
    }

    public List<User> getActiveUsers() {
        UUID tenantId = TenantContext.requireTenantId();
        String role = TenantContext.getRole();
        UUID userId = TenantContext.requireUserId();

        if ("TENANT_ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role)) {
            return userRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            List<User> managed = userRepository.findByTenantIdAndManagerIdAndStatus(tenantId, userId, "ACTIVE");
            User self = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);
            List<User> result = new java.util.ArrayList<>();
            if (self != null) {
                result.add(self);
            }
            for (User u : managed) {
                if (!u.getId().equals(userId)) {
                    result.add(u);
                }
            }
            return result;
        } else {
            return userRepository.findByIdAndTenantId(userId, tenantId)
                    .map(List::of)
                    .orElseGet(List::of);
        }
    }

    public User getUser(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getCurrentUser() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Transactional
    public User createUser(CreateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID creatorId = TenantContext.requireUserId();

        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new DuplicateResourceException("User with this email already exists in this organization");
        }

        // Validate department, team, manager belong to same tenant
        if (request.getDepartmentId() != null) {
            departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this organization"));
        }
        if (request.getTeamId() != null) {
            teamRepository.findByIdAndTenantId(request.getTeamId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found in this organization"));
        }
        if (request.getManagerId() != null) {
            userRepository.findByIdAndTenantId(request.getManagerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found in this organization"));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));

        String employeeCode = generateEmployeeCode(tenantId, tenant.getCode(), request.getRole());

        User user = User.builder()
                .tenantId(tenantId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .employeeCode(employeeCode)
                .role(request.getRole() != null ? request.getRole() : "EMPLOYEE")
                .departmentId(request.getDepartmentId())
                .teamId(request.getTeamId())
                .managerId(request.getManagerId())
                .phone(request.getPhone())
                .status("ACTIVE")
                .build();

        user = userRepository.save(user);
        auditService.log(tenantId, creatorId, "USER_CREATED", "USER", user.getId(), null, null);
        return user;
    }

    @Transactional
    public User updateUser(UUID id, UpdateUserRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID operatorId = TenantContext.requireUserId();

        // Validate department, team, manager belong to same tenant
        if (request.getDepartmentId() != null) {
            departmentRepository.findByIdAndTenantId(request.getDepartmentId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found in this organization"));
        }
        if (request.getTeamId() != null) {
            teamRepository.findByIdAndTenantId(request.getTeamId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found in this organization"));
        }
        if (request.getManagerId() != null) {
            userRepository.findByIdAndTenantId(request.getManagerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Manager not found in this organization"));
        }

        User user = getUser(id);
        user.setFullName(request.getFullName().trim());
        if (request.getRole() != null) user.setRole(request.getRole());
        user.setDepartmentId(request.getDepartmentId());
        user.setTeamId(request.getTeamId());
        user.setManagerId(request.getManagerId());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setTimezone(request.getTimezone());
        if (request.getStatus() != null) user.setStatus(request.getStatus());

        user = userRepository.save(user);
        auditService.log(tenantId, operatorId, "USER_UPDATED", "USER", user.getId(), null, null);
        return user;
    }

    @Transactional
    public User updateProfile(UpdateProfileRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        User user = getUser(userId);
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setTimezone(request.getTimezone());

        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        User user = getUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        auditService.log(tenantId, userId, "PASSWORD_CHANGED", "USER", userId, null, null);
    }

    @Transactional
    public void deactivateUser(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID operatorId = TenantContext.requireUserId();

        if (operatorId.equals(id)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }

        User user = getUser(id);

        if ("TENANT_ADMIN".equalsIgnoreCase(user.getRole())) {
            long activeAdmins = userRepository.countByTenantIdAndRole(tenantId, "TENANT_ADMIN");
            if (activeAdmins <= 1) {
                throw new BadRequestException("Cannot deactivate the only active Tenant Admin in this workspace. Please assign another Admin first.");
            }
        }

        user.setStatus("INACTIVE");
        userRepository.save(user);

        // Disconnect email connections safely
        if (emailConnectionRepository != null) {
            emailConnectionRepository.findByTenantIdAndUserId(tenantId, id).ifPresent(conn -> {
                conn.setStatus("NOT_CONNECTED");
                conn.setAccessTokenEnc(null);
                conn.setRefreshTokenEnc(null);
                emailConnectionRepository.save(conn);
            });
        }

        // Disconnect user integrations safely
        if (integrationRepository != null) {
            try {
                integrationRepository.findByTenantIdAndConnectedBy(tenantId, id).forEach(integration -> {
                    integration.setStatus("DISCONNECTED");
                    integration.setAccessTokenEnc(null);
                    integration.setRefreshTokenEnc(null);
                    integrationRepository.save(integration);
                });
            } catch (Exception ignored) {}
        }

        auditService.log(tenantId, operatorId, "USER_DEACTIVATED", "USER", user.getId(), null, null);
    }

    @Transactional
    public void reactivateUser(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID operatorId = TenantContext.requireUserId();

        User user = getUser(id);
        user.setStatus("ACTIVE");
        userRepository.save(user);

        auditService.log(tenantId, operatorId, "USER_REACTIVATED", "USER", user.getId(), null, null);
    }

    @Transactional
    public void deleteUser(UUID id) {
        // Safe soft-deletion preserving historical data
        deactivateUser(id);
    }

    public String generateEmployeeCode(UUID tenantId, String companyCode, String role) {
        String rolePrefix = "EMP";
        if ("TENANT_ADMIN".equalsIgnoreCase(role)) rolePrefix = "ADM";
        else if ("MANAGER".equalsIgnoreCase(role)) rolePrefix = "MGR";

        String prefix = companyCode + "-" + rolePrefix + "-";
        long count = userRepository.countByTenantId(tenantId) + 1;
        return String.format("%s%03d", prefix, count);
    }
}
