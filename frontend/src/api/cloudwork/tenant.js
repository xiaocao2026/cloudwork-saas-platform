import request from '@/utils/request'

export function createWorkspace(data) {
  return request({ url: '/tenant/workspaces', method: 'post', data })
}

export function listMyWorkspaces() {
  return request({ url: '/tenant/workspaces/mine', method: 'get' })
}

export function getWorkspace(tenantId) {
  return request({ url: '/tenant/workspaces/' + tenantId, method: 'get' })
}
