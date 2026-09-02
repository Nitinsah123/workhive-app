package com.workhive.module.tenant.repository;

import com.workhive.module.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findByCode(String code);
    boolean existsBySlug(String slug);
    boolean existsByCode(String code);
}
