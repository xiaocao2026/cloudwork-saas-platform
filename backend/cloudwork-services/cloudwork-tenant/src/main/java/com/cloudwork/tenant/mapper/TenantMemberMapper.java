package com.cloudwork.tenant.mapper;

import com.cloudwork.tenant.domain.TenantMember;
import com.cloudwork.tenant.vo.TenantAccessVo;
import org.apache.ibatis.annotations.Param;

/**
 * Tenant member persistence.
 */
public interface TenantMemberMapper
{
    int insertTenantMember(TenantMember member);

    TenantAccessVo selectAccess(@Param("tenantId") Long tenantId,
            @Param("userId") Long userId, @Param("memberStatus") int memberStatus);
}
