package com.workhive.module.team.repository;

import com.workhive.module.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    Page<Team> findByTenantId(UUID tenantId, Pageable pageable);
    List<Team> findByTenantId(UUID tenantId);
    List<Team> findByTenantIdAndStatus(UUID tenantId, String status);
    List<Team> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);
    Optional<Team> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    long countByTenantId(UUID tenantId);
}
