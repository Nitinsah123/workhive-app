package com.workhive.module.department.service;

import com.workhive.common.exception.DuplicateResourceException;
import com.workhive.common.exception.ResourceNotFoundException;
import com.workhive.module.audit.service.AuditService;
import com.workhive.module.department.dto.DepartmentDtos.*;
import com.workhive.module.department.entity.Department;
import com.workhive.module.department.repository.DepartmentRepository;
import com.workhive.security.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final AuditService auditService;

    public DepartmentService(DepartmentRepository departmentRepository, AuditService auditService) {
        this.departmentRepository = departmentRepository;
        this.auditService = auditService;
    }

    public Page<Department> getDepartments(Pageable pageable) {
        UUID tenantId = TenantContext.requireTenantId();
        return departmentRepository.findByTenantId(tenantId, pageable);
    }

    public List<Department> getActiveDepartments() {
        UUID tenantId = TenantContext.requireTenantId();
        return departmentRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    public Department getDepartment(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return departmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    @Transactional
    public Department createDepartment(CreateDepartmentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        if (departmentRepository.existsByTenantIdAndName(tenantId, request.getName().trim())) {
            throw new DuplicateResourceException("Department with this name already exists");
        }

        Department department = Department.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .description(request.getDescription())
                .managerId(request.getManagerId())
                .status("ACTIVE")
                .build();

        department = departmentRepository.save(department);
        auditService.log(tenantId, userId, "DEPARTMENT_CREATED", "DEPARTMENT", department.getId(), null, null);
        return department;
    }

    @Transactional
    public Department updateDepartment(UUID id, UpdateDepartmentRequest request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Department department = getDepartment(id);
        department.setName(request.getName().trim());
        department.setDescription(request.getDescription());
        department.setManagerId(request.getManagerId());
        if (request.getStatus() != null) {
            department.setStatus(request.getStatus());
        }

        department = departmentRepository.save(department);
        auditService.log(tenantId, userId, "DEPARTMENT_UPDATED", "DEPARTMENT", department.getId(), null, null);
        return department;
    }

    @Transactional
    public void deleteDepartment(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Department department = getDepartment(id);
        department.setStatus("INACTIVE"); // Soft-deactivation
        departmentRepository.save(department);
        auditService.log(tenantId, userId, "DEPARTMENT_DEACTIVATED", "DEPARTMENT", department.getId(), null, null);
    }
}
