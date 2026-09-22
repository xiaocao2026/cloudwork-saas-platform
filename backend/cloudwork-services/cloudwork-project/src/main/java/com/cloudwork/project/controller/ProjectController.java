package com.cloudwork.project.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.cloudwork.project.dto.*;
import com.cloudwork.project.service.ProjectService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresLogin;

@RestController
@RequestMapping
public class ProjectController
{
    @Autowired private ProjectService projectService;

    @RequiresLogin @PostMapping("/projects")
    public AjaxResult createProject(@Validated @RequestBody CreateProjectRequest request) { return AjaxResult.success(projectService.createProject(request)); }
    @RequiresLogin @GetMapping("/projects")
    public AjaxResult listProjects(@RequestParam Long tenantId) { return AjaxResult.success(projectService.listProjects(tenantId)); }
    @RequiresLogin @GetMapping("/projects/{projectId}")
    public AjaxResult getProject(@PathVariable Long projectId, @RequestParam Long tenantId) { return AjaxResult.success(projectService.getProject(tenantId, projectId)); }
    @RequiresLogin @PostMapping("/tasks")
    public AjaxResult createTask(@Validated @RequestBody CreateTaskRequest request) { return AjaxResult.success(projectService.createTask(request)); }
    @RequiresLogin @GetMapping("/tasks")
    public AjaxResult listTasks(@RequestParam Long tenantId, @RequestParam Long projectId) { return AjaxResult.success(projectService.listTasks(tenantId, projectId)); }
    @RequiresLogin @PutMapping("/tasks/{taskId}/status")
    public AjaxResult updateTaskStatus(@PathVariable Long taskId, @Validated @RequestBody UpdateTaskStatusRequest request) { projectService.updateTaskStatus(taskId, request); return AjaxResult.success(); }
}
