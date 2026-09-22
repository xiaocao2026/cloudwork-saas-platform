package com.cloudwork.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateTaskStatusRequest
{
    @NotNull private Long tenantId; @NotBlank private String status;
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
}
