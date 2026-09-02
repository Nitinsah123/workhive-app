package com.workhive.module.task.repository;

import com.workhive.module.task.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SubtaskRepository extends JpaRepository<Subtask, UUID> {
    List<Subtask> findByTaskIdAndTenantIdOrderBySortOrder(UUID taskId, UUID tenantId);
    long countByTaskIdAndTenantId(UUID taskId, UUID tenantId);
    long countByTaskIdAndTenantIdAndCompletedTrue(UUID taskId, UUID tenantId);
}
