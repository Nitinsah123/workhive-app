package com.workhive.module.integration.repository;

import com.workhive.module.integration.entity.IntegrationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationMappingRepository extends JpaRepository<IntegrationMapping, UUID> {
    List<IntegrationMapping> findByTenantIdAndIntegrationId(UUID tenantId, UUID integrationId);
    Optional<IntegrationMapping> findByTenantIdAndExternalIdAndExternalType(UUID tenantId, String externalId, String externalType);
    List<IntegrationMapping> findByTenantIdAndWorkhiveEntityTypeAndWorkhiveEntityId(UUID tenantId, String workhiveEntityType, UUID workhiveEntityId);
}
