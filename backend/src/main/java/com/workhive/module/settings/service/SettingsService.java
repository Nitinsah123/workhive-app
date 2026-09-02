package com.workhive.module.settings.service;

import com.workhive.module.audit.service.AuditService;
import com.workhive.module.settings.entity.OrgSetting;
import com.workhive.module.settings.repository.OrgSettingRepository;
import com.workhive.module.tenant.entity.Tenant;
import com.workhive.module.tenant.repository.TenantRepository;
import com.workhive.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SettingsService {

    private final OrgSettingRepository orgSettingRepository;
    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public SettingsService(OrgSettingRepository orgSettingRepository,
                           TenantRepository tenantRepository,
                           AuditService auditService) {
        this.orgSettingRepository = orgSettingRepository;
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    public Map<String, Object> getSettings() {
        UUID tenantId = TenantContext.requireTenantId();
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);

        Map<String, Object> result = new HashMap<>();
        if (tenant != null) {
            result.put("organizationName", tenant.getName());
            result.put("organizationCode", tenant.getCode());
            result.put("industry", tenant.getIndustry());
            result.put("timezone", tenant.getTimezone());
            result.put("workingDays", tenant.getWorkingDays());
            result.put("logoUrl", tenant.getLogoUrl());
        }

        List<OrgSetting> customSettings = orgSettingRepository.findByTenantId(tenantId);
        for (OrgSetting s : customSettings) {
            result.put(s.getKey(), s.getValue());
        }

        return result;
    }

    @Transactional
    public void updateSettings(Map<String, String> settings) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID userId = TenantContext.requireUserId();

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant != null) {
            if (settings.containsKey("organizationName")) tenant.setName(settings.get("organizationName"));
            if (settings.containsKey("industry")) tenant.setIndustry(settings.get("industry"));
            if (settings.containsKey("timezone")) tenant.setTimezone(settings.get("timezone"));
            if (settings.containsKey("workingDays")) tenant.setWorkingDays(settings.get("workingDays"));
            if (settings.containsKey("logoUrl")) tenant.setLogoUrl(settings.get("logoUrl"));
            tenantRepository.save(tenant);
        }

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            if (!entry.getKey().startsWith("organization") && !entry.getKey().equals("industry")
                    && !entry.getKey().equals("timezone") && !entry.getKey().equals("workingDays")
                    && !entry.getKey().equals("logoUrl")) {

                OrgSetting setting = orgSettingRepository.findByTenantIdAndKey(tenantId, entry.getKey())
                        .orElseGet(() -> OrgSetting.builder()
                                .tenantId(tenantId)
                                .key(entry.getKey())
                                .build());

                setting.setValue(entry.getValue());
                orgSettingRepository.save(setting);
            }
        }

        auditService.log(tenantId, userId, "SETTINGS_UPDATED", "ORGANIZATION", tenantId, null, null);
    }
}
