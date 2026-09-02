package com.workhive.module.project.repository;

import com.workhive.module.project.entity.Project;
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
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Page<Project> findByTenantId(UUID tenantId, Pageable pageable);
    Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Project> findByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Project> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND p.status IN :statuses")
    Page<Project> findByTenantIdAndStatusIn(@Param("tenantId") UUID tenantId,
                                             @Param("statuses") List<String> statuses, Pageable pageable);
}
