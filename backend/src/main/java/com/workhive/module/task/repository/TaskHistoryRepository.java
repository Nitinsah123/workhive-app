package com.workhive.module.task.repository;

import com.workhive.module.task.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
    List<TaskHistory> findByTaskIdAndTenantIdOrderByCreatedAtDesc(UUID taskId, UUID tenantId);
}
