package com.workhive.module.project.repository;

import com.workhive.module.project.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, UUID> {
    List<Milestone> findByProjectIdAndTenantId(UUID projectId, UUID tenantId);
    Optional<Milestone> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByProjectIdAndTenantIdAndStatus(UUID projectId, UUID tenantId, String status);
}
