package com.workhive.module.department.repository;

import com.workhive.module.department.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Page<Department> findByTenantId(UUID tenantId, Pageable pageable);
    Page<Department> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
    Page<Department> findByTenantIdAndStatusNot(UUID tenantId, String status, Pageable pageable);
    List<Department> findByTenantIdAndStatus(UUID tenantId, String status);
    Optional<Department> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    long countByTenantId(UUID tenantId);
}
