<template>
  <div class="page-container">
    <!-- 任务列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span><el-icon><Timer /></el-icon> 定时任务管理</span>
          <el-button @click="loadTasks"><el-icon><Refresh /></el-icon> 刷新</el-button>
        </div>
      </template>

      <el-table :data="tasks" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="taskName" label="任务名称" width="160" />
        <el-table-column prop="taskKey" label="任务标识" width="200">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.taskKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="执行频率" width="280">
          <template #default="{ row }">
            <div v-if="row._editingCron" class="cron-edit">
              <el-popover trigger="click" placement="bottom" :width="320">
                <template #reference>
                  <el-input v-model="row._cronValue" placeholder="如: 0 0 22 * * ?" size="small" style="width:180px">
                    <template #append>
                      <el-icon><QuestionFilled /></el-icon>
                    </template>
                  </el-input>
                </template>
                <div class="cron-help">
                  <div class="cron-help-title">常用 Cron 表达式</div>
                  <div v-for="item in cronExamples" :key="item.expr" class="cron-help-item" @click="row._cronValue = item.expr">
                    <code>{{ item.expr }}</code>
                    <span>{{ item.label }}</span>
                  </div>
                  <div class="cron-help-title" style="margin-top:8px">格式说明</div>
                  <div class="cron-help-format">秒 分 时 日 月 周</div>
                  <div class="cron-help-format">例: <code>0 0 22 * * ?</code> = 每天22:00</div>
                </div>
              </el-popover>
              <el-button type="primary" link size="small" @click="saveCron(row)" style="margin-left:4px">
                <el-icon><Check /></el-icon>
              </el-button>
              <el-button link size="small" @click="row._editingCron = false">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
            <div v-else class="cron-display" @click="startEditCron(row)">
              <code class="cron-code">{{ row.cronExpr }}</code>
              <el-icon class="cron-edit-icon"><Edit /></el-icon>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="任务说明" min-width="250" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="handleToggle(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :loading="row._running" @click="handleRun(row)">
              <el-icon><CaretRight /></el-icon> 手动执行
            </el-button>
            <el-button type="info" size="small" link @click="showLogs(row)">
              执行记录
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行记录 -->
    <el-card shadow="never" style="margin-top:12px">
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><Notebook /></el-icon> 执行记录
            <span v-if="currentTask" class="task-tag">
              <el-tag size="small">{{ currentTask.taskName }}</el-tag>
            </span>
          </span>
          <el-button size="small" @click="loadAllLogs">全部记录</el-button>
        </div>
      </template>

      <el-table :data="logs" v-loading="logsLoading" stripe border size="small">
        <el-table-column prop="taskName" label="任务名称" width="140" />
        <el-table-column prop="triggerType" label="触发方式" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.triggerType === '自动' ? 'primary' : 'success'" size="small">{{ row.triggerType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resultMsg" label="执行结果" min-width="250" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="executionTime" label="耗时" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: row.executionTime > 5000 ? '#f56c6c' : '' }">{{ row.executionTime }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="executedAt" label="执行时间" width="170" />
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="logPageNum" v-model:page-size="logPageSize" :total="logTotal"
          :page-sizes="[20,50,100]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadLogs" @current-change="loadLogs" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Timer, Refresh, CaretRight, Notebook, Edit, Check, Close, QuestionFilled } from '@element-plus/icons-vue'
import { scheduledTaskApi } from '../../api'
import api from '../../api'

const loading = ref(false)
const logsLoading = ref(false)
const tasks = ref<any[]>([])
const logs = ref<any[]>([])
const currentTask = ref<any>(null)
const logPageNum = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

const cronExamples = [
  { expr: '0 0 * * * ?', label: '每小时整点' },
  { expr: '0 */30 * * * ?', label: '每30分钟' },
  { expr: '0 0 9 * * ?', label: '每天 09:00' },
  { expr: '0 0 22 * * ?', label: '每天 22:00' },
  { expr: '0 30 8 * * 1-5', label: '工作日 08:30' },
  { expr: '0 0 9 * * 1', label: '每周一 09:00' },
  { expr: '0 0 0 1 * ?', label: '每月1号 00:00' },
]

onMounted(() => { loadTasks(); loadAllLogs() })

