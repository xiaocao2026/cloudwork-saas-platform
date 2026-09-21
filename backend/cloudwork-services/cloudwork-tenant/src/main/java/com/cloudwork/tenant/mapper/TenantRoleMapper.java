package com.cloudwork.tenant.mapper;

import com.cloudwork.tenant.domain.TenantRole;

/**
 * Tenant role persistence.
 */
public interface TenantRoleMapper
{
    int insertTenantRole(TenantRole role);
}
