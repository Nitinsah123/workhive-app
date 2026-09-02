package com.workhive.security;

import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.team.dto.TeamDtos.CreateTeamRequest;
import com.workhive.module.team.dto.TeamDtos.UpdateTeamRequest;
import com.workhive.module.team.entity.Team;
import com.workhive.module.team.repository.TeamRepository;
import com.workhive.module.team.service.TeamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private AuditService auditService;

    private TeamService teamService;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID adminUserId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();
    private final UUID leadId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setContext(adminUserId, tenantA, "TENANT_ADMIN");
        teamService = new TeamService(teamRepository, auditService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("1. Create Team - Successfully creates and audits active team")
    void testCreateTeam_Success() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Platform Engineering");
        req.setDescription("Core platform squad");
        req.setDepartmentId(deptId);
        req.setLeadId(leadId);

        when(teamRepository.existsByTenantIdAndName(tenantA, "Platform Engineering")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(teamId);
            return t;
        });

        Team created = teamService.createTeam(req);

        assertNotNull(created);
        assertEquals("Platform Engineering", created.getName());
        assertEquals("ACTIVE", created.getStatus());
        assertEquals(tenantA, created.getTenantId());
        assertEquals(deptId, created.getDepartmentId());
        assertEquals(leadId, created.getLeadId());

        verify(auditService).log(eq(tenantA), eq(adminUserId), eq("TEAM_CREATED"), eq("TEAM"), eq(teamId), any(), any());
    }

    @Test
    @DisplayName("2. Create Team - Duplicate name in same tenant is rejected")
    void testCreateTeam_DuplicateName_ThrowsException() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("Platform Engineering");

        when(teamRepository.existsByTenantIdAndName(tenantA, "Platform Engineering")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> teamService.createTeam(req));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("3. Edit Team - Updates existing record without creating duplicates")
    void testUpdateTeam_Success_UpdatesExisting() {
        Team existing = Team.builder()
                .id(teamId)
                .tenantId(tenantA)
                .name("Old Squad Name")
                .description("Old description")
                .status("ACTIVE")
                .build();

        when(teamRepository.findByIdAndTenantId(teamId, tenantA)).thenReturn(Optional.of(existing));
        when(teamRepository.existsByTenantIdAndName(tenantA, "New Squad Name")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateTeamRequest updateReq = new UpdateTeamRequest();
        updateReq.setName("New Squad Name");
        updateReq.setDescription("Updated deliverables");
        updateReq.setDepartmentId(deptId);
        updateReq.setLeadId(leadId);
        updateReq.setStatus("ACTIVE");

        Team updated = teamService.updateTeam(teamId, updateReq);

        assertEquals(teamId, updated.getId(), "Must preserve exact same ID (no duplicate created)");
        assertEquals("New Squad Name", updated.getName());
        assertEquals("Updated deliverables", updated.getDescription());
        assertEquals(deptId, updated.getDepartmentId());
        assertEquals(leadId, updated.getLeadId());
        assertEquals("ACTIVE", updated.getStatus());

        verify(auditService).log(eq(tenantA), eq(adminUserId), eq("TEAM_UPDATED"), eq("TEAM"), eq(teamId), any(), any());
    }

    @Test
    @DisplayName("4. Edit Team - Rejects duplicate name if already taken by another team")
    void testUpdateTeam_DuplicateName_ThrowsException() {
        Team existing = Team.builder()
                .id(teamId)
                .tenantId(tenantA)
                .name("Squad Alpha")
                .status("ACTIVE")
                .build();

        when(teamRepository.findByIdAndTenantId(teamId, tenantA)).thenReturn(Optional.of(existing));
        when(teamRepository.existsByTenantIdAndName(tenantA, "Existing Squad Beta")).thenReturn(true);

        UpdateTeamRequest updateReq = new UpdateTeamRequest();
        updateReq.setName("Existing Squad Beta");

        assertThrows(DuplicateResourceException.class, () -> teamService.updateTeam(teamId, updateReq));
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("5. Delete / Archive Team - Production-safe deactivation preserves history")
    void testDeleteTeam_DeactivatesAndPreservesHistory() {
        Team existing = Team.builder()
                .id(teamId)
                .tenantId(tenantA)
                .name("Historical Team")
                .description("Has historical tasks and projects")
                .status("ACTIVE")
                .build();

        when(teamRepository.findByIdAndTenantId(teamId, tenantA)).thenReturn(Optional.of(existing));
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        teamService.deleteTeam(teamId);

        ArgumentCaptor<Team> captor = ArgumentCaptor.forClass(Team.class);
        verify(teamRepository).save(captor.capture());
        assertEquals("INACTIVE", captor.getValue().getStatus(), "Team status must be set to INACTIVE (archived)");
        verify(teamRepository, never()).delete(any());
        verify(auditService).log(eq(tenantA), eq(adminUserId), eq("TEAM_DEACTIVATED"), eq("TEAM"), eq(teamId), any(), any());
    }

    @Test
    @DisplayName("6. Tenant Isolation - Cannot edit or access team from another tenant")
    void testTenantIsolation_CrossTenantAccessBlocked() {
        // Current context is tenantA, but target team belongs to tenantB
        when(teamRepository.findByIdAndTenantId(teamId, tenantA)).thenReturn(Optional.empty());

        UpdateTeamRequest updateReq = new UpdateTeamRequest();
        updateReq.setName("Hacked Team");

        assertThrows(ResourceNotFoundException.class, () -> teamService.getTeam(teamId));
        assertThrows(ResourceNotFoundException.class, () -> teamService.updateTeam(teamId, updateReq));
        assertThrows(ResourceNotFoundException.class, () -> teamService.deleteTeam(teamId));

        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("7. Get Active vs All Teams - Correct persistence and filtering")
    void testGetTeams_ActiveVsAll() {
        Team t1 = Team.builder().id(UUID.randomUUID()).tenantId(tenantA).name("Alpha").status("ACTIVE").build();
        Team t2 = Team.builder().id(UUID.randomUUID()).tenantId(tenantA).name("Beta").status("INACTIVE").build();

        when(teamRepository.findByTenantIdAndStatus(tenantA, "ACTIVE")).thenReturn(List.of(t1));
        when(teamRepository.findByTenantId(tenantA)).thenReturn(List.of(t1, t2));

        List<Team> active = teamService.getActiveTeams();
        assertEquals(1, active.size());
        assertEquals("Alpha", active.get(0).getName());

        List<Team> all = teamService.getAllTeams();
        assertEquals(2, all.size());
    }
}
