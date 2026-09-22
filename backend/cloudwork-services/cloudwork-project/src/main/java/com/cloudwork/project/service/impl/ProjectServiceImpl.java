package com.cloudwork.project.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudwork.project.constant.ProjectConstants;
import com.cloudwork.project.domain.Project;
import com.cloudwork.project.domain.Task;
import com.cloudwork.project.dto.CreateProjectRequest;
import com.cloudwork.project.dto.CreateTaskRequest;
import com.cloudwork.project.dto.UpdateTaskStatusRequest;
import com.cloudwork.project.mapper.ProjectMapper;
import com.cloudwork.project.mapper.TaskMapper;
import com.cloudwork.project.remote.TenantAccessRemoteService;
import com.cloudwork.project.remote.TenantAccessVo;
import com.cloudwork.project.service.ProjectService;
import com.cloudwork.project.vo.ProjectVo;
import com.cloudwork.project.vo.TaskVo;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.security.utils.SecurityUtils;

@Service
public class ProjectServiceImpl implements ProjectService
{
    @Autowired private ProjectMapper projectMapper;
    @Autowired private TaskMapper taskMapper;
    @Autowired private TenantAccessRemoteService tenantAccessRemoteService;

    @Override @Transactional(rollbackFor = Exception.class)
    public ProjectVo createProject(CreateProjectRequest request)
    {
        ensureAccess(request.getTenantId());
        Project project = new Project();
        project.setTenantId(request.getTenantId()); project.setProjectCode("prj_" + IdUtils.fastSimpleUUID());
        project.setProjectName(request.getProjectName().trim()); project.setDescription(request.getDescription());
        project.setStatus(ProjectConstants.PROJECT_ACTIVE); project.setCreatedByUserId(currentUserId());
        if (projectMapper.insert(project) != 1) throw new ServiceException("创建项目失败");
        return projectMapper.selectById(project.getProjectId(), project.getTenantId());
    }

    @Override public List<ProjectVo> listProjects(Long tenantId) { ensureAccess(tenantId); return projectMapper.selectActive(tenantId, ProjectConstants.PROJECT_ACTIVE); }

    @Override public ProjectVo getProject(Long tenantId, Long projectId)
    {
        ensureAccess(tenantId); ProjectVo project = projectMapper.selectById(projectId, tenantId);
        if (project == null) throw new ServiceException("项目不存在或无权访问", HttpStatus.NOT_FOUND);
        return project;
    }

    @Override @Transactional(rollbackFor = Exception.class)
    public TaskVo createTask(CreateTaskRequest request)
    {
        ensureAccess(request.getTenantId());
        if (taskMapper.countProject(request.getProjectId(), request.getTenantId()) != 1)
            throw new ServiceException("项目不存在或已归档", HttpStatus.NOT_FOUND);
        Task task = new Task(); task.setTenantId(request.getTenantId()); task.setProjectId(request.getProjectId());
        task.setTitle(request.getTitle().trim()); task.setDescription(request.getDescription()); task.setStatus(ProjectConstants.TASK_TODO);
        task.setPriority(request.getPriority() == null || request.getPriority().isBlank() ? ProjectConstants.PRIORITY_MEDIUM : request.getPriority());
        task.setAssigneeUserId(request.getAssigneeUserId()); task.setCreatedByUserId(currentUserId());
        if (taskMapper.insert(task) != 1) throw new ServiceException("创建任务失败");
        return taskMapper.selectByProject(task.getTenantId(), task.getProjectId()).stream().filter(t -> task.getTaskId().equals(t.getTaskId())).findFirst().orElse(null);
    }

    @Override public List<TaskVo> listTasks(Long tenantId, Long projectId) { ensureAccess(tenantId); if (taskMapper.countProject(projectId, tenantId) != 1) throw new ServiceException("项目不存在或已归档", HttpStatus.NOT_FOUND); return taskMapper.selectByProject(tenantId, projectId); }

    @Override public void updateTaskStatus(Long taskId, UpdateTaskStatusRequest request)
    {
        ensureAccess(request.getTenantId()); validateStatus(request.getStatus());
        if (taskMapper.updateStatus(taskId, request.getTenantId(), request.getStatus()) != 1) throw new ServiceException("任务不存在或无权访问", HttpStatus.NOT_FOUND);
    }

    private void validateStatus(String status) { if (!ProjectConstants.TASK_TODO.equals(status) && !ProjectConstants.TASK_DOING.equals(status) && !ProjectConstants.TASK_DONE.equals(status)) throw new ServiceException("任务状态只允许 TODO、DOING、DONE", HttpStatus.BAD_REQUEST); }
    private Long currentUserId() { Long id = SecurityUtils.getUserId(); if (id == null) throw new ServiceException("登录身份无效", HttpStatus.UNAUTHORIZED); return id; }
    private void ensureAccess(Long tenantId)
    {
        currentUserId(); R<TenantAccessVo> result = tenantAccessRemoteService.checkAccess(tenantId);
        if (result == null || !R.isSuccess(result) || result.getData() == null || !result.getData().isAccessible()) throw new ServiceException("无权访问该Workspace", HttpStatus.FORBIDDEN);
    }
}
