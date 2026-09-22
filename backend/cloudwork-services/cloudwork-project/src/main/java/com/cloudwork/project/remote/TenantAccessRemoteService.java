package com.cloudwork.project.remote;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.ruoyi.common.core.domain.R;

@FeignClient(contextId = "tenantAccessRemoteService", name = "cloudwork-tenant")
public interface TenantAccessRemoteService
{
    @GetMapping("/internal/access/{tenantId}")
    R<TenantAccessVo> checkAccess(@PathVariable("tenantId") Long tenantId);
}
