package com.workhive.module.notification.service;

import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.notification.entity.Notification;
import com.workhive.module.notification.repository.NotificationRepository;
import com.workhive.module.user.entity.User;
import com.workhive.module.user.repository.UserRepository;
import com.workhive.security.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification createNotification(UUID tenantId, UUID recipientId, String type,
                                           String title, String message, String entityType,
                                           UUID entityId, String actionUrl) {
        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .recipientId(recipientId)
                .type(type)
                .title(title)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .actionUrl(actionUrl)
                .read(false)
                .build();

        notification = notificationRepository.save(notification);

        // Real-time broadcast to user-specific channel
        try {
            messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".user." + recipientId, notification);
        } catch (Exception e) {
            log.warn("Failed to broadcast real-time notification: {}", e.getMessage());
        }

        return notification;
    }

    @Transactional
    public void notifyAdmins(UUID tenantId, String type, String title, String message,
                             String entityType, UUID entityId, String actionUrl) {
        List<User> admins = userRepository.findByTenantIdAndRole(tenantId, "TENANT_ADMIN");
        for (User admin : admins) {
            createNotification(tenantId, admin.getId(), type, title, message, entityType, entityId, actionUrl);
        }
        // Also broadcast to general admin Action Center topic
        try {
            messagingTemplate.convertAndSend("/topic/tenant." + tenantId + ".action-center",
                    java.util.Map.of("type", type, "title", title, "entityType", entityType, "entityId", entityId));
        } catch (Exception e) {
            log.warn("Failed to broadcast action-center update: {}", e.getMessage());
        }
    }

    @Transactional
    public void notifyProjectMembers(UUID tenantId, List<UUID> userIds, String type,
                                     String title, String message, String entityType,
                                     UUID entityId, String actionUrl) {
        for (UUID userId : userIds) {
            createNotification(tenantId, userId, type, title, message, entityType, entityId, actionUrl);
        }
    }

    public Page<Notification> getMyNotifications(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return notificationRepository.findByTenantIdAndRecipientIdOrderByCreatedAtDesc(tenantId, userId, pageable);
    }

    public long getUnreadCount() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        return notificationRepository.countUnreadByRecipient(tenantId, userId);
    }

    @Transactional
    public void markAsRead(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        Notification notif = notificationRepository.findByIdAndTenantIdAndRecipientId(id, tenantId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notif.setRead(true);
        notificationRepository.save(notif);
    }

    @Transactional
    public void markAllAsRead() {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();
        notificationRepository.markAllAsRead(tenantId, userId);
    }
}
