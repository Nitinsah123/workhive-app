package com.workhive.module.announcement.repository;

import com.workhive.module.announcement.entity.Announcement;
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
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    Page<Announcement> findByTenantIdAndPublishedTrueOrderByPublishedAtDesc(UUID tenantId, Pageable pageable);
    Optional<Announcement> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT a FROM Announcement a WHERE a.tenantId = :tenantId AND a.published = true AND " +
           "(a.targetType = 'ORGANIZATION' OR " +
           "(a.targetType = 'DEPARTMENT' AND a.targetId = :departmentId) OR " +
           "(a.targetType = 'TEAM' AND a.targetId = :teamId)) " +
           "ORDER BY a.publishedAt DESC")
    List<Announcement> findTargetedForUser(@Param("tenantId") UUID tenantId,
                                          @Param("departmentId") UUID departmentId,
                                          @Param("teamId") UUID teamId);
}
