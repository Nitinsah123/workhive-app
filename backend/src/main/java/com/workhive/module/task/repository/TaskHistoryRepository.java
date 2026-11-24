package com.workhive.module.task.repository;

import com.workhive.module.task.entity.TaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskHistoryRepository extends JpaRepository<TaskHistory, UUID> {
    List<TaskHistory> findByTaskIdAndTenantIdOrderByCreatedAtDesc(UUID taskId, UUID tenantId);

    @Query("SELECT COUNT(th) FROM TaskHistory th WHERE th.tenantId = :tenantId AND th.taskId = :taskId AND " +
           "(th.field = 'status' OR th.field = 'review') AND (UPPER(th.newValue) LIKE '%REVIEW%' OR UPPER(th.oldValue) LIKE '%REVIEW%')")
    long countReviewTransitionsByTaskId(@Param("tenantId") UUID tenantId, @Param("taskId") UUID taskId);

    @Query("SELECT th.taskId, COUNT(th) FROM TaskHistory th WHERE th.tenantId = :tenantId AND th.taskId IN :taskIds AND " +
           "(th.field = 'status' OR th.field = 'review') AND (UPPER(th.newValue) LIKE '%REVIEW%' OR UPPER(th.oldValue) LIKE '%REVIEW%') GROUP BY th.taskId")
    List<Object[]> countReviewTransitionsByTaskIds(@Param("tenantId") UUID tenantId, @Param("taskIds") Collection<UUID> taskIds);
}

