package com.workhive.module.settings.repository;

import com.workhive.module.settings.entity.OrgSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgSettingRepository extends JpaRepository<OrgSetting, UUID> {
    List<OrgSetting> findByTenantId(UUID tenantId);
    Optional<OrgSetting> findByTenantIdAndKey(UUID tenantId, String key);
}
