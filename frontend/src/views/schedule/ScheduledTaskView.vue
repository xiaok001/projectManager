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
        <el-table-column prop="cronExpr" label="Cron表达式" width="150">
          <template #default="{ row }">
            <code class="cron-code">{{ row.cronExpr }}</code>
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
import { Timer, Refresh, CaretRight, Notebook } from '@element-plus/icons-vue'
import { scheduledTaskApi } from '../../api'

const loading = ref(false)
const logsLoading = ref(false)
const tasks = ref<any[]>([])
const logs = ref<any[]>([])
const currentTask = ref<any>(null)
const logPageNum = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)

onMounted(() => { loadTasks(); loadAllLogs() })

async function loadTasks() {
  loading.value = true
  try {
    const res: any = await scheduledTaskApi.list()
    tasks.value = (res.data || []).map((t: any) => ({ ...t, _running: false }))
  } catch { /* handled */ }
  loading.value = false
}

async function handleToggle(row: any) {
  try {
    await scheduledTaskApi.toggle(row.id)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    loadTasks()
  } catch { /* handled */ }
}

async function handleRun(row: any) {
  try {
    await ElMessageBox.confirm(`确认立即执行「${row.taskName}」？`, '手动执行', {
      confirmButtonText: '立即执行', cancelButtonText: '取消', type: 'info',
    })
    row._running = true
    const res: any = await scheduledTaskApi.run(row.id)
    ElMessage.success(res.message || '执行完成')
    row._running = false
    loadLogs()
  } catch { row._running = false }
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
.task-tag { margin-left: 10px; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
