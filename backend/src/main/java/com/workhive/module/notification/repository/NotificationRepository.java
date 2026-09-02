package com.workhive.module.notification.repository;

import com.workhive.module.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByTenantIdAndRecipientIdOrderByCreatedAtDesc(UUID tenantId, UUID recipientId, Pageable pageable);
    Optional<Notification> findByIdAndTenantIdAndRecipientId(UUID id, UUID tenantId, UUID recipientId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.read = false")
    long countUnreadByRecipient(@Param("tenantId") UUID tenantId, @Param("recipientId") UUID recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.tenantId = :tenantId AND n.recipientId = :recipientId AND n.read = false")
    void markAllAsRead(@Param("tenantId") UUID tenantId, @Param("recipientId") UUID recipientId);
}
