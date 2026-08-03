import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// 请求拦截器: 附加 Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器: 统一错误处理
api.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      } else if (status === 403) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
        ElMessage.error('无权操作，请重新登录')
      } else {
        ElMessage.error(data?.message || '服务器错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

// ============ 认证接口 ============
export const authApi = {
  login: (data: { username: string; password: string }) =>
    api.post('/auth/login', data),
  getUsers: () => api.get('/auth/users'),
}

// ============ 项目接口 ============
export const projectApi = {
  list: () => api.get('/projects'),
  detail: (id: number) => api.get(`/projects/${id}`),
  create: (data: any) => api.post('/projects', data),
  update: (id: number, data: any) => api.put(`/projects/${id}`, data),
  updateSatisfaction: (id: number, score: number) =>
    api.put(`/projects/${id}/satisfaction`, { score }),
}

// ============ 阶段接口 ============
export const stageApi = {
  listByProject: (projectId: number) => api.get(`/projects/${projectId}/stages`),
  update: (id: number, data: any) => api.put(`/stages/${id}`, data),
}

// ============ 风险接口 ============
export const riskApi = {
  listByProject: (projectId: number) => api.get(`/projects/${projectId}/risks`),
  aggregated: () => api.get('/risks/aggregated'),
  create: (projectId: number, data: any) => api.post(`/projects/${projectId}/risks`, data),
  update: (id: number, data: any) => api.put(`/risks/${id}`, data),
  staleOverride: (id: number, staleOverride: boolean) =>
    api.put(`/risks/${id}/stale-override`, { staleOverride }),
}

// ============ Dashboard接口 ============
export const dashboardApi = {
  summary: () => api.get('/dashboard/summary'),
  healthList: () => api.get('/dashboard/health'),
  healthDetail: (projectId: number) => api.get(`/dashboard/health/${projectId}`),
}

// ============ 配置接口 ============
export const configApi = {
  list: () => api.get('/config'),
  update: (configs: any[]) => api.put('/config', configs),
}

// ============ 邮件摘要接口 ============
export const digestApi = {
  sendNow: () => api.post('/digest/send-now'),
  testEmail: (email: string) => api.post(`/digest/test?email=${encodeURIComponent(email)}`),
  logs: (pageNum = 1, pageSize = 10) =>
    api.get('/digest/logs', { params: { pageNum, pageSize } }),
}

// ============ 变更记录接口 ============
export const changeLogApi = {
  list: (projectId: number) => api.get(`/projects/${projectId}/changes`),
  create: (projectId: number, data: any) =>
    api.post(`/projects/${projectId}/changes`, data),
}

// ============ AI建议接口 ============
export const aiSuggestionApi = {
  list: (status?: string) =>
    api.get('/ai-suggestions', { params: status ? { status } : {} }),
  accept: (id: number) => api.post(`/ai-suggestions/${id}/accept`),
  ignore: (id: number) => api.post(`/ai-suggestions/${id}/ignore`),
}

// ============ 用户管理接口 ============
export const userApi = {
  list: () => api.get('/users'),
  detail: (id: number) => api.get(`/users/${id}`),
  create: (data: any) => api.post('/users', data),
  update: (id: number, data: any) => api.put(`/users/${id}`, data),
  delete: (id: number) => api.delete(`/users/${id}`),
}

// ============ 角色管理接口 ============
export const roleApi = {
  list: () => api.get('/roles'),
  create: (data: any) => api.post('/roles', data),
  update: (id: number, data: any) => api.put(`/roles/${id}`, data),
  delete: (id: number) => api.delete(`/roles/${id}`),
  getPermissions: (id: number) => api.get(`/roles/${id}/permissions`),
  assignPermissions: (id: number, permissionIds: number[]) =>
    api.put(`/roles/${id}/permissions`, { permissionIds }),
}

// ============ 权限接口 ============
export const permissionApi = {
  tree: () => api.get('/permissions/tree'),
  treeWithChecked: (roleId: number) => api.get(`/permissions/tree/${roleId}`),
}

// ============ 操作日志接口 ============
export const operationLogApi = {
  list: (params: { pageNum?: number; pageSize?: number; module?: string; operation?: string }) =>
    api.get('/operation-logs', { params }),
}

export default api
