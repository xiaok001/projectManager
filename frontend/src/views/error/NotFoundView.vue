<template>
  <div class="not-found-page">
    <div class="content">
      <div class="illustration">
        <div class="code">404</div>
        <div class="astronaut">
          <el-icon :size="80"><Warning /></el-icon>
        </div>
      </div>
      <h1 class="title">页面迷路了</h1>
      <p class="desc">抱歉，您访问的页面不存在或已被移除</p>
      <div class="actions">
        <el-button type="primary" size="large" @click="goHome">
          <el-icon><HomeFilled /></el-icon> 返回首页
        </el-button>
        <el-button size="large" @click="goBack">
          <el-icon><Back /></el-icon> 返回上一页
        </el-button>
      </div>
      <div class="tip">
        <el-icon><InfoFilled /></el-icon>
        如果您认为这是一个错误，请联系系统管理员
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { Warning, HomeFilled, Back, InfoFilled } from '@element-plus/icons-vue'

const router = useRouter()

function goHome() {
  router.push('/dashboard')
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/dashboard')
  }
}
</script>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7ed 100%);
}

.content {
  text-align: center;
  padding: 40px;
}

.illustration {
  position: relative;
  margin-bottom: 32px;
}

.code {
  font-size: 160px;
  font-weight: 900;
  background: linear-gradient(135deg, #409eff 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  letter-spacing: 8px;
  opacity: 0.15;
}

.astronaut {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(-50%, -50%); }
  50% { transform: translate(-50%, calc(-50% - 12px)); }
}

.title {
  font-size: 28px;
  color: #303133;
  margin: 0 0 12px;
  font-weight: 700;
}

.desc {
  font-size: 16px;
  color: #909399;
  margin: 0 0 36px;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
}

.tip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #c0c4cc;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
}
</style>
