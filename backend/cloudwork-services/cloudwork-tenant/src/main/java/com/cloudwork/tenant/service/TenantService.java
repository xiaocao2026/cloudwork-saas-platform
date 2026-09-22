package com.cloudwork.tenant.service;

import java.util.List;
import com.cloudwork.tenant.dto.CreateTenantRequest;
import com.cloudwork.tenant.vo.TenantSummaryVo;
import com.cloudwork.tenant.vo.TenantAccessVo;

/**
 * Workspace use cases.
 */
public interface TenantService
{
    TenantSummaryVo createTenant(CreateTenantRequest request);

    List<TenantSummaryVo> listMine();

    TenantSummaryVo getAccessibleTenant(Long tenantId);

    TenantAccessVo checkAccess(Long tenantId);
}