async function loadTasks() {
  loading.value = true
  try {
    const res: any = await scheduledTaskApi.list()
    tasks.value = (res.data || []).map((t: any) => ({ ...t, _running: false, _editingCron: false, _cronValue: t.cronExpr }))
  } catch { /* handled */ }
  loading.value = false
}

function startEditCron(row: any) {
  row._cronValue = row.cronExpr
  row._editingCron = true
}

async function saveCron(row: any) {
  if (!row._cronValue || !row._cronValue.trim()) {
    ElMessage.warning('请选择或输入Cron表达式')
    return
  }
  try {
    await api.put(`/scheduled-tasks/${row.id}/cron`, { cronExpr: row._cronValue })
    row.cronExpr = row._cronValue
    row._editingCron = false
    ElMessage.success('执行频率已更新')
  } catch { /* handled */ }
}

async function handleToggle(row: any) {
  try {
    await scheduledTaskApi.toggle(row.id)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    loadTasks()
  } catch { /* handled */ }
}

// 任务影响说明映射
const taskImpactMap: Record<string, string> = {
  refreshDelayedStages: '将扫描全部项目阶段，自动标记计划已过期但未完成的阶段为"已延期"',
  refreshStaleRisks: '将扫描全部未关闭风险，自动标记超过阈值天数未更新的风险为"停滞"',
  refreshOverdueTodos: '将扫描全部待处理待办，逾期待办将自动创建对应的风险记录',
  nightlyAiRiskScan: '将扫描全部进行中项目的阶段备注，调用AI识别潜在风险（耗时取决于项目数量）',
  dailyTodoAndRiskDigest: '将汇总全部项目的待办和风险，生成日报邮件发送给配置的收件人',
  dailyDigest: '将汇总Dashboard数据，调用AI生成自然语言摘要，发送邮件给配置的收件人',
}

async function handleRun(row: any) {
  const impact = taskImpactMap[row.taskKey] || row.description || '执行指定任务'
  try {
    await ElMessageBox.confirm(
      `<div style="line-height:1.8">
        <p><b>任务名称：</b>${row.taskName}</p>
        <p><b>影响范围：</b>${impact}</p>
        <p style="color:#909399;font-size:12px;margin-top:8px">Cron频率：${row.cronExpr}</p>
      </div>`,
      '确认手动执行',
      {
        confirmButtonText: '立即执行',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: true,
        customStyle: { maxWidth: '480px' },
      }
    )
    row._running = true
    const res: any = await scheduledTaskApi.run(row.id)
    row._running = false
    ElMessage.success({ message: res.message || '执行完成', duration: 5000 })
    loadLogs()
  } catch {
    row._running = false
  }
}

function showLogs(row: any) {
  currentTask.value = row
  logPageNum.value = 1
  loadLogs()
}

function loadAllLogs() {
  currentTask.value = null
  logPageNum.value = 1
  loadLogs()
}

async function loadLogs() {
  logsLoading.value = true
  try {
    const params: any = { pageNum: logPageNum.value, pageSize: logPageSize.value }
    if (currentTask.value) params.taskId = currentTask.value.id
    const res: any = await scheduledTaskApi.logs(params)
    logs.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } catch { /* handled */ }
  logsLoading.value = false
}
</script>

<style scoped>
.cron-code {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
.cron-display {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 4px;
  border-radius: 4px;
  transition: background 0.2s;
}
.cron-display:hover { background: #f0f5ff; }
.cron-edit-icon { color: #c0c4cc; font-size: 14px; opacity: 0; transition: opacity 0.2s; }
.cron-display:hover .cron-edit-icon { opacity: 1; }
.cron-edit { display: flex; align-items: center; }

/* Cron帮助弹窗 */
.cron-help-title {
  font-size: 12px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.cron-help-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 6px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background 0.15s;
}
.cron-help-item:hover { background: #f0f5ff; }
.cron-help-item code {
  background: #f5f7fa;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 11px;
}
.cron-help-item span { color: #909399; }
.cron-help-format {
  font-size: 12px;
  color: #606266;
  line-height: 1.8;
}
.cron-help-format code {
  background: #f5f7fa;
  padding: 1px 5px;
  border-radius: 3px;
}

.task-tag { margin-left: 10px; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
