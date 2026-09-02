package com.workhive.module.project.repository;

import com.workhive.module.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {
    List<ProjectMember> findByProjectIdAndTenantId(UUID projectId, UUID tenantId);
    List<ProjectMember> findByUserIdAndTenantId(UUID userId, UUID tenantId);
    Optional<ProjectMember> findByProjectIdAndUserIdAndTenantId(UUID projectId, UUID userId, UUID tenantId);
    boolean existsByProjectIdAndUserIdAndTenantId(UUID projectId, UUID userId, UUID tenantId);
    void deleteByProjectIdAndUserIdAndTenantId(UUID projectId, UUID userId, UUID tenantId);
}
