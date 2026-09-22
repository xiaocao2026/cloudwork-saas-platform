package com.cloudwork.tenant.vo;

/** Minimal membership authorization result for internal service calls. */
public class TenantAccessVo
{
    private Long tenantId;
    private Long userId;
    private String roleCode;
    private boolean accessible;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }
}
