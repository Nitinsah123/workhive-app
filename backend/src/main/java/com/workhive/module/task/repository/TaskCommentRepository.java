package com.workhive.module.task.repository;

import com.workhive.module.task.entity.TaskComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {
    Page<TaskComment> findByTaskIdAndTenantIdOrderByCreatedAtDesc(UUID taskId, UUID tenantId, Pageable pageable);
}
