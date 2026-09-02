package com.workhive.module.task.repository;

import com.workhive.module.task.entity.TaskSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, UUID> {

    List<TaskSubmission> findByTaskIdAndTenantIdOrderBySubmittedAtDesc(UUID taskId, UUID tenantId);

    Optional<TaskSubmission> findFirstByTaskIdAndTenantIdAndReviewStatusOrderBySubmittedAtDesc(UUID taskId, UUID tenantId, String reviewStatus);

    Optional<TaskSubmission> findFirstByTaskIdAndTenantIdOrderBySubmittedAtDesc(UUID taskId, UUID tenantId);

    List<TaskSubmission> findByProjectIdAndTenantIdOrderBySubmittedAtDesc(UUID projectId, UUID tenantId);

    long countByTaskIdAndTenantId(UUID taskId, UUID tenantId);
}
