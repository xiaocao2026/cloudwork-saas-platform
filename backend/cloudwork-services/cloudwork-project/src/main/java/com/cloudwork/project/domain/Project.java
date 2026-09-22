package com.cloudwork.project.domain;

import java.util.Date;

public class Project
{
    private Long projectId; private Long tenantId; private String projectCode; private String projectName;
    private String description; private Integer status; private Long createdByUserId; private Date createTime; private Date updateTime; private Integer version;
    public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
    public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
    public String getProjectCode(){return projectCode;} public void setProjectCode(String v){projectCode=v;}
    public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public Long getCreatedByUserId(){return createdByUserId;} public void setCreatedByUserId(Long v){createdByUserId=v;}
    public Date getCreateTime(){return createTime;} public void setCreateTime(Date v){createTime=v;}
    public Date getUpdateTime(){return updateTime;} public void setUpdateTime(Date v){updateTime=v;}
    public Integer getVersion(){return version;} public void setVersion(Integer v){version=v;}
}
