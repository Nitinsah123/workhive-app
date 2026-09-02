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
    Optional<Attendance> findByTenantIdAndUserIdAndDate(UUID tenantId, UUID userId, LocalDate date);
    Page<Attendance> findByTenantIdAndUserIdOrderByDateDesc(UUID tenantId, UUID userId, Pageable pageable);
    Page<Attendance> findByTenantIdAndDateOrderByCreatedAtDesc(UUID tenantId, LocalDate date, Pageable pageable);
    List<Attendance> findByTenantIdAndUserIdAndDateBetween(UUID tenantId, UUID userId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByTenantIdAndDateBetween(UUID tenantId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.tenantId = :tenantId AND a.date = :date AND a.status = 'CHECKED_IN'")
    long countCurrentlyCheckedIn(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM Attendance a WHERE a.tenantId = :tenantId AND a.date = :date")
    long countPresentToday(@Param("tenantId") UUID tenantId, @Param("date") LocalDate date);
}
