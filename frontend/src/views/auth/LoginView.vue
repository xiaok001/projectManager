<template>
  <div class="login-container">
    <!-- 左侧品牌区 -->
    <div class="brand-section">
      <div class="brand-header">
        <div class="logo-icon">
          <el-icon :size="22"><Briefcase /></el-icon>
        </div>
        <span class="brand-title">PMM 协同管理平台</span>
      </div>

      <div class="brand-content">
        <h1>全局掌控多项目<br>精准把控风险与进度</h1>
        <p>集中化管理所有跨部门项目，实时识别潜在风险，让团队交付更高效、更透明。</p>
        <div class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon blue"><el-icon :size="18"><TrendCharts /></el-icon></div>
            <div class="stat-value">100%</div>
            <div class="stat-label">项目进度透明化</div>
          </div>
          <div class="stat-card">
            <div class="stat-icon orange"><el-icon :size="18"><Warning /></el-icon></div>
            <div class="stat-value">智能识别</div>
            <div class="stat-label">实时风险预警</div>
          </div>
        </div>
      </div>

      <div class="brand-footer">&copy; 2026 PMM System. 保留所有权利。</div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="form-section">
      <div class="login-wrapper">
        <div class="login-header">
          <h2>欢迎回来</h2>
          <p>请输入您的账号信息以登录系统</p>
        </div>

        <el-form ref="formRef" :model="loginForm" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <template #label>
              <span class="form-label">用户名&nbsp;&nbsp;&nbsp;</span>
            </template>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入您的用户名"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <template #label>
              <span class="form-label">登录密码</span>
            </template>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>

          <div class="form-actions">
            <el-checkbox v-model="rememberMe" label="记住账号" />
            <a href="javascript:void(0)" class="forgot-link" @click="router.push('/forgot-password')">忘记密码？</a>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="btn-submit"
            @click="handleLogin"
          >
            立 即 登 录
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Briefcase, TrendCharts, Warning } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, max: 50, message: '账号长度 2-50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度 6-100 个字符', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(loginForm.username, loginForm.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  min-height: 100vh;
  background-color: #f8fafc;
}

/* ========== 左侧品牌区 ========== */
.brand-section {
  flex: 1.1;
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 50%, #0d1b2a 100%);
  color: #fff;
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.brand-section::before {
  content: '';
  position: absolute;
  top: -20%;
  right: -20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(22,119,255,0.15) 0%, rgba(0,0,0,0) 70%);
  border-radius: 50%;
}

.brand-header {
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 2;
}

.logo-icon {
  width: 40px;
  height: 40px;
  background: #1677ff;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(22,119,255,0.4);
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-content {
  z-index: 2;
  max-width: 520px;
  margin: auto 0;
}

.brand-content h1 {
  font-size: 38px;
  font-weight: 700;
  line-height: 1.25;
  margin-bottom: 20px;
  background: linear-gradient(to right, #ffffff, #93c5fd);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-content p {
  font-size: 16px;
  color: #94a3b8;
  line-height: 1.6;
  margin-bottom: 40px;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.stat-card {
  background: rgba(255,255,255,0.05);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  padding: 20px;
  border-radius: 12px;
}

.stat-icon {
  margin-bottom: 10px;
}
.stat-icon.blue { color: #60a5fa; }
.stat-icon.orange { color: #f59e0b; }

.stat-value {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
}

.brand-footer {
  z-index: 2;
  font-size: 13px;
  color: #64748b;
}

/* ========== 右侧登录表单 ========== */
.form-section {
  flex: 0.9;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.login-wrapper {
  width: 100%;
  max-width: 400px;
}

.login-header {
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px;
}

.login-header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.form-label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.forgot-link {
  color: #1677ff;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
}

.forgot-link:hover {
  text-decoration: underline;
}

.btn-submit {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border-radius: 8px;
  background: #1677ff;
  border: none;
  transition: background 0.2s;
}

.btn-submit:hover {
  background: #4096ff;
}

/* 响应式 */
@media (max-width: 900px) {
  .brand-section {
    display: none;
  }
  .form-section {
    flex: 1;
  }
}
</style>
