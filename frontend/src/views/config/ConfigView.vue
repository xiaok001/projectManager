<template>
  <div class="page-container">
    <!-- Sticky 顶部操作栏 -->
    <div class="config-topbar">
      <div class="topbar-left">
        <el-icon><Setting /></el-icon>
        <span>系统配置</span>
      </div>
      <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
    </div>

    <!-- 左右两栏布局 -->
    <div class="config-layout" v-loading="loading">
      <!-- 左侧导航 -->
      <nav class="config-nav">
        <div
          v-for="group in navGroups"
          :key="group.key"
          :class="['nav-item', { active: activeGroup === group.key }]"
          @click="activeGroup = group.key"
        >
          <el-icon><component :is="group.icon" /></el-icon>
          <span>{{ group.label }}</span>
        </div>
      </nav>

      <!-- 右侧内容区 -->
      <div class="config-content">

        <!-- 基础配置 -->
        <template v-if="activeGroup === 'basic'">
          <el-card shadow="never" class="config-card">
            <template #header><span>风险停滞配置</span></template>
            <el-form label-width="140px">
              <el-form-item label="停滞判定天数">
                <el-input-number v-model="configMap.stale_threshold_days" :min="1" :max="30" />
              </el-form-item>
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>邮件摘要配置</span></template>
            <el-form label-width="140px">
              <el-form-item label="每日发送时间">
                <el-time-picker v-model="digestTime" format="HH:mm" value-format="HH:mm" placeholder="选择时间" />
              </el-form-item>
              <el-form-item label="收件人邮箱">
                <el-input v-model="configMap.digest_recipient_emails" placeholder="多个邮箱用逗号分隔">
                  <template #append>
                    <el-button :loading="testingEmail" @click="handleTestEmail">测试发送</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <!-- 评分配置 -->
        <template v-if="activeGroup === 'score'">
          <el-card shadow="never" class="config-card">
            <template #header>
              <span>健康评分权重</span>
              <el-tag :type="weightSumValid ? 'success' : 'danger'" size="small">权重总和: {{ weightSum }}%</el-tag>
            </template>
            <el-form label-width="140px">
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="时间维度(%)">
                    <el-input-number v-model="configMap.health_weight_time" :min="0" :max="100" style="width:100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="风险维度(%)">
                    <el-input-number v-model="configMap.health_weight_risk" :min="0" :max="100" style="width:100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="交付维度(%)">
                    <el-input-number v-model="configMap.health_weight_delivery" :min="0" :max="100" style="width:100%" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>健康度颜色阈值</span></template>
            <el-form label-width="140px">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="绿色最低分">
                    <el-input-number v-model="configMap.health_score_green_min" :min="0" :max="100"
                      :class="{ 'is-error': !thresholdValid }" style="width:100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="黄色最低分">
                    <el-input-number v-model="configMap.health_score_yellow_min" :min="0" :max="100"
                      :class="{ 'is-error': !thresholdValid }" style="width:100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-alert v-if="!thresholdValid" title="绿色阈值需大于黄色阈值" type="error" :closable="false" show-icon />
            </el-form>
          </el-card>

          <el-card shadow="never" class="config-card">
            <template #header><span>风险扣分系数</span></template>
            <el-form label-width="140px">
              <el-form-item label="每天延期扣分">
                <el-input-number v-model="configMap.time_delay_penalty_per_day" :min="0" :max="20" />
              </el-form-item>
              <el-row :gutter="20">
                <el-col :span="8">
                  <el-form-item label="高危风险">
                    <el-input-number v-model="configMap.risk_penalty_high" :min="0" :max="50" style="width:100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="中危风险">
                    <el-input-number v-model="configMap.risk_penalty_medium" :min="0" :max="50" style="width:100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="8">
                  <el-form-item label="低危风险">
                    <el-input-number v-model="configMap.risk_penalty_low" :min="0" :max="50" style="width:100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="停滞风险扣分">
                <el-input-number v-model="configMap.risk_penalty_stale" :min="0" :max="50" />
              </el-form-item>
            </el-form>
          </el-card>
        </template>

        <!-- AI配置 -->
        <template v-if="activeGroup === 'ai'">
          <el-card shadow="never" class="config-card">
            <template #header>
              <span>AI 服务配置</span>
              <el-button type="primary" size="small" :loading="testingAi" @click="handleTestAi">
                <el-icon><Connection /></el-icon> 测试连接
              </el-button>
            </template>
            <el-form label-width="140px">
              <el-form-item label="AI服务提供者">
                <el-select v-model="configMap.ai_provider" style="width:280px">
                  <el-option value="deepseek" label="DeepSeek (云端)" />
                  <el-option value="ollama" label="Ollama (本地部署)" />
                </el-select>
                <div class="field-hint">切换后需要保存配置并重启后端服务生效</div>
              </el-form-item>

              <!-- DeepSeek 配置 -->
              <template v-if="configMap.ai_provider === 'deepseek'">
                <el-divider content-position="left">DeepSeek 云端配置</el-divider>
                <el-form-item label="API Key">
                  <el-input v-model="configMap.ai_deepseek_api_key" type="password" show-password placeholder="sk-..." style="width:400px" />
                  <div class="field-hint">在 platform.deepseek.com 获取 API Key，调用时作为 Bearer Token 鉴权</div>
                </el-form-item>
                <el-form-item label="API 地址">
                  <el-input v-model="configMap.ai_deepseek_base_url" placeholder="https://api.deepseek.com" style="width:400px" />
                  <div class="field-hint">DeepSeek 官方 API 地址，一般无需修改</div>
                </el-form-item>
                <el-form-item label="模型名称">
                  <el-input v-model="configMap.ai_deepseek_model" placeholder="deepseek-chat" style="width:280px" />
                  <div class="field-hint">可选 deepseek-chat（通用对话）或 deepseek-reasoner（深度推理）</div>
                </el-form-item>
              </template>

              <!-- Ollama 配置 -->
              <template v-if="configMap.ai_provider === 'ollama'">
                <el-divider content-position="left">Ollama 本地配置</el-divider>
                <el-form-item label="服务地址">
                  <el-input v-model="configMap.ai_ollama_base_url" placeholder="http://localhost:11434" style="width:400px" />
                  <div class="field-hint">Ollama 服务的 HTTP 地址，需确保后端服务器可访问该地址</div>
                </el-form-item>
                <el-form-item label="模型名称">
                  <el-input v-model="configMap.ai_ollama_model" placeholder="qwen2.5:7b" style="width:280px" />
                  <div class="field-hint">已部署在 Ollama 上的模型名称，可通过 ollama list 命令查看</div>
                </el-form-item>
              </template>
            </el-form>
          </el-card>
        </template>
      </div>
    </div>

    <!-- 邮件发送记录（始终展示） -->
    <el-card shadow="never" class="config-card" style="margin-top:12px">
      <template #header>
        <div class="card-header">
          <span><el-icon><Message /></el-icon> 邮件发送记录</span>
          <el-button size="small" @click="loadLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="logs" v-loading="logsLoading" stripe border size="small">
        <el-table-column prop="sendDate" label="日期" width="120" />
        <el-table-column prop="recipients" label="收件人" min-width="200" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="sendStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sendStatus === '成功' ? 'success' : 'danger'" size="small">{{ row.sendStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="150" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="sentAt" label="发送时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="logPageNum" v-model:page-size="logPageSize"
          :total="logTotal" :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next, jumper"
          background @size-change="loadLogs" @current-change="loadLogs" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi, digestApi } from '../../api'
import api from '../../api'

const loading = ref(false)
const saving = ref(false)
const logsLoading = ref(false)
const testingEmail = ref(false)
const testingAi = ref(false)
const activeGroup = ref('basic')
const configMap = ref<Record<string, string | number>>({})
const digestTime = ref('09:30')

const navGroups = [
  { key: 'basic', label: '基础配置', icon: 'Setting' },
  { key: 'score', label: '评分配置', icon: 'TrendCharts' },
  { key: 'ai', label: 'AI配置', icon: 'Cpu' },
]

const logs = ref<any[]>([])
const logPageNum = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

const weightSum = computed(() => {
  return Number(configMap.value.health_weight_time || 0)
    + Number(configMap.value.health_weight_risk || 0)
    + Number(configMap.value.health_weight_delivery || 0)
})
const weightSumValid = computed(() => weightSum.value === 100)

const thresholdValid = computed(() => {
  return Number(configMap.value.health_score_green_min || 0) > Number(configMap.value.health_score_yellow_min || 0)
})

onMounted(async () => { await loadConfig(); loadLogs() })

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await configApi.list()
    const map: Record<string, string> = {}
    res.data.forEach((c: any) => { map[c.configKey] = c.configValue })
    configMap.value = map
    if (map.daily_digest_send_time) digestTime.value = map.daily_digest_send_time
  } catch { /* ignore */ }
  loading.value = false
}

