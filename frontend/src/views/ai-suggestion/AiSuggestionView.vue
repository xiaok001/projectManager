<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>
            <el-icon><Cpu /></el-icon> AI风险建议
          </span>
          <el-radio-group v-model="statusFilter" @change="loadData">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="待确认">待确认</el-radio-button>
            <el-radio-button value="已采纳">已采纳</el-radio-button>
            <el-radio-button value="已忽略">已忽略</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="suggestions" v-loading="loading" stripe>
        <el-table-column prop="projectId" label="项目ID" width="80" />
        <el-table-column prop="sourceText" label="原始备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="suggestedRiskDesc" label="AI建议风险描述" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待确认'">
              <el-button type="primary" size="small" @click="handleAccept(row.id)">采纳登记</el-button>
              <el-button type="info" size="small" @click="handleIgnore(row.id)">忽略</el-button>
            </template>
            <span v-else style="color:#909399;font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiSuggestionApi } from '../../api'

const loading = ref(false)
const suggestions = ref<any[]>([])
const statusFilter = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res: any = await aiSuggestionApi.list(statusFilter.value || undefined)
    suggestions.value = res.data
  } catch (e) { /* ignore */ }
  loading.value = false
}

async function handleAccept(id: number) {
  await ElMessageBox.confirm('确认采纳此AI建议并登记为正式风险？', '确认')
  try {
    await aiSuggestionApi.accept(id)
    ElMessage.success('已采纳并创建正式风险记录')
    loadData()
  } catch (e) { /* ignore */ }
}

async function handleIgnore(id: number) {
  await ElMessageBox.confirm('确认忽略此AI建议？', '确认')
  try {
    await aiSuggestionApi.ignore(id)
    ElMessage.success('已忽略')
    loadData()
  } catch (e) { /* ignore */ }
}

function statusTagType(status: string) {
  if (status === '待确认') return 'warning'
  if (status === '已采纳') return 'success'
  return 'info'
}
</script>
