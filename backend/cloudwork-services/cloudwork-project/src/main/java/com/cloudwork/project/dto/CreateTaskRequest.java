package com.cloudwork.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateTaskRequest
{
    @NotNull private Long tenantId; @NotNull private Long projectId; @NotBlank @Size(max=200) private String title; @Size(max=1000) private String description; private String priority; private Long assigneeUserId;
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;} public Long getAssigneeUserId(){return assigneeUserId;} public void setAssigneeUserId(Long v){assigneeUserId=v;}
}
