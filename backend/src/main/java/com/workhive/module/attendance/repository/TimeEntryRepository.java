package com.workhive.module.attendance.repository;

import com.workhive.module.attendance.entity.TimeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    Page<TimeEntry> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId, Pageable pageable);
    List<TimeEntry> findByTenantIdAndTaskIdOrderByCreatedAtDesc(UUID tenantId, UUID taskId);
    List<TimeEntry> findByTenantIdAndProjectIdOrderByCreatedAtDesc(UUID tenantId, UUID projectId);

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TimeEntry t WHERE t.tenantId = :tenantId AND t.userId = :userId AND t.createdAt >= :since")
    long sumDurationByUserIdSince(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT COALESCE(SUM(t.durationMinutes), 0) FROM TimeEntry t WHERE t.tenantId = :tenantId AND t.taskId = :taskId")
    long sumDurationByTaskId(@Param("tenantId") UUID tenantId, @Param("taskId") UUID taskId);
}
