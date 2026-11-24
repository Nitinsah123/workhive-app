package com.workhive.module.activity.repository;

import com.workhive.module.activity.entity.EmployeeActivityEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeActivityEventRepository extends JpaRepository<EmployeeActivityEvent, UUID> {

    List<EmployeeActivityEvent> findByTenantIdAndUserIdOrderByCreatedAtDesc(UUID tenantId, UUID userId, Pageable pageable);

    List<EmployeeActivityEvent> findByTenantIdAndDepartmentIdOrderByCreatedAtDesc(UUID tenantId, UUID departmentId, Pageable pageable);

    List<EmployeeActivityEvent> findByTenantIdAndTeamIdOrderByCreatedAtDesc(UUID tenantId, UUID teamId, Pageable pageable);

    long countByTenantIdAndUserIdAndEventTypeAndCreatedAtAfter(UUID tenantId, UUID userId, String eventType, Instant after);

    List<EmployeeActivityEvent> findByTenantIdAndUserIdAndCreatedAtAfterOrderByCreatedAtAsc(UUID tenantId, UUID userId, Instant after);

    List<EmployeeActivityEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
