<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="card-header">
        <el-icon :size="40" color="#1677ff"><Lock /></el-icon>
        <h2>重置密码</h2>
        <p>请按步骤验证您的身份，系统将发送新密码至您的邮箱</p>
      </div>

      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom:32px">
        <el-step title="验证账号" />
        <el-step title="验证邮箱" />
        <el-step title="重置完成" />
      </el-steps>

      <!-- Step 1: 验证账号 -->
      <div v-if="currentStep === 0" class="step-content">
        <el-form ref="usernameFormRef" :model="usernameForm" :rules="usernameRules" label-width="0">
          <el-form-item prop="username">
            <el-input
              v-model="usernameForm.username"
              placeholder="请输入您的登录账号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>
        </el-form>
        <el-button type="primary" size="large" class="step-btn" :loading="loading" @click="handleVerifyUsername">
          下一步
        </el-button>
      </div>

      <!-- Step 2: 验证邮箱 -->
      <div v-if="currentStep === 1" class="step-content">
        <div class="verify-tip">
          <el-icon><InfoFilled /></el-icon>
          请确认账号 <strong>{{ usernameForm.username }}</strong> 绑定的邮箱地址
        </div>
        <el-form ref="emailFormRef" :model="emailForm" :rules="emailRules" label-width="0">
          <el-form-item prop="email">
            <el-input
              v-model="emailForm.email"
              placeholder="请输入账号绑定的邮箱地址"
              size="large"
              :prefix-icon="Message"
            />
          </el-form-item>
        </el-form>
        <div class="step-actions">
          <el-button size="large" @click="currentStep = 0">上一步</el-button>
          <el-button type="primary" size="large" :loading="loading" @click="handleVerifyEmail">
            发送新密码
          </el-button>
        </div>
      </div>

      <!-- Step 3: 完成 -->
      <div v-if="currentStep === 2" class="step-content success-content">
        <el-icon :size="64" color="#67c23a"><CircleCheckFilled /></el-icon>
        <h3>新密码已发送</h3>
        <p>系统已将新密码发送至您的邮箱 <strong>{{ emailForm.email }}</strong></p>
        <p class="tip">请登录后及时修改密码，如未收到邮件请检查垃圾箱</p>
        <el-button type="primary" size="large" class="step-btn" @click="router.push('/login')">
          返回登录
        </el-button>
      </div>

      <div class="back-login">
        <el-icon><Back /></el-icon>
        <a href="javascript:void(0)" @click="router.push('/login')">返回登录页面</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock, Message, InfoFilled, CircleCheckFilled, Back } from '@element-plus/icons-vue'
import api from '../../api'

const router = useRouter()
const loading = ref(false)
const currentStep = ref(0)

// Step 1
const usernameFormRef = ref<FormInstance>()
const usernameForm = reactive({ username: '' })
const usernameRules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
}

// Step 2
const emailFormRef = ref<FormInstance>()
const emailForm = reactive({ email: '' })
const emailRules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

async function handleVerifyUsername() {
  const valid = await usernameFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await api.post('/auth/verify-username', { username: usernameForm.username })
    currentStep.value = 1
  } catch { /* handled by interceptor */ }
  loading.value = false
}

async function handleVerifyEmail() {
  const valid = await emailFormRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await api.post('/auth/reset-password', {
      username: usernameForm.username,
      email: emailForm.email,
    })
    currentStep.value = 2
    ElMessage.success('新密码已发送至您的邮箱')
  } catch { /* handled by interceptor */ }
  loading.value = false
}
</script>

<style scoped>
.forgot-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0d1b2a 0%, #1b263b 50%, #0d1b2a 100%);
  padding: 20px;
}

.forgot-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}

.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.card-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 12px 0 8px;
}

.card-header p {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.step-content {
  margin-top: 8px;
}

.step-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 8px;
  margin-top: 8px;
}

.step-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}

.step-actions .el-button {
  flex: 1;
  height: 44px;
  border-radius: 8px;
}

.verify-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f0f5ff;
  border-radius: 8px;
  color: #1677ff;
  font-size: 14px;
  margin-bottom: 20px;
}

.success-content {
  text-align: center;
  padding: 20px 0;
}

.success-content h3 {
  font-size: 20px;
  color: #0f172a;
  margin: 16px 0 8px;
}

.success-content p {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 4px;
}

.success-content .tip {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 24px;
}

.back-login {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
  font-size: 14px;
  color: #6b7280;
}

.back-login a {
  color: #1677ff;
  text-decoration: none;
}

.back-login a:hover {
  text-decoration: underline;
}
</style>
