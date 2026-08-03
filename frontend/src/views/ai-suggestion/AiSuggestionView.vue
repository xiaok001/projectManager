<template>
  <div class="page-container">
    <div class="page-header">
      <h2>
        AI风险建议
        <span class="source-tip">
          <el-icon><InfoFilled /></el-icon>
          数据来源：项目经理更新阶段备注时，系统自动调用AI分析文本，识别潜在风险并生成建议，仅作参考需人工确认
        </span>
      </h2>
      <el-button type="success" :loading="scanning" @click="handleScan">
        <el-icon><Cpu /></el-icon> 手动扫描项目
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="项目编号">
          <el-input v-model="searchForm.projectCode" placeholder="请输入项目编号" clearable style="width:180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="项目名称">
          <el-input v-model="searchForm.projectName" placeholder="请输入项目名称" clearable style="width:180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:260px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px" @change="handleSearch">
            <el-option value="待确认" label="待确认" />
            <el-option value="已采纳" label="已采纳" />
            <el-option value="已忽略" label="已忽略" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never" style="margin-top:12px">
      <el-table :data="suggestions" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="60" align="center">
          <template #default="{ $index }">{{ (pageNum - 1) * pageSize + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="projectCode" label="项目编号" width="140" />
        <el-table-column prop="projectName" label="项目名称" width="160" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="sourceText" label="原始备注" min-width="220" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="suggestedRiskDesc" label="AI建议风险描述" min-width="260" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === '待确认'">
              <el-button type="primary" size="small" link @click="handleAccept(row.id)">采纳登记</el-button>
              <el-button type="info" size="small" link @click="handleIgnore(row.id)">忽略</el-button>
            </template>
            <span v-else style="color:#909399;font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, InfoFilled } from '@element-plus/icons-vue'
import { aiSuggestionApi } from '../../api'
import api from '../../api'

const loading = ref(false)
const scanning = ref(false)
const suggestions = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const searchForm = reactive({
  status: '',
  projectCode: '',
  projectName: '',
  dateRange: null as string[] | null,
})

onMounted(() => { loadData() })

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.projectCode) params.projectCode = searchForm.projectCode
    if (searchForm.projectName) params.projectName = searchForm.projectName
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res: any = await aiSuggestionApi.page(params)
    suggestions.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* handled */ }
  loading.value = false
}

function handleSearch() {
  pageNum.value = 1
  loadData()
}

function handleReset() {
  searchForm.status = ''
  searchForm.projectCode = ''
  searchForm.projectName = ''
  searchForm.dateRange = null
  pageNum.value = 1
  loadData()
}

async function handleScan() {
  try {
    await ElMessageBox.confirm(
      `<div style="line-height:1.8">
        <p><b>AI风险扫描原理：</b></p>
        <ol style="padding-left:20px;margin:8px 0">
          <li>扫描所有「进行中」项目的阶段备注（由项目经理填写的进展说明）</li>
          <li>将每条备注文本发送给AI大模型进行语义分析</li>
          <li>AI识别文本中潜在的风险信号（如进度延迟、资源不足、需求变更等）</li>
          <li>识别到的风险以「待确认」状态写入建议列表，需人工审核后才会转为正式风险</li>
          <li>已扫描过的备注不会重复分析，避免产生重复建议</li>
        </ol>
        <p style="color:#909399;font-size:13px;margin-top:8px">
          ⏱ 扫描耗时取决于项目数量和AI服务响应速度，通常需要1-3分钟
        </p>
      </div>`,
      '手动扫描项目',
      {
        confirmButtonText: '开始扫描',
        cancelButtonText: '取消',
        dangerouslyUseHTMLString: true,
        type: 'info',
        customStyle: { maxWidth: '520px' },
      }
    )
    scanning.value = true
    try {
      const res: any = await api.post('/ai-suggestions/scan')
      ElMessage.success(res.message || '扫描完成')
      loadData()
    } catch { /* handled */ }
    scanning.value = false
  } catch {
    // 用户点了取消
  }
}

async function handleAccept(id: number) {
  await ElMessageBox.confirm('确认采纳此AI建议并登记为正式风险？', '确认')
  try {
    await aiSuggestionApi.accept(id)
    ElMessage.success('已采纳并创建正式风险记录')
    loadData()
  } catch { /* handled */ }
}

async function handleIgnore(id: number) {
  await ElMessageBox.confirm('确认忽略此AI建议？', '确认')
  try {
    await aiSuggestionApi.ignore(id)
    ElMessage.success('已忽略')
    loadData()
  } catch { /* handled */ }
}

function statusTagType(status: string) {
  if (status === '待确认') return 'warning'
  if (status === '已采纳') return 'success'
  return 'info'
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}
.source-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}
.source-tip .el-icon { color: #c0c4cc; }
.search-card :deep(.el-form-item) { margin-bottom: 0; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
