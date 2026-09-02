package com.workhive.module.audit.service;

import com.workhive.module.audit.entity.AuditLog;
import com.workhive.module.audit.repository.AuditLogRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void log(UUID tenantId, UUID userId, String action, String entityType,
                    UUID entityId, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }

    public void logSync(UUID tenantId, UUID userId, String action, String entityType,
                        UUID entityId, String ipAddress, String userAgent) {
        AuditLog log = AuditLog.builder()
                .tenantId(tenantId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(log);
    }
}
