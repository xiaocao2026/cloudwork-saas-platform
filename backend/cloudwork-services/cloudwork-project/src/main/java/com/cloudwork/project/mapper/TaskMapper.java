package com.cloudwork.project.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.cloudwork.project.domain.Task;
import com.cloudwork.project.vo.TaskVo;

public interface TaskMapper
{
    int insert(Task task);
    List<TaskVo> selectByProject(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
    int updateStatus(@Param("taskId") Long taskId, @Param("tenantId") Long tenantId, @Param("status") String status);
    int countProject(@Param("projectId") Long projectId, @Param("tenantId") Long tenantId);
}
