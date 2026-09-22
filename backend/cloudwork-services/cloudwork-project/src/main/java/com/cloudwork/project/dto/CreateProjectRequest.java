package com.cloudwork.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest
{
    @NotNull private Long tenantId;
    @NotBlank @Size(max=100) private String projectName;
    @Size(max=500) private String description;
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
