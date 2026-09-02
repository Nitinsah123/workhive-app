package com.workhive.module.announcement.service;

import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.activity.service.WorkActivityService;
import com.workhive.module.announcement.dto.AnnouncementDtos.CreateAnnouncementRequest;
import com.workhive.module.announcement.entity.Announcement;
import com.workhive.module.announcement.repository.AnnouncementRepository;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.notification.service.NotificationService;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final WorkActivityService workActivityService;
    private final AuditService auditService;

    public AnnouncementService(AnnouncementRepository announcementRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               WorkActivityService workActivityService,
                               AuditService auditService) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.workActivityService = workActivityService;
        this.auditService = auditService;
    }

    public Page<Announcement> getAnnouncements(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return announcementRepository.findByTenantIdAndPublishedTrueOrderByPublishedAtDesc(tenantId, pageable);
    }

    public List<Announcement> getMyTargetedAnnouncements() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId).orElse(null);

        UUID deptId = user != null ? user.getDepartmentId() : null;
        UUID teamId = user != null ? user.getTeamId() : null;

        return announcementRepository.findTargetedForUser(tenantId, deptId, teamId);
    }

    @Transactional
    public Announcement createAnnouncement(CreateAnnouncementRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Announcement announcement = Announcement.builder()
                .tenantId(tenantId)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .authorId(userId)
                .targetType(request.getTargetType() != null ? request.getTargetType() : "ORGANIZATION")
                .targetId(request.getTargetId())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .published(request.getPublished() != null ? request.getPublished() : true)
                .publishedAt(Instant.now())
                .build();

        announcement = announcementRepository.save(announcement);

        // Notify target audience
        List<User> targetUsers;
        if ("DEPARTMENT".equals(announcement.getTargetType()) && announcement.getTargetId() != null) {
            targetUsers = userRepository.findByTenantIdAndDepartmentId(tenantId, announcement.getTargetId());
        } else if ("TEAM".equals(announcement.getTargetType()) && announcement.getTargetId() != null) {
            targetUsers = userRepository.findByTenantIdAndTeamId(tenantId, announcement.getTargetId());
        } else {
            targetUsers = userRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        }

        for (User u : targetUsers) {
            if (!u.getId().equals(userId)) {
                notificationService.createNotification(tenantId, u.getId(), "ANNOUNCEMENT",
                        "Announcement: " + announcement.getTitle(),
                        announcement.getContent(), "ANNOUNCEMENT", announcement.getId(), "/announcements");
            }
        }

        workActivityService.recordActivity(tenantId, userId, null, null, "WORKHIVE", "ANNOUNCEMENT_CREATED",
                "Published announcement: " + announcement.getTitle(), null, null, null);
        auditService.log(tenantId, userId, "ANNOUNCEMENT_CREATED", "ANNOUNCEMENT", announcement.getId(), null, null);

        return announcement;
    }

    public Announcement getAnnouncement(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return announcementRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
    }
}
