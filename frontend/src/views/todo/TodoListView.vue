<template>
  <div class="page-container">
    <div class="page-header">
      <h2>项目待办</h2>
      <el-button type="primary" @click="openDialog(null)">新增待办</el-button>
    </div>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="项目">
          <el-select v-model="searchForm.projectId" placeholder="全部项目" clearable filterable style="width:200px" @change="handleSearch">
            <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="`${p.projectCode} - ${p.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:110px" @change="handleSearch">
            <el-option value="待处理" label="待处理" /><el-option value="进行中" label="进行中" />
            <el-option value="已完成" label="已完成" /><el-option value="已逾期" label="已逾期" />
            <el-option value="已取消" label="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="全部" clearable style="width:100px" @change="handleSearch">
            <el-option value="高" label="高" /><el-option value="中" label="中" /><el-option value="低" label="低" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="编号/事项/备注" clearable style="width:180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch"><el-icon><Search /></el-icon> 查询</el-button>
          <el-button @click="handleReset"><el-icon><Refresh /></el-icon> 重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 待办列表 -->
    <el-card shadow="never" style="margin-top:12px">

      <!-- 表格 -->
      <el-table :data="todos" v-loading="loading" stripe border>
        <el-table-column type="index" label="序号" width="55" align="center">
          <template #default="{ $index }">{{ (pageNum - 1) * pageSize + $index + 1 }}</template>
        </el-table-column>
        <el-table-column prop="todoCode" label="待办编号" width="180" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="projectCode" label="项目编号" width="130" />
        <el-table-column prop="title" label="待办事项" min-width="200" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }" />
        <el-table-column prop="stageName" label="所属阶段" width="100" />
        <el-table-column prop="priority" label="优先级" width="75" align="center">
          <template #default="{ row }">
            <el-tag :type="row.priority==='高'?'danger':row.priority==='中'?'warning':'info'" size="small">{{ row.priority }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="urgency" label="紧急程度" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="row.urgency==='特急'?'danger':row.urgency==='紧急'?'warning':''" size="small">{{ row.urgency }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="85" />
        <el-table-column prop="planEnd" label="计划完成" width="105" />
        <el-table-column label="进度" width="100">
          <template #default="{ row }">
            <el-progress :percentage="row.progress||0" :stroke-width="14" :text-inside="true" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此待办？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total"
          :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next, jumper" background
          @size-change="loadData" @current-change="loadData" />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑待办' : '新增待办'" width="700px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属项目" required>
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width:100%" :disabled="isEdit">
            <el-option v-for="p in availableProjects" :key="p.id" :value="p.id" :label="`${p.projectCode} - ${p.name}`">
              <span>{{ p.projectCode }} - {{ p.name }}</span>
              <el-tag v-if="p.status==='已完成'" size="small" type="info" style="margin-left:8px">已结束</el-tag>
            </el-option>
          </el-select>
          <div v-if="form.projectId && selectedProjectStatus === '已完成'" class="form-tip warning">
            <el-icon><Warning /></el-icon> 该项目已结束，仅可选择非运维阶段的待办
          </div>
        </el-form-item>
        <el-form-item label="待办事项" required>
          <el-input v-model="form.title" placeholder="请输入待办事项" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属阶段">
              <el-select v-model="form.stageId" placeholder="选择阶段" clearable style="width:100%">
                <el-option v-for="s in availableStages" :key="s.id" :value="s.id" :label="s.stageName" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="来源">
              <el-select v-model="form.source" placeholder="选择来源" clearable filterable allow-create style="width:100%">
                <el-option value="会议纪要" /><el-option value="客户需求" /><el-option value="内部评估" />
                <el-option value="风险跟踪" /><el-option value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width:100%">
                <el-option value="高" /><el-option value="中" /><el-option value="低" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="紧急程度">
              <el-select v-model="form.urgency" style="width:100%">
                <el-option value="特急" /><el-option value="紧急" /><el-option value="普通" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="负责人">
              <el-select
                v-model="selectedOwner"
                placeholder="选择或输入负责人"
                filterable
                allow-create
                default-first-option
                clearable
                style="width:100%"
              >
                <el-option v-for="u in users" :key="u.id" :value="u.id" :label="u.realName" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始">
              <el-date-picker v-model="form.planStart" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划完成">
              <el-date-picker v-model="form.planEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="完成百分比">
              <el-slider v-model="form.progress" :min="0" :max="100" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width:100%">
                <el-option value="待处理" /><el-option value="进行中" />
                <el-option value="已完成" /><el-option value="已取消" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="阻塞问题">
          <el-input v-model="form.blockIssue" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="风险说明">
          <el-input v-model="form.riskDesc" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="输出物">
          <el-input v-model="form.outputDesc" placeholder="预期交付物描述" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Warning } from '@element-plus/icons-vue'
import { todoApi, projectApi, stageApi, authApi } from '../../api'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const todos = ref<any[]>([])
const projects = ref<any[]>([])
const allStages = ref<any[]>([])
const users = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editingId = ref<number | null>(null)
const isEdit = computed(() => editingId.value !== null)

const searchForm = reactive({ projectId: undefined as number | undefined, status: '', priority: '', keyword: '' })

const form = reactive<any>({
  projectId: null, stageId: null, title: '', source: '', priority: '中', urgency: '普通',
  ownerId: null, ownerName: '', planStart: '', planEnd: '', status: '待处理', progress: 0,
  blockIssue: '', riskDesc: '', outputDesc: '', remark: '',
})

// 负责人选择器（兼容数字ID和手动输入的字符串）
const selectedOwner = ref<number | string | null>(null)

// 可选项目：排除已结束(非运维阶段的项目不可选，但如果选了已结束项目，阶段下拉排除运维)
const availableProjects = computed(() => projects.value)

// 当前选中项目的状态
const selectedProjectStatus = computed(() => {
  const p = projects.value.find((x: any) => x.id === form.projectId)
  return p?.status || ''
})

// 可选阶段：返回该项目的所有阶段
const availableStages = computed(() => {
  return allStages.value.filter((s: any) => s.projectId === form.projectId)
})

function statusType(s: string) {
  return { '待处理': 'info', '进行中': 'primary', '已完成': 'success', '已取消': 'info', '已逾期': 'danger' }[s] ?? 'info'
}

onMounted(async () => {
  const [projRes, userRes]: any[] = await Promise.all([projectApi.list(), authApi.getUsers()])
  projects.value = projRes.data || []
  users.value = userRes.data || []
  // 加载所有项目的阶段
  for (const p of projects.value) {
    try {
      const res: any = await stageApi.listByProject(p.id)
      const stages = (res.data || []).map((s: any) => ({ ...s, projectId: p.id }))
      allStages.value.push(...stages)
    } catch { /* ignore */ }
  }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchForm.projectId) params.projectId = searchForm.projectId
    if (searchForm.status) params.status = searchForm.status
    if (searchForm.priority) params.priority = searchForm.priority
    if (searchForm.keyword) params.keyword = searchForm.keyword
    const res: any = await todoApi.page(params)
    todos.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { /* handled */ }
  loading.value = false
}

function handleSearch() { pageNum.value = 1; loadData() }
function handleReset() {
  Object.assign(searchForm, { projectId: undefined, status: '', priority: '', keyword: '' })
  pageNum.value = 1; loadData()
}

function openDialog(row: any) {
  if (row) {
    editingId.value = row.id
    // 设置 selectedOwner：优先用 ownerId，没有则用 ownerName
    selectedOwner.value = row.ownerId || row.ownerName || null
    Object.assign(form, {
      projectId: row.projectId, stageId: row.stageId, title: row.title, source: row.source || '',
      priority: row.priority || '中', urgency: row.urgency || '普通', ownerId: row.ownerId, ownerName: row.ownerName || '',
      planStart: row.planStart, planEnd: row.planEnd, status: row.status || '待处理',
      progress: row.progress || 0, blockIssue: row.blockIssue || '', riskDesc: row.riskDesc || '',
      outputDesc: row.outputDesc || '', remark: row.remark || '',
    })
  } else {
    editingId.value = null
    selectedOwner.value = null
    Object.assign(form, {
      projectId: null, stageId: null, title: '', source: '', priority: '中', urgency: '普通',
      ownerId: null, ownerName: '', planStart: '', planEnd: '', status: '待处理', progress: 0,
      blockIssue: '', riskDesc: '', outputDesc: '', remark: '',
    })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.projectId) { ElMessage.warning('请选择项目'); return }
  if (!form.title) { ElMessage.warning('请输入待办事项'); return }

  // 解析负责人：数字=用户ID，字符串=手动输入的姓名
  if (selectedOwner.value !== null && selectedOwner.value !== undefined) {
    if (typeof selectedOwner.value === 'number') {
      form.ownerId = selectedOwner.value
      form.ownerName = null
    } else {
      form.ownerId = null
      form.ownerName = String(selectedOwner.value)
    }
  } else {
    form.ownerId = null
    form.ownerName = null
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await todoApi.update(editingId.value!, { ...form })
    } else {
      await todoApi.createGlobal({ ...form })
    }
    dialogVisible.value = false
    ElMessage.success(isEdit.value ? '待办已更新' : '待办已创建')
    loadData()
  } catch { /* handled */ }
  submitting.value = false
}

async function handleDelete(id: number) {
  try { await todoApi.delete(id); ElMessage.success('已删除'); loadData() } catch { /* handled */ }
}
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; }
.search-card :deep(.el-form-item) { margin-bottom: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.form-tip { font-size: 12px; margin-top: 4px; display: flex; align-items: center; gap: 4px; }
.form-tip.warning { color: #e6a23c; }
</style>
