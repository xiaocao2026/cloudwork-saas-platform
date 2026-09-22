package com.cloudwork.project.domain;

import java.util.Date;

public class Task
{
    private Long taskId; private Long tenantId; private Long projectId; private String title; private String description;
    private String status; private String priority; private Long assigneeUserId; private Long createdByUserId; private Date createTime; private Date updateTime; private Integer version;
    public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;}
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getPriority(){return priority;} public void setPriority(String v){priority=v;}
    public Long getAssigneeUserId(){return assigneeUserId;} public void setAssigneeUserId(Long v){assigneeUserId=v;}
    public Long getCreatedByUserId(){return createdByUserId;} public void setCreatedByUserId(Long v){createdByUserId=v;}
    public Date getCreateTime(){return createTime;} public void setCreateTime(Date v){createTime=v;}
    public Date getUpdateTime(){return updateTime;} public void setUpdateTime(Date v){updateTime=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){version=v;}
}
