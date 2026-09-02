package com.workhive.module.task.repository;

import com.workhive.module.task.entity.Task;
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
public interface TaskRepository extends JpaRepository<Task, UUID> {
    Page<Task> findByTenantId(UUID tenantId, Pageable pageable);
    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);
    Page<Task> findByTenantIdAndProjectId(UUID tenantId, UUID projectId, Pageable pageable);
    Page<Task> findByTenantIdAndAssigneeId(UUID tenantId, UUID assigneeId, Pageable pageable);
    List<Task> findByTenantIdAndProjectIdAndStatus(UUID tenantId, UUID projectId, String status);
    List<Task> findByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantIdAndProjectId(UUID tenantId, UUID projectId);
    long countByTenantIdAndProjectIdAndStatus(UUID tenantId, UUID projectId, String status);
    long countByTenantId(UUID tenantId);
    long countByTenantIdAndStatus(UUID tenantId, String status);
    long countByTenantIdAndAssigneeIdAndStatus(UUID tenantId, UUID assigneeId, String status);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Task> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);

    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE t.tenantId = :tenantId AND t.projectId = :projectId GROUP BY t.status")
    List<Object[]> countByProjectGroupByStatus(@Param("tenantId") UUID tenantId, @Param("projectId") UUID projectId);

    @Query("SELECT t FROM Task t WHERE t.tenantId = :tenantId AND t.assigneeId = :userId AND t.status NOT IN ('COMPLETED','CANCELLED') ORDER BY t.dueDate ASC NULLS LAST")
    List<Task> findActiveTasksByUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
