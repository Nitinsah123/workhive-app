package com.workhive.module.team.service;

import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.team.dto.TeamDtos.*;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final AuditService auditService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public TeamService(TeamRepository teamRepository, AuditService auditService) {
        this.teamRepository = teamRepository;
        this.auditService = auditService;
    }

    public Page<Team> getTeams(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantIdAndStatusNot(tenantId, "ARCHIVED", pageable);
    }

    public List<Team> getAllTeams() {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantId(tenantId);
    }

    public List<Team> getActiveTeams() {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public List<Team> getArchivedTeams() {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantIdAndStatus(tenantId, "ARCHIVED");
    }

    public List<Team> getTeamsByDepartment(UUID departmentId) {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantIdAndDepartmentId(tenantId, departmentId);
    }

    public Team getTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    @Transactional
    public Team createTeam(CreateTeamRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        if (teamRepository.existsByTenantIdAndName(tenantId, request.getName().trim())) {
            throw new DuplicateResourceException("Team with this name already exists");
        }

        Team team = Team.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .departmentId(request.getDepartmentId())
                .leadId(request.getLeadId())
                .description(request.getDescription())
                .status("ACTIVE")
                .build();

        team = teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_CREATED", "TEAM", team.getId(), null, null);
        return team;
    }

    @Transactional
    public Team updateTeam(UUID id, UpdateTeamRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Team team = getTeam(id);
        String newName = request.getName().trim();
        if (!team.getName().equalsIgnoreCase(newName) && teamRepository.existsByTenantIdAndName(tenantId, newName)) {
            throw new DuplicateResourceException("Team with this name already exists");
        }

        team.setName(newName);
        team.setDepartmentId(request.getDepartmentId());
        team.setLeadId(request.getLeadId());
        team.setDescription(request.getDescription());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            team.setStatus(request.getStatus().toUpperCase().trim());
        }

        team = teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_UPDATED", "TEAM", team.getId(), null, null);
        return team;
    }

    @Transactional
    public void archiveTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if (!"TENANT_ADMIN".equalsIgnoreCase(role)) {
            throw new com.workhive.common.exception.BadRequestException("Only Tenant Admins can archive teams");
        }

        Team team = getTeam(id);
        team.setStatus("ARCHIVED");
        teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_ARCHIVED", "TEAM", team.getId(), null, null);
    }

    @Transactional
    public void unarchiveTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if (!"TENANT_ADMIN".equalsIgnoreCase(role)) {
            throw new com.workhive.common.exception.BadRequestException("Only Tenant Admins can restore teams");
        }

        Team team = getTeam(id);
        team.setStatus("ACTIVE");
        teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_RESTORED", "TEAM", team.getId(), null, null);
    }

    @Transactional
    public void permanentDeleteTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        String role = TenantContext.getRole();

        if (!"TENANT_ADMIN".equalsIgnoreCase(role)) {
            throw new com.workhive.common.exception.BadRequestException("Only Tenant Admins can permanently delete teams");
        }

        Team team = getTeam(id);

        // Safely detach foreign key references
        entityManager.createNativeQuery("UPDATE users SET team_id = NULL WHERE team_id = :tid AND tenant_id = :tenantId")
                .setParameter("tid", id).setParameter("tenantId", tenantId).executeUpdate();
        entityManager.createNativeQuery("UPDATE projects SET team_id = NULL WHERE team_id = :tid AND tenant_id = :tenantId")
                .setParameter("tid", id).setParameter("tenantId", tenantId).executeUpdate();
        entityManager.createNativeQuery("UPDATE invitations SET team_id = NULL WHERE team_id = :tid AND tenant_id = :tenantId")
                .setParameter("tid", id).setParameter("tenantId", tenantId).executeUpdate();

        teamRepository.delete(team);
        auditService.log(tenantId, userId, "TEAM_PERMANENTLY_DELETED", "TEAM", id, null, null);
    }

    @Transactional
    public void deleteTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Team team = getTeam(id);
        team.setStatus("INACTIVE");
        teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_DEACTIVATED", "TEAM", team.getId(), null, null);
    }
}
