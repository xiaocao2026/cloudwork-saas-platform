package com.cloudwork.project.service;

import java.util.List;
import com.cloudwork.project.dto.CreateProjectRequest;
import com.cloudwork.project.dto.CreateTaskRequest;
import com.cloudwork.project.dto.UpdateTaskStatusRequest;
import com.cloudwork.project.vo.ProjectVo;
import com.cloudwork.project.vo.TaskVo;

public interface ProjectService
{
    ProjectVo createProject(CreateProjectRequest request);
    List<ProjectVo> listProjects(Long tenantId);
    ProjectVo getProject(Long tenantId, Long projectId);
    TaskVo createTask(CreateTaskRequest request);
    List<TaskVo> listTasks(Long tenantId, Long projectId);
    void updateTaskStatus(Long taskId, UpdateTaskStatusRequest request);
}
