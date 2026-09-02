package com.workhive.module.leave.repository;

import com.workhive.module.leave.entity.LeaveRequest;
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
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Page<LeaveRequest> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<LeaveRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, String status, Pageable pageable);
    Page<LeaveRequest> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId, Pageable pageable);
    Optional<LeaveRequest> findByIdAndTenantId(UUID id, UUID tenantId);
    List<LeaveRequest> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.tenantId = :tenantId AND l.status = 'PENDING'")
    long countPendingByTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT l FROM LeaveRequest l WHERE l.tenantId = :tenantId AND l.userId = :userId " +
           "AND l.status = 'APPROVED' AND ((l.startDate <= :end AND l.endDate >= :start))")
    List<LeaveRequest> findApprovedLeavesInDateRange(@Param("tenantId") UUID tenantId,
                                                    @Param("userId") UUID userId,
                                                    @Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);

    @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.tenantId = :tenantId AND l.userId = :userId " +
           "AND l.status IN ('PENDING', 'APPROVED') " +
           "AND ((l.startDate <= :endDate AND l.endDate >= :startDate))")
    long countOverlappingRequests(@Param("tenantId") UUID tenantId,
                                  @Param("userId") UUID userId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);
}
