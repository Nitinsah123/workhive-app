package com.workhive.module.document.repository;

import com.workhive.module.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Page<Document> findByTenantIdAndStatus(UUID tenantId, String status, Pageable pageable);
    Page<Document> findByTenantIdAndOwnerIdAndStatus(UUID tenantId, UUID ownerId, String status, Pageable pageable);
    List<Document> findByTenantIdAndEntityTypeAndEntityIdAndStatus(UUID tenantId, String entityType, UUID entityId, String status);
    Optional<Document> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = 'ACTIVE' AND " +
           "(LOWER(d.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(d.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Document> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);

    long countByTenantIdAndStatus(UUID tenantId, String status);
}
