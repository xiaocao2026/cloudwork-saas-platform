package com.cloudwork.tenant.domain;

import java.util.Date;

/**
 * Fields required to insert the workspace creator as an active member.
 */
public class TenantMember
{
    private Long memberId;

    private Long tenantId;

    private Long userId;

    private Long roleId;

    private Integer memberStatus;

    private Date joinedAt;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getRoleId()
    {
        return roleId;
    }

    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    public Integer getMemberStatus()
    {
        return memberStatus;
    }

    public void setMemberStatus(Integer memberStatus)
    {
        this.memberStatus = memberStatus;
    }

    public Date getJoinedAt()
    {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt)
    {
        this.joinedAt = joinedAt;
    }
}
