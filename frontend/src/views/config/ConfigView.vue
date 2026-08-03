<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Setting /></el-icon> 系统配置</span>
          <el-button type="primary" @click="handleSave" :loading="saving">保存配置</el-button>
        </div>
      </template>

      <el-form v-loading="loading" label-width="200px" style="max-width: 700px">
        <el-divider content-position="left">风险停滞配置</el-divider>
        <el-form-item label="停滞判定天数">
          <el-input-number v-model="configMap.stale_threshold_days" :min="1" :max="30" />
        </el-form-item>

        <el-divider content-position="left">邮件摘要配置</el-divider>
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

        <el-divider content-position="left">健康评分权重（之和必须100%）</el-divider>
        <el-form-item label="时间维度权重(%)">
          <el-input-number v-model="configMap.health_weight_time" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="风险维度权重(%)">
          <el-input-number v-model="configMap.health_weight_risk" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="交付维度权重(%)">
          <el-input-number v-model="configMap.health_weight_delivery" :min="0" :max="100" />
        </el-form-item>
        <el-form-item>
          <el-tag :type="weightSumValid ? 'success' : 'danger'">
            权重总和: {{ weightSum }}%
          </el-tag>
        </el-form-item>

        <el-divider content-position="left">健康度颜色阈值</el-divider>
        <el-form-item label="绿色最低分">
          <el-input-number v-model="configMap.health_score_green_min" :min="0" :max="100" />
        </el-form-item>
        <el-form-item label="黄色最低分">
          <el-input-number v-model="configMap.health_score_yellow_min" :min="0" :max="100" />
        </el-form-item>

        <el-divider content-position="left">风险扣分系数</el-divider>
        <el-form-item label="每天延期扣分">
          <el-input-number v-model="configMap.time_delay_penalty_per_day" :min="0" :max="20" />
        </el-form-item>
        <el-form-item label="高危风险扣分">
          <el-input-number v-model="configMap.risk_penalty_high" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="中危风险扣分">
          <el-input-number v-model="configMap.risk_penalty_medium" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="低危风险扣分">
          <el-input-number v-model="configMap.risk_penalty_low" :min="0" :max="50" />
        </el-form-item>
        <el-form-item label="停滞风险扣分">
          <el-input-number v-model="configMap.risk_penalty_stale" :min="0" :max="50" />
        </el-form-item>

        <el-divider content-position="left">AI配置</el-divider>
        <el-form-item label="AI服务提供者">
          <el-select v-model="configMap.ai_provider">
            <el-option value="deepseek" label="DeepSeek (云端)" />
            <el-option value="ollama" label="Ollama (本地)" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card style="margin-top:20px">
      <template #header>
        <div class="card-header">
          <span><el-icon><Message /></el-icon> 邮件发送记录</span>
          <el-button @click="loadLogs">刷新</el-button>
        </div>
      </template>
      <el-table :data="logs" v-loading="logsLoading" stripe>
        <el-table-column prop="sendDate" label="日期" width="120" />
        <el-table-column prop="recipients" label="收件人" min-width="200" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="sendStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.sendStatus === '成功' ? 'success' : 'danger'">{{ row.sendStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="150" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="sentAt" label="发送时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { configApi, digestApi } from '../../api'

const loading = ref(false)
const saving = ref(false)
const logsLoading = ref(false)
const testingEmail = ref(false)
const configMap = ref<Record<string, string | number>>({})
const digestTime = ref('09:30')
const logs = ref<any[]>([])

const weightSum = computed(() => {
  return Number(configMap.value.health_weight_time || 0)
    + Number(configMap.value.health_weight_risk || 0)
    + Number(configMap.value.health_weight_delivery || 0)
})
const weightSumValid = computed(() => weightSum.value === 100)

onMounted(async () => {
  await loadConfig()
  loadLogs()
})

async function loadConfig() {
  loading.value = true
  try {
    const res: any = await configApi.list()
    const map: Record<string, string> = {}
    res.data.forEach((c: any) => {
      map[c.configKey] = c.configValue
    })
    configMap.value = map
    if (map.daily_digest_send_time) {
      digestTime.value = map.daily_digest_send_time
    }
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function handleSave() {
  if (!weightSumValid.value) {
    ElMessage.error('健康评分三项权重之和必须为100%')
    return
  }

  saving.value = true
  try {
    // 更新发送时间
    configMap.value.daily_digest_send_time = digestTime.value
    const configs = Object.entries(configMap.value).map(([key, value]) => ({
      configKey: key,
      configValue: String(value),
    }))
    await configApi.update(configs)
    ElMessage.success('配置保存成功')
  } catch (e) { /* ignore */ }
  saving.value = false
}

async function handleTestEmail() {
  const emails = (configMap.value.digest_recipient_emails as string || '').trim()
  if (!emails) {
    ElMessage.warning('请先输入收件人邮箱')
    return
  }
  // 取第一个邮箱测试
  const testAddr = emails.split(',')[0].trim()
  testingEmail.value = true
  try {
    await digestApi.testEmail(testAddr)
    ElMessage.success(`测试邮件已发送至 ${testAddr}，请检查收件箱`)
  } catch (e) { /* handled by interceptor */ }
  testingEmail.value = false
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const res: any = await digestApi.logs(1, 20)
    logs.value = res.data.records || []
  } catch (e) { /* ignore */ }
  logsLoading.value = false
}
</script>