async function handleSave() {
  if (!weightSumValid.value) { ElMessage.error('健康评分三项权重之和必须为100%'); return }
  if (!thresholdValid.value) { ElMessage.error('绿色阈值需大于黄色阈值'); return }
  saving.value = true
  try {
    configMap.value.daily_digest_send_time = digestTime.value
    const configs = Object.entries(configMap.value).map(([key, value]) => ({ configKey: key, configValue: String(value) }))
    await configApi.update(configs)
    ElMessage.success('配置保存成功')
  } catch { /* ignore */ }
  saving.value = false
}

async function handleTestEmail() {
  const emails = (configMap.value.digest_recipient_emails as string || '').trim()
  if (!emails) { ElMessage.warning('请先输入收件人邮箱'); return }
  testingEmail.value = true
  try {
    await digestApi.testEmail(emails.split(',')[0].trim())
    ElMessage.success(`测试邮件已发送，请检查收件箱`)
  } catch { /* handled */ }
  testingEmail.value = false
}

async function handleTestAi() {
  testingAi.value = true
  try {
    const res: any = await api.post('/config/test-ai')
    ElMessage.success(res.message || 'AI连接成功')
  } catch { /* handled */ }
  testingAi.value = false
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res: any = await digestApi.logs(logPageNum.value, logPageSize.value)
    logs.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } catch { /* ignore */ }
  logsLoading.value = false
}
</script>

