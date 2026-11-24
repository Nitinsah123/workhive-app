package com.workhive.module.attendance.repository;

import com.workhive.module.attendance.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    @Query("SELECT a FROM Attendance a WHERE a.tenantId = :tenantId AND a.userId = :userId AND a.date = :date ORDER BY a.createdAt DESC LIMIT 1")
    Optional<Attendance> findByTenantIdAndUserIdAndDate(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId, @Param("date") LocalDate date);

    List<Attendance> findByTenantIdAndUserIdAndDateOrderByCreatedAtAsc(UUID tenantId, UUID userId, LocalDate date);

    Optional<Attendance> findFirstByTenantIdAndUserIdAndDateAndCheckOutIsNullOrderByCreatedAtDesc(UUID tenantId, UUID userId, LocalDate date);

    Optional<Attendance> findFirstByTenantIdAndUserIdAndDateAndStatusOrderByCreatedAtDesc(UUID tenantId, UUID userId, LocalDate date, String status);

    Optional<Attendance> findFirstByTenantIdAndUserIdAndDateOrderByCreatedAtDesc(UUID tenantId, UUID userId, LocalDate date);

    Page<Attendance> findByTenantIdAndUserIdOrderByDateDesc(UUID tenantId, UUID userId, Pageable pageable);

    Page<Attendance> findByTenantIdAndDateOrderByCreatedAtDesc(UUID tenantId, LocalDate date, Pageable pageable);

    List<Attendance> findByTenantIdAndUserIdAndDateBetween(UUID tenantId, UUID userId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByTenantIdAndDateBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.tenantId = :tenantId AND a.date = :date AND a.status = 'CHECKED_IN' AND a.checkOut IS NULL")
    long countCurrentlyCheckedIn(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT a.userId FROM Attendance a WHERE a.tenantId = :tenantId AND a.date = :date AND a.status = 'CHECKED_IN' AND a.checkOut IS NULL")
    List<UUID> findActiveUserIdsToday(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM Attendance a WHERE a.tenantId = :tenantId AND a.date = :date")
    long countPresentToday(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
