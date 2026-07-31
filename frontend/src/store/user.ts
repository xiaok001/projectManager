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

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(
    JSON.parse(localStorage.getItem('user') || 'null')
  )

  const isLoggedIn = computed(() => !!token.value)
  const isDeptManager = computed(() => userInfo.value?.role === 'DEPT_MANAGER')
  const userName = computed(() => userInfo.value?.realName || userInfo.value?.username || '')

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
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    router.push('/login')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isDeptManager,
    userName,
    login,
    logout,
  }
})
