<template>
  <div v-loading="loading" class="project-detail-page">
    <!-- 基本信息卡片 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-header">
          <span>项目信息</span>
          <el-button link type="primary" @click="router.back()">返回</el-button>
        </div>
      </template>
      <el-descriptions :column="4" border>
        <el-descriptions-item label="项目编号">{{ project.code }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ project.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ project.type }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <el-tag :type="levelTagType(project.level)">
            {{ levelLabel(project.level) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          {{ project.amount != null ? `¥${project.amount.toLocaleString()}` : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ project.pmName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(project.status)">{{ project.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户满意度">
          <div class="satisfaction-row">
            <el-input-number
              v-model="satisfactionScore"
              :min="1"
              :max="10"
              :step="1"
              size="small"
              style="width: 120px"
            />
            <el-button
              type="primary"
              size="small"
              :loading="savingSatisfaction"
              style="margin-left: 8px"
              @click="saveSatisfaction"
            >
              保存
            </el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Tab 区域 -->
    <el-card shadow="never" style="margin-top: 16px">
      <el-tabs v-model="activeTab">
        <!-- 阶段管理 Tab -->
        <el-tab-pane label="阶段管理" name="stages">
          <el-table :data="stages" border stripe style="width: 100%">
            <el-table-column prop="name" label="阶段名" min-width="120" />
            <el-table-column prop="planStart" label="计划开始" min-width="110" />
            <el-table-column prop="planEnd" label="计划结束" min-width="110" />
            <el-table-column prop="actualStart" label="实际开始" min-width="110" />
            <el-table-column prop="actualEnd" label="实际结束" min-width="110" />
            <el-table-column prop="status" label="状态" min-width="90" />
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openStageDialog(row)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 风险列表 Tab -->
        <el-tab-pane label="风险列表" name="risks">
          <div style="margin-bottom: 12px; text-align: right">
            <el-button type="primary" @click="openRiskDialog(null)">
              登记新风险
            </el-button>
          </div>
          <el-table :data="risks" border stripe style="width: 100%">
            <el-table-column prop="riskCode" label="风险编号" min-width="110" />
            <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" min-width="90" />
            <el-table-column prop="severity" label="严重程度" min-width="90" />
            <el-table-column prop="status" label="状态" min-width="80" />
            <el-table-column prop="owner" label="责任人" min-width="90" />
            <el-table-column prop="mitigation" label="处理措施" min-width="160" show-overflow-tooltip />
            <el-table-column prop="isStale" label="是否停滞" min-width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isStale ? 'danger' : 'success'" disable-transitions>
                  {{ row.isStale ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="最近更新" min-width="110" />
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openRiskDialog(row)">
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 变更记录 Tab -->
        <el-tab-pane label="变更记录" name="changes">
          <div style="margin-bottom: 16px; text-align: right">
            <el-button type="primary" @click="openChangeDialog">
              登记变更
            </el-button>
          </div>
          <el-timeline v-if="changeLogs.length > 0">
            <el-timeline-item
              v-for="log in changeLogs"
              :key="log.id"
              :timestamp="log.createdAt"
              placement="top"
            >
              <el-card shadow="never">
                <h4 style="margin: 0 0 4px">{{ log.title }}</h4>
                <p style="margin: 0; color: #606266">{{ log.content }}</p>
                <p style="margin: 4px 0 0; color: #909399; font-size: 12px">
                  操作人：{{ log.operator }}
                </p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无变更记录" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 阶段编辑弹窗 -->
    <el-dialog v-model="stageDialogVisible" title="编辑阶段" width="560px" destroy-on-close>
      <el-form :model="stageForm" label-width="90px">
        <el-form-item label="阶段名">
          <el-input v-model="stageForm.name" disabled />
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker
            v-model="stageForm.planStart"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker
            v-model="stageForm.planEnd"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实际开始">
          <el-date-picker
            v-model="stageForm.actualStart"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="实际结束">
          <el-date-picker
            v-model="stageForm.actualEnd"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="stageForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="未开始" value="未开始" />
            <el-option label="进行中" value="进行中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已延期" value="已延期" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stageForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStage" @click="saveStage">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 风险编辑弹窗 -->
    <el-dialog
      v-model="riskDialogVisible"
      :title="riskForm.id ? '编辑风险' : '登记新风险'"
      width="600px"
      destroy-on-close
    >
      <el-form :model="riskForm" label-width="90px">
        <el-form-item label="描述">
          <el-input v-model="riskForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="riskForm.type" placeholder="如：技术风险、进度风险" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="riskForm.severity" placeholder="选择严重程度" style="width: 100%">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="riskForm.status" placeholder="选择状态" style="width: 100%">
            <el-option label="开放" value="开放" />
            <el-option label="处理中" value="处理中" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人">
          <el-input v-model="riskForm.owner" />
        </el-form-item>
        <el-form-item label="处理措施">
          <el-input v-model="riskForm.mitigation" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="riskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRisk" @click="saveRisk">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 变更登记弹窗 -->
    <el-dialog v-model="changeDialogVisible" title="登记变更" width="500px" destroy-on-close>
      <el-form :model="changeForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="changeForm.title" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="changeForm.content" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChange" @click="saveChange">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  projectApi,
  stageApi,
  riskApi,
  changeLogApi,
} from '../../api'

// --- Types ---
interface Project {
  id: number
  code: string
  name: string
  type: string
  level: number
  amount: number
  pmName: string
  status: string
  satisfaction: number | null
}

interface Stage {
  id: number
  name: string
  planStart: string
  planEnd: string
  actualStart: string
  actualEnd: string
  status: string
  remark: string
}

interface Risk {
  id: number
  riskCode: string
  description: string
  type: string
  severity: string
  status: string
  owner: string
  mitigation: string
  isStale: boolean
  updatedAt: string
}

interface ChangeLog {
  id: number
  title: string
  content: string
  operator: string
  createdAt: string
}

// --- Route ---
const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)

// --- Loading state ---
const loading = ref(false)

// --- Project basic info ---
const project = reactive<Project>({
  id: projectId,
  code: '',
  name: '',
  type: '',
  level: 0,
  amount: 0,
  pmName: '',
  status: '',
  satisfaction: null,
})

const satisfactionScore = ref<number>(5)
const savingSatisfaction = ref(false)

// --- Stages ---
const stages = ref<Stage[]>([])
const stageDialogVisible = ref(false)
const savingStage = ref(false)
const stageForm = reactive({
  id: 0,
  name: '',
  planStart: '',
  planEnd: '',
  actualStart: '',
  actualEnd: '',
  status: '',
  remark: '',
})

// --- Risks ---
const risks = ref<Risk[]>([])
const riskDialogVisible = ref(false)
const savingRisk = ref(false)
const riskForm = reactive({
  id: 0,
  description: '',
  type: '',
  severity: '',
  status: '',
  owner: '',
  mitigation: '',
})

// --- Change logs ---
const changeLogs = ref<ChangeLog[]>([])
const changeDialogVisible = ref(false)
const savingChange = ref(false)
const changeForm = reactive({
  title: '',
  content: '',
})

// --- Tab ---
const activeTab = ref((route.query.tab as string) || 'stages')

// --- Helpers ---
const levelTagType = (level: number): 'danger' | 'warning' | 'info' => {
  const map: Record<number, 'danger' | 'warning' | 'info'> = {
    0: 'danger',
    1: 'warning',
    2: 'info',
  }
  return map[level] ?? 'info'
}

const levelLabel = (level: number): string => {
  const map: Record<number, string> = { 0: 'P0', 1: 'P1', 2: 'P2' }
  return map[level] ?? `P${level}`
}

const statusTagType = (status: string): 'primary' | 'success' | 'info' => {
  const map: Record<string, 'primary' | 'success' | 'info'> = {
    '进行中': 'primary',
    '已完成': 'success',
    '已暂停': 'info',
  }
  return map[status] ?? 'info'
}

// --- Data fetching ---
const fetchProject = async () => {
  try {
    const res: any = await projectApi.detail(projectId)
    const data = res.data ?? res ?? {}
    Object.assign(project, data)
    satisfactionScore.value = data.satisfaction ?? 5
  } catch {
    // Error handled by interceptor
  }
}

const fetchStages = async () => {
  try {
    const res: any = await stageApi.listByProject(projectId)
    stages.value = res.data ?? res ?? []
  } catch {
    // Error handled by interceptor
  }
}

const fetchRisks = async () => {
  try {
    const res: any = await riskApi.listByProject(projectId)
    risks.value = res.data ?? res ?? []
  } catch {
    // Error handled by interceptor
  }
}

const fetchChangeLogs = async () => {
  try {
    const res: any = await changeLogApi.list(projectId)
    changeLogs.value = res.data ?? res ?? []
  } catch {
    // Error handled by interceptor
  }
}

const fetchAll = async () => {
  loading.value = true
  try {
    await Promise.all([fetchProject(), fetchStages(), fetchRisks(), fetchChangeLogs()])
  } finally {
    loading.value = false
  }
}

// --- Satisfaction save ---
const saveSatisfaction = async () => {
  savingSatisfaction.value = true
  try {
    await projectApi.updateSatisfaction(projectId, satisfactionScore.value)
    project.satisfaction = satisfactionScore.value
    ElMessage.success('满意度已更新')
  } catch {
    // Error handled by interceptor
  } finally {
    savingSatisfaction.value = false
  }
}

// --- Stage dialog ---
const openStageDialog = (row: Stage) => {
  Object.assign(stageForm, {
    id: row.id,
    name: row.name,
    planStart: row.planStart,
    planEnd: row.planEnd,
    actualStart: row.actualStart,
    actualEnd: row.actualEnd,
    status: row.status,
    remark: row.remark,
  })
  stageDialogVisible.value = true
}

const saveStage = async () => {
  savingStage.value = true
  try {
    const { id, name, ...payload } = stageForm
    await stageApi.update(id, payload)
    stageDialogVisible.value = false
    ElMessage.success('阶段已更新')

    // Prompt about whether this update involves personnel or content changes
    try {
      await ElMessageBox.confirm(
        '本次更新是否涉及人员或项目内容变更？',
        '确认',
        {
          confirmButtonText: '是',
          cancelButtonText: '否',
          type: 'question',
        }
      )
      // User clicked "是" -- switch to the changes tab for them to log it
      activeTab.value = 'changes'
      ElMessage.info('请在"变更记录"页签中登记本次变更')
    } catch {
      // User clicked "否" -- do nothing
    }

    await fetchStages()
  } catch {
    // Error handled by interceptor
  } finally {
    savingStage.value = false
  }
}

// --- Risk dialog ---
const openRiskDialog = (row: Risk | null) => {
  if (row) {
    Object.assign(riskForm, {
      id: row.id,
      description: row.description,
      type: row.type,
      severity: row.severity,
      status: row.status,
      owner: row.owner,
      mitigation: row.mitigation,
    })
  } else {
    Object.assign(riskForm, {
      id: 0,
      description: '',
      type: '',
      severity: '',
      status: '',
      owner: '',
      mitigation: '',
    })
  }
  riskDialogVisible.value = true
}

const saveRisk = async () => {
  savingRisk.value = true
  try {
    if (riskForm.id) {
      await riskApi.update(riskForm.id, { ...riskForm })
    } else {
      await riskApi.create(projectId, { ...riskForm })
    }
    riskDialogVisible.value = false
    ElMessage.success(riskForm.id ? '风险已更新' : '风险已登记')
    await fetchRisks()
  } catch {
    // Error handled by interceptor
  } finally {
    savingRisk.value = false
  }
}

// --- Change log dialog ---
const openChangeDialog = () => {
  changeForm.title = ''
  changeForm.content = ''
  changeDialogVisible.value = true
}

const saveChange = async () => {
  savingChange.value = true
  try {
    await changeLogApi.create(projectId, { ...changeForm })
    changeDialogVisible.value = false
    ElMessage.success('变更已登记')
    await fetchChangeLogs()
  } catch {
    // Error handled by interceptor
  } finally {
    savingChange.value = false
  }
}

// --- Init ---
onMounted(() => {
  fetchAll()
})
</script>

<style scoped>
.project-detail-page {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.satisfaction-row {
  display: flex;
  align-items: center;
}
</style>
