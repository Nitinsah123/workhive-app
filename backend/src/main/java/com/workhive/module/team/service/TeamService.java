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

    public TeamService(TeamRepository teamRepository, AuditService auditService) {
        this.teamRepository = teamRepository;
        this.auditService = auditService;
    }

    public Page<Team> getTeams(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantId(tenantId, pageable);
    }

    public List<Team> getAllTeams() {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantId(tenantId);
    }

    public List<Team> getActiveTeams() {
        UUID tenantId = TenantContext.requireTenantId();
        return teamRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
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
    public void deleteTeam(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Team team = getTeam(id);
        team.setStatus("INACTIVE");
        teamRepository.save(team);
        auditService.log(tenantId, userId, "TEAM_DEACTIVATED", "TEAM", team.getId(), null, null);
    }
}
