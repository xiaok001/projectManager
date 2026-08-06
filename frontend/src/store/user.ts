import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api'
import router from '../router'

interface UserInfo {
  userId: number
  username: string
  realName: string
  role: string
}

interface Permission {
  id: number
  parentId: number
  permName: string
  permKey: string
  type: string  // menu / button
  path: string | null
  icon: string | null
  children?: Permission[]
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('user') || 'null')
  )
  const permissions = ref<Permission[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const isDeptManager = computed(() => userInfo.value?.role === 'DEPT_MANAGER')
  const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')

  // 扁平化权限 key 列表，用于快速查找
  const permKeys = computed(() => {
    const keys = new Set<string>()
    const flatten = (list: Permission[]) => {
      for (const p of list) {
        keys.add(p.permKey)
        if (p.children) flatten(p.children)
      }
    }
    flatten(permissions.value)
    return keys
  })

  // 判断是否有某个权限
  function hasPermission(permKey: string): boolean {
    // DEPT_MANAGER 拥有全部权限
    if (isDeptManager.value) return true
    return permKeys.value.has(permKey)
  }

  // 获取菜单权限列表（type=menu）
  const menuPermissions = computed(() => {
    return permissions.value.filter(p => p.type === 'menu')
  })

  async function login(username: string, password: string) {
    const res: any = await authApi.login({ username, password })
    const data = res.data
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role,
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(userInfo.value))

    // 登录后加载权限
    await loadPermissions()
  }

  async function loadPermissions() {
    try {
      const res: any = await authApi.getPermissions()
      permissions.value = res.data || []
    } catch {
      permissions.value = []
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    isDeptManager,
    userName,
    permKeys,
    menuPermissions,
    hasPermission,
    login,
    loadPermissions,
    logout,
  }
})
