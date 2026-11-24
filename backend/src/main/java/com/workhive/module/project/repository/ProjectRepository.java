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

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Page<Project> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.id = :id AND p.tenantId = :tenantId AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    Optional<Project> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND p.status = :status AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    List<Project> findByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") String status);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenantId = :tenantId AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.tenantId = :tenantId AND p.status = :status AND (p.isDeleted = false OR p.isDeleted IS NULL)")
    long countByTenantIdAndStatus(@Param("tenantId") UUID tenantId, @Param("status") String status);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND (p.isDeleted = false OR p.isDeleted IS NULL) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Project> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);

    @Query("SELECT p FROM Project p WHERE p.tenantId = :tenantId AND (p.isDeleted = false OR p.isDeleted IS NULL) AND p.status IN :statuses")
    Page<Project> findByTenantIdAndStatusIn(@Param("tenantId") UUID tenantId,
                                            @Param("statuses") List<String> statuses, Pageable pageable);
}
