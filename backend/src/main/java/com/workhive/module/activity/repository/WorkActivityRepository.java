package com.workhive.module.activity.repository;

import com.workhive.module.activity.entity.WorkActivity;
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
public interface WorkActivityRepository extends JpaRepository<WorkActivity, UUID> {
    Page<WorkActivity> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<WorkActivity> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId, Pageable pageable);
    Page<WorkActivity> findByTenantIdAndProjectIdOrderByCreatedAtDesc(UUID tenantId, UUID projectId, Pageable pageable);
    List<WorkActivity> findByTenantIdAndCreatedAtBetween(UUID tenantId, Instant start, Instant end);
    List<WorkActivity> findByTenantIdAndUserIdAndCreatedAtBetween(UUID tenantId, UUID userId, Instant start, Instant end);

    @Query("SELECT COUNT(w) FROM WorkActivity w WHERE w.tenantId = :tenantId AND w.userId = :userId AND w.createdAt >= :since")
    long countByUserSince(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("since") Instant since);

    @Query("SELECT w.activityType, COUNT(w) FROM WorkActivity w WHERE w.tenantId = :tenantId AND w.userId = :userId GROUP BY w.activityType")
    List<Object[]> countActivitiesByTypeForUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    boolean existsByTenantIdAndExternalEventId(UUID tenantId, String externalEventId);
    boolean existsByTenantIdAndUserIdAndExternalEventId(UUID tenantId, UUID userId, String externalEventId);
    Page<WorkActivity> findByTenantIdAndUserIdInOrderByCreatedAtDesc(UUID tenantId, java.util.Collection<UUID> userIds, Pageable pageable);
    List<WorkActivity> findByTenantIdAndUserIdInAndCreatedAtBetween(UUID tenantId, java.util.Collection<UUID> userIds, Instant start, Instant end);
}
