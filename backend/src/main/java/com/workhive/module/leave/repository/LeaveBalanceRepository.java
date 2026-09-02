package com.workhive.module.leave.repository;

import com.workhive.module.leave.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    List<LeaveBalance> findByTenantIdAndUserIdAndYear(UUID tenantId, UUID userId, Integer year);
    Optional<LeaveBalance> findByTenantIdAndUserIdAndLeaveTypeIdAndYear(UUID tenantId, UUID userId, UUID leaveTypeId, Integer year);
    List<LeaveBalance> findByTenantIdAndYear(UUID tenantId, Integer year);
}
