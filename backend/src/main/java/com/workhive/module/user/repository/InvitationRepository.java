package com.workhive.module.user.repository;

import com.workhive.module.user.entity.Invitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByToken(String token);
    Optional<Invitation> findByTokenAndStatus(String token, String status);
    Page<Invitation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Optional<Invitation> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndEmailAndStatus(UUID tenantId, String email, String status);
}
