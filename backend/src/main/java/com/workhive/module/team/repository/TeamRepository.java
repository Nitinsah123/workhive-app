package com.workhive.module.team.repository;

import com.workhive.module.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {
    Page<Team> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Team> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
    Page<Team> findByTenantIdAndStatusNot(UUID tenantId, String status, Pageable pageable);
    List<Team> findByTenantId(UUID tenantId);
    List<Team> findByTenantIdAndStatus(UUID tenantId, String status);
    List<Team> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);
    Optional<Team> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    long countByTenantId(UUID tenantId);

    @Query("SELECT t FROM Team t WHERE t.tenantId = :tenantId AND (t.status IS NULL OR t.status <> 'ARCHIVED') " +
           "AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Team> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);
}
