package com.workhive.module.integration.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_mappings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IntegrationMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "integration_id", nullable = false)
    private UUID integrationId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_name")
    private String externalName;

    @Column(name = "external_type", nullable = false, length = 50)
    private String externalType; // REPOSITORY, PROJECT, ISSUE

    @Column(name = "workhive_entity_type", length = 50)
    private String workhiveEntityType; // PROJECT, TASK

    @Column(name = "workhive_entity_id")
    private UUID workhiveEntityId;

    @Column(name = "sync_enabled", nullable = false)
    @Builder.Default
    private Boolean syncEnabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
