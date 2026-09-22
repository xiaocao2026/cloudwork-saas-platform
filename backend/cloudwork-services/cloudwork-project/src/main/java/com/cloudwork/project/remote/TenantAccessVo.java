package com.cloudwork.project.remote;

public class TenantAccessVo
{
    private Long tenantId; private Long userId; private String roleCode; private boolean accessible;
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getRoleCode(){return roleCode;} public void setRoleCode(String v){roleCode=v;} public boolean isAccessible(){return accessible;} public void setAccessible(boolean v){accessible=v;}
}
