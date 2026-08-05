<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo" @click="router.push('/dashboard')">
        <el-icon :size="24"><Briefcase /></el-icon>
        <span v-show="!isCollapse" class="logo-text">项目管理系统</span>
      </div>
      <el-menu
        :default-active="currentRoute"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>
        <el-menu-item index="/projects">
          <el-icon><Folder /></el-icon>
          <template #title>项目管理</template>
        </el-menu-item>
        <el-menu-item index="/ai-suggestions">
          <el-icon><Cpu /></el-icon>
          <template #title>AI风险建议</template>
        </el-menu-item>
        <el-menu-item index="/todos">
          <el-icon><Finished /></el-icon>
          <template #title>项目待办</template>
        </el-menu-item>
        <el-sub-menu index="operation">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>运营中心</span>
          </template>
          <el-menu-item index="/reports">
            <el-icon><Document /></el-icon>
            <template #title>项目报告</template>
          </el-menu-item>
          <el-menu-item index="/schedule">
            <el-icon><Timer /></el-icon>
            <template #title>定时任务</template>
          </el-menu-item>
        </el-sub-menu>
        <el-sub-menu v-if="userStore.isDeptManager" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/config">
            <el-icon><Setting /></el-icon>
            <template #title>系统配置</template>
          </el-menu-item>
          <el-menu-item index="/users">
            <el-icon><UserFilled /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/roles">
            <el-icon><User /></el-icon>
            <template #title>角色管理</template>
          </el-menu-item>
          <el-menu-item index="/logs">
            <el-icon><Notebook /></el-icon>
            <template #title>操作日志</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主区域 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="20">
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentMeta.title">{{ currentMeta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              <span>{{ userStore.userName }}</span>
              <el-tag size="small" :type="userStore.isDeptManager ? 'danger' : 'primary'" style="margin-left:8px">
                {{ userStore.isDeptManager ? '部门经理' : '项目经理' }}
              </el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人资料
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 个人资料弹窗 -->
  <el-dialog v-model="profileVisible" title="个人资料" width="480px" destroy-on-close>
    <el-form :model="profileForm" label-width="80px">
      <el-form-item label="用户名">
        <el-input :model-value="profileForm.username" disabled />
      </el-form-item>
      <el-form-item label="真实姓名">
        <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="手机号">
        <el-input v-model="profileForm.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="角色">
        <el-tag :type="userStore.isDeptManager ? 'danger' : 'primary'">
          {{ userStore.isDeptManager ? '部门经理' : '项目经理' }}
        </el-tag>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="profileVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存</el-button>
    </template>
  </el-dialog>

  <!-- 修改密码弹窗 -->
  <el-dialog v-model="passwordVisible" title="修改密码" width="440px" destroy-on-close>
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码(至少6位)" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="passwordVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingPassword" @click="changePassword">确认修改</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '../store/user'
import api from '../api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)

const currentRoute = computed(() => {
  const path = route.path
  if (path.startsWith('/projects')) return '/projects'
  return path
})

const currentMeta = computed(() => route.meta || {})

// --- 个人资料 ---
const profileVisible = ref(false)
const savingProfile = ref(false)
const profileForm = reactive({ username: '', realName: '', email: '', phone: '' })

async function openProfile() {
  try {
    const res: any = await api.get('/users/me')
    const u = res.data
    Object.assign(profileForm, { username: u.username, realName: u.realName || '', email: u.email || '', phone: u.phone || '' })
    profileVisible.value = true
  } catch { /* handled */ }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    await api.put('/users/me', { realName: profileForm.realName, email: profileForm.email, phone: profileForm.phone })
    ElMessage.success('资料更新成功')
    profileVisible.value = false
    // 刷新 store 中的用户名
    localStorage.setItem('user', JSON.stringify({ ...JSON.parse(localStorage.getItem('user') || '{}'), realName: profileForm.realName }))
  } catch { /* handled */ }
  savingProfile.value = false
}

// --- 修改密码 ---
const passwordVisible = ref(false)
const savingPassword = ref(false)
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_r: any, val: string, cb: any) => {
        if (val !== pwdForm.newPassword) return cb(new Error('两次输入的密码不一致'))
        cb()
      },
      trigger: 'blur',
    },
  ],
}

async function changePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  savingPassword.value = true
  try {
    await api.put('/users/me/password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    userStore.logout()
  } catch { /* handled */ }
  savingPassword.value = false
}

// --- 下拉命令 ---
function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    userStore.logout()
  } else if (cmd === 'profile') {
    openProfile()
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    passwordVisible.value = true
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  cursor: pointer;
  gap: 8px;
  border-bottom: 1px solid #3a4a5a;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
}

.main {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}
</style>
