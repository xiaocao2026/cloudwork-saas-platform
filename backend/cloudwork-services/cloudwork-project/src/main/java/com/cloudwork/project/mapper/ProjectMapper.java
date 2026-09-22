package com.cloudwork.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.cloudwork.project.domain.Project;
import com.cloudwork.project.vo.ProjectVo;

public interface ProjectMapper
{
    int insert(Project project);
    List<ProjectVo> selectActive(@Param("tenantId") Long tenantId, @Param("status") int status);
    ProjectVo selectById(@Param("projectId") Long projectId, @Param("tenantId") Long tenantId);
}
