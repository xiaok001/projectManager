import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/DashboardView.vue'),
        meta: { title: '首页概览', icon: 'Odometer' },
      },
      {
        path: 'projects',
        name: 'ProjectList',
        component: () => import('../views/project/ProjectListView.vue'),
        meta: { title: '项目管理', icon: 'Folder' },
      },
      {
        path: 'projects/create',
        name: 'ProjectCreate',
        component: () => import('../views/project/ProjectFormView.vue'),
        meta: { title: '创建项目', icon: 'Folder' },
      },
      {
        path: 'projects/:id',
        name: 'ProjectDetail',
        component: () => import('../views/project/ProjectDetailView.vue'),
        meta: { title: '项目详情', icon: 'Folder' },
      },
      {
        path: 'projects/:id/edit',
        name: 'ProjectEdit',
        component: () => import('../views/project/ProjectFormView.vue'),
        meta: { title: '编辑项目', icon: 'Folder' },
      },
      {
        path: 'ai-suggestions',
        name: 'AiSuggestions',
        component: () => import('../views/ai-suggestion/AiSuggestionView.vue'),
        meta: { title: 'AI风险建议', icon: 'Cpu' },
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('../views/report/ReportView.vue'),
        meta: { title: '报告', icon: 'Document' },
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('../views/config/ConfigView.vue'),
        meta: { title: '系统配置', icon: 'Setting', roles: ['DEPT_MANAGER'] },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('../views/user/UserManagementView.vue'),
        meta: { title: '用户管理', icon: 'UserFilled', roles: ['DEPT_MANAGER'] },
      },
      {
        path: 'roles',
        name: 'RoleManagement',
        component: () => import('../views/role/RoleManagementView.vue'),
        meta: { title: '角色管理', icon: 'User', roles: ['DEPT_MANAGER'] },
      },
      {
        path: 'logs',
        name: 'OperationLog',
        component: () => import('../views/log/OperationLogView.vue'),
        meta: { title: '操作日志', icon: 'Notebook', roles: ['DEPT_MANAGER'] },
      },
    ],
  },
  // 404 页面（放在最后，匹配所有未命中的路由）
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('../views/error/NotFoundView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  const user = userStr ? JSON.parse(userStr) : null

  // 未登录 → 跳登录页（登录页本身除外）
  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
    return
  }

  // 已登录访问登录页 → 跳首页
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }

  // 角色权限校验
  if (to.meta.roles && user) {
    const allowedRoles = to.meta.roles as string[]
    if (!allowedRoles.includes(user.role)) {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