<style scoped>
/* 吸顶操作栏 */
.config-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
  margin: -20px -20px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.topbar-left .el-icon { color: #409eff; }

/* 左右布局 */
.config-layout {
  display: flex;
  gap: 0;
  min-height: 400px;
}

/* 左侧导航 */
.config-nav {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 8px 0;
  margin-right: 16px;
  position: sticky;
  top: 72px;
  align-self: flex-start;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
  margin: 2px 0;
}
.nav-item:hover {
  background: #f0f5ff;
  color: #409eff;
}
.nav-item.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
  border-left-color: #409eff;
}

/* 右侧内容区 */
.config-content {
  flex: 1;
  min-width: 0;
}

/* 左右布局 */
.config-layout {
  display: flex;
  gap: 0;
  min-height: 400px;
}

/* 内容区域限宽 */
.config-content {
  flex: 1;
  min-width: 0;
  max-width: 800px;
}

/* 配置卡片 */
.config-card {
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  border-radius: 8px;
}
.config-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

/* 字段说明 */
.field-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.5;
}

/* 阈值校验标红 */
:deep(.is-error .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f56c6c inset !important;
}

/* 分页 */
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 响应式：窄屏降级为顶部横向tab */
@media (max-width: 768px) {
  .config-layout { flex-direction: column; }
  .config-nav {
    width: 100%;
    display: flex;
    gap: 0;
    margin-right: 0;
    margin-bottom: 12px;
    position: static;
    padding: 0;
    overflow-x: auto;
  }
  .nav-item {
    flex: 1;
    justify-content: center;
    border-left: none;
    border-bottom: 3px solid transparent;
    padding: 10px 12px;
    font-size: 13px;
    white-space: nowrap;
  }
  .nav-item.active {
    border-left-color: transparent;
    border-bottom-color: #409eff;
  }
}
</style>
