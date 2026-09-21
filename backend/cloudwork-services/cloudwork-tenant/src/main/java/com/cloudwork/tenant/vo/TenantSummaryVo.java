package com.cloudwork.tenant.vo;

/**
 * Workspace information visible to an active member.
 */
public class TenantSummaryVo
{
    private Long tenantId;

    private String tenantCode;

    private String tenantName;

    private Integer status;

    private String roleCode;

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getTenantCode()
    {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode)
    {
        this.tenantCode = tenantCode;
    }

    public String getTenantName()
    {
        return tenantName;
    }

    public void setTenantName(String tenantName)
    {
        this.tenantName = tenantName;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getRoleCode()
    {
        return roleCode;
    }

    public void setRoleCode(String roleCode)
    {
        this.roleCode = roleCode;
    }
}
