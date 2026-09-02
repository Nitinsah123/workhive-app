package com.workhive.module.leave.repository;

import com.workhive.module.leave.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, UUID> {
    List<LeaveType> findByTenantIdAndStatus(UUID tenantId, String status);
    List<LeaveType> findByTenantId(UUID tenantId);
    Optional<LeaveType> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndName(UUID tenantId, String name);
}
