package com.workhive.module.user.repository;

import com.workhive.module.user.entity.EmailConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailConnectionRepository extends JpaRepository<EmailConnection, UUID> {

    Optional<EmailConnection> findByTenantIdAndUserIdAndProvider(UUID tenantId, UUID userId, String provider);

    Optional<EmailConnection> findByTenantIdAndEmailAddressAndProvider(UUID tenantId, String emailAddress, String provider);

    Optional<EmailConnection> findByTenantIdAndProvider(UUID tenantId, String provider);

    Optional<EmailConnection> findByEmailAddressAndProvider(String emailAddress, String provider);

    Optional<EmailConnection> findFirstByTenantIdAndStatus(UUID tenantId, String status);

    Optional<EmailConnection> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<EmailConnection> findByOauthState(String oauthState);

    Optional<EmailConnection> findByTenantIdAndUserIdAndStatus(UUID tenantId, UUID userId, String status);

    boolean existsByTenantIdAndUserIdAndProvider(UUID tenantId, UUID userId, String provider);
}
