package com.cloudwork.tenant.mapper;

import com.cloudwork.tenant.domain.TenantMember;

/**
 * Tenant member persistence.
 */
public interface TenantMemberMapper
{
    int insertTenantMember(TenantMember member);
}
