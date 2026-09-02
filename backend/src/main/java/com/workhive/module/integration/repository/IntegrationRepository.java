package com.workhive.module.integration.repository;

import com.workhive.module.integration.entity.Integration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationRepository extends JpaRepository<Integration, UUID> {
    List<Integration> findByTenantId(UUID tenantId);
    List<Integration> findByTenantIdAndConnectedBy(UUID tenantId, UUID connectedBy);
    Optional<Integration> findByTenantIdAndProvider(UUID tenantId, String provider);
    Optional<Integration> findByTenantIdAndConnectedByAndProvider(UUID tenantId, UUID connectedBy, String provider);
    Optional<Integration> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Integration> findByIdAndTenantIdAndConnectedBy(UUID id, UUID tenantId, UUID connectedBy);
    boolean existsByTenantIdAndProvider(UUID tenantId, String provider);
    boolean existsByTenantIdAndConnectedByAndProvider(UUID tenantId, UUID connectedBy, String provider);
    List<Integration> findByTenantIdAndProviderAndStatus(UUID tenantId, String provider, String status);
    List<Integration> findByTenantIdAndConnectedByAndStatus(UUID tenantId, UUID connectedBy, String status);
    Optional<Integration> findByTenantIdAndProviderAndExternalUsername(UUID tenantId, String provider, String externalUsername);
}
