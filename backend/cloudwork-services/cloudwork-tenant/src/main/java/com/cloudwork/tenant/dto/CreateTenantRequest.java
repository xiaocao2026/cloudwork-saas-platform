package com.cloudwork.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Only the workspace name is client-controlled.
 */
public class CreateTenantRequest
{
    @NotBlank(message = "Workspace名称不能为空")
    @Size(min = 2, max = 100, message = "Workspace名称长度必须在2到100个字符之间")
    private String tenantName;

    public String getTenantName()
    {
        return tenantName;
    }

    public void setTenantName(String tenantName)
    {
        this.tenantName = tenantName;
    }
}
