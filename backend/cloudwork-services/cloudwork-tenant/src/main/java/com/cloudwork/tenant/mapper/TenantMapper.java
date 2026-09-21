package com.cloudwork.tenant.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.cloudwork.tenant.domain.Tenant;
import com.cloudwork.tenant.vo.TenantSummaryVo;

/**
 * Workspace persistence and membership-scoped reads.
 */
public interface TenantMapper
{
    int insertTenant(Tenant tenant);

    List<TenantSummaryVo> selectMine(@Param("userId") Long userId,
            @Param("memberStatus") int memberStatus);

    TenantSummaryVo selectAccessibleById(@Param("tenantId") Long tenantId,
            @Param("userId") Long userId, @Param("memberStatus") int memberStatus);
}
