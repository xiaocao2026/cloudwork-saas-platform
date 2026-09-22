import request from '@/utils/request'

export function createProject(data) {
  return request({ url: '/project/projects', method: 'post', data })
}

export function listProjects(tenantId) {
  return request({ url: '/project/projects', method: 'get', params: { tenantId } })
}

export function getProject(projectId, tenantId) {
  return request({ url: '/project/projects/' + projectId, method: 'get', params: { tenantId } })
}

export function createTask(data) {
  return request({ url: '/project/tasks', method: 'post', data })
}

export function listTasks(tenantId, projectId) {
  return request({ url: '/project/tasks', method: 'get', params: { tenantId, projectId } })
}

export function updateTaskStatus(taskId, data) {
  return request({ url: '/project/tasks/' + taskId + '/status', method: 'put', data })
}
