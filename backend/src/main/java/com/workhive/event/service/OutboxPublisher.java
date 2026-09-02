package com.workhive.event.service;

import com.workhive.event.entity.OutboxEvent;
import com.workhive.event.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           SimpMessagingTemplate messagingTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                // Broadcast on tenant-specific STOMP topic
                messagingTemplate.convertAndSend("/topic/tenant." + event.getTenantId() + ".events", event);
                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to process outbox event {}: {}", event.getId(), e.getMessage());
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus("FAILED");
                }
            }
            outboxEventRepository.save(event);
        }
    }
}
