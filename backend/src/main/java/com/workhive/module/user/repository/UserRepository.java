package com.workhive.module.user.repository;

import com.workhive.module.user.entity.User;
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
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

    Page<User> findByTenantIdAndStatusNot(UUID tenantId, String status, Pageable pageable);

    Page<User> findByTenantId(UUID tenantId, Pageable pageable);

    List<User> findByTenantIdAndDepartmentId(UUID tenantId, UUID departmentId);

    List<User> findByTenantIdAndTeamId(UUID tenantId, UUID teamId);

    List<User> findByTenantIdAndManagerId(UUID tenantId, UUID managerId);
    List<User> findByTenantIdAndManagerIdAndStatus(UUID tenantId, UUID managerId, String status);
    List<User> findByTenantIdAndStatus(UUID tenantId, String status);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.role = :role AND u.status = 'ACTIVE'")
    List<User> findByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId AND u.role = :role AND u.status = 'ACTIVE'")
    long countByTenantIdAndRole(@Param("tenantId") UUID tenantId, @Param("role") String role);

    @Query("SELECT MAX(u.employeeCode) FROM User u WHERE u.tenantId = :tenantId AND u.employeeCode LIKE :prefix%")
    Optional<String> findMaxEmployeeCodeByPrefix(@Param("tenantId") UUID tenantId, @Param("prefix") String prefix);

    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId AND u.status = 'ACTIVE' " +
           "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.employeeCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<User> searchByTenant(@Param("tenantId") UUID tenantId, @Param("q") String query, Pageable pageable);

    long countByTenantId(UUID tenantId);

    boolean existsByEmailAndTenantId(String email, UUID tenantId);
}
