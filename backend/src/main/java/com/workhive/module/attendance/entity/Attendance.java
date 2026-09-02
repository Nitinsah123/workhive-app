package com.workhive.module.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "attendance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attendance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private LocalDate date;
    @Column(name = "check_in") private Instant checkIn;
    @Column(name = "check_out") private Instant checkOut;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    private String timezone;
    @Column(nullable = false) @Builder.Default private String status = "CHECKED_IN";
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) @Builder.Default private Instant createdAt = Instant.now();
}
