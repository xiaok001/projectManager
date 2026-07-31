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
        <el-descriptions-item label="项目编号">{{ project.projectCode }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ project.name }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ project.type }}</el-descriptions-item>
        <el-descriptions-item label="等级">
          <el-tag :type="levelTagType(project.level)">{{ levelLabel(project.level) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          {{ project.amount != null ? `¥${Number(project.amount).toLocaleString()}` : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ project.pmName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(project.status)">{{ project.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户满意度">
          <div class="satisfaction-row">
            <el-input-number v-model="satisfactionScore" :min="1" :max="10" :step="1" size="small" style="width:120px" />
            <el-button type="primary" size="small" :loading="savingSatisfaction" style="margin-left:8px" @click="saveSatisfaction">保存</el-button>
          </div>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- Tab 区域 -->
    <el-card shadow="never" style="margin-top:16px">
      <el-tabs v-model="activeTab">

        <!-- ========== 阶段管理 Tab ========== -->
        <el-tab-pane label="阶段管理" name="stages">
          <el-table :data="stages" border stripe style="width:100%">
            <el-table-column prop="stageName" label="阶段" width="100" />
            <el-table-column label="计划时间" min-width="170">
              <template #default="{ row }">
                <div>{{ row.planStart || '-' }} ~ {{ row.planEnd || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="实际时间" min-width="170">
              <template #default="{ row }">
                <div>{{ row.actualStart || '-' }} ~ {{ row.actualEnd || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="预估" width="130">
              <template #default="{ row }">
                <div v-if="row.planManDays || row.planCost">
                  <div v-if="row.planManDays">{{ row.planManDays }} 人天</div>
                  <div v-if="row.planCost" style="color:#909399;font-size:12px">¥{{ Number(row.planCost).toLocaleString() }}</div>
                </div>
                <span v-else style="color:#c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="实际" width="130">
              <template #default="{ row }">
                <div v-if="row.actualManDays || row.actualCost">
                  <div v-if="row.actualManDays">{{ row.actualManDays }} 人天</div>
                  <div v-if="row.actualCost" style="color:#909399;font-size:12px">¥{{ Number(row.actualCost).toLocaleString() }}</div>
                </div>
                <span v-else style="color:#c0c4cc">-</span>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.progress || 0" :stroke-width="14" :text-inside="true" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="stageStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="70" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openStageDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 风险列表 Tab ========== -->
        <el-tab-pane label="风险列表" name="risks">
          <div style="margin-bottom:12px;text-align:right">
            <el-button type="primary" @click="openRiskDialog(null)">登记新风险</el-button>
          </div>
          <el-table :data="risks" border stripe style="width:100%">
            <el-table-column prop="riskCode" label="风险编号" width="160" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="severity" label="严重程度" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="severityType(row.severity)">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="riskStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="责任人" width="90" />
            <el-table-column prop="actionPlan" label="处理措施" min-width="160" show-overflow-tooltip />
            <el-table-column prop="isStale" label="停滞" width="70" align="center">
              <template #default="{ row }">
                <el-tag :type="row.isStale ? 'danger' : 'success'" size="small">{{ row.isStale ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link @click="openRiskDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 变更记录 Tab (修改前后对比) ========== -->
        <el-tab-pane label="变更记录" name="changes">
          <div style="margin-bottom:16px;text-align:right">
            <el-button type="primary" @click="openChangeDialog">登记变更</el-button>
          </div>
          <div v-if="changeLogs.length > 0" class="change-timeline">
            <div v-for="log in changeLogs" :key="log.id" class="change-item">
              <div class="change-time-line">
                <div class="change-dot" :class="changeTypeColor(log.changeType)"></div>
                <div class="change-line"></div>
              </div>
              <div class="change-card">
                <div class="change-header">
                  <el-tag size="small" :type="changeTagType(log.changeType)">{{ log.changeType }}</el-tag>
                  <span v-if="log.changeField" class="change-field">{{ log.changeField }}</span>
                  <span class="change-time">{{ log.changedAt }}</span>
                </div>
                <div class="change-desc">{{ log.changeDesc }}</div>
                <div v-if="log.beforeValue || log.afterValue" class="change-diff">
                  <div class="diff-row">
                    <div class="diff-before">
                      <span class="diff-label">变更前</span>
                      <span class="diff-value old">{{ log.beforeValue || '(空)' }}</span>
                    </div>
                    <div class="diff-arrow">→</div>
                    <div class="diff-after">
                      <span class="diff-label">变更后</span>
                      <span class="diff-value new">{{ log.afterValue || '(空)' }}</span>
                    </div>
                  </div>
                </div>
                <div class="change-footer">操作人：{{ log.changedByName || '-' }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无变更记录" />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- ========== 阶段编辑弹窗 ========== -->
    <el-dialog v-model="stageDialogVisible" title="编辑阶段" width="660px" destroy-on-close>
      <el-form :model="stageForm" label-width="100px">
        <el-form-item label="阶段名称">
          <el-select v-model="stageForm.stageName" placeholder="选择或输入阶段名" filterable allow-create style="width:100%">
            <el-option v-for="s in stageOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="计划开始">
              <el-date-picker v-model="stageForm.planStart" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束">
              <el-date-picker v-model="stageForm.planEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="实际开始">
              <el-date-picker v-model="stageForm.actualStart" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际结束">
              <el-date-picker v-model="stageForm.actualEnd" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">工时与成本</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预估人天">
              <el-input-number v-model="stageForm.planManDays" :min="0" :precision="1" :step="1" placeholder="人天" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际人天">
              <el-input-number v-model="stageForm.actualManDays" :min="0" :precision="1" :step="1" placeholder="人天" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="预估成本">
              <el-input-number v-model="stageForm.planCost" :min="0" :precision="0" :step="1000" placeholder="元" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实际成本">
              <el-input-number v-model="stageForm.actualCost" :min="0" :precision="0" :step="1000" placeholder="元" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="完成进度">
              <el-slider v-model="stageForm.progress" :min="0" :max="100" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-select v-model="stageForm.status" placeholder="选择状态" style="width:100%">
                <el-option label="未开始" value="未开始" />
                <el-option label="进行中" value="进行中" />
                <el-option label="已完成" value="已完成" />
                <el-option label="已延期" value="已延期" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="stageForm.remark" type="textarea" :rows="3" placeholder="阶段进展说明（AI风险探测将分析此内容）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStage" @click="saveStage">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 风险编辑弹窗 ========== -->
    <el-dialog v-model="riskDialogVisible" :title="riskForm.id ? '编辑风险' : '登记新风险'" width="600px" destroy-on-close>
      <el-form :model="riskForm" label-width="90px">
        <el-form-item label="描述">
          <el-input v-model="riskForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="riskForm.type" placeholder="选择或自定义类型" filterable allow-create style="width:100%">
            <el-option v-for="t in riskTypeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="riskForm.severity" placeholder="选择严重程度" style="width:100%">
            <el-option label="高" value="高" />
            <el-option label="中" value="中" />
            <el-option label="低" value="低" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="riskForm.status" placeholder="选择状态" style="width:100%">
            <el-option label="待处理" value="待处理" />
            <el-option label="处理中" value="处理中" />
            <el-option label="已解决" value="已解决" />
            <el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任人">
          <el-input v-model="riskForm.owner" />
        </el-form-item>
        <el-form-item label="处理措施">
          <el-input v-model="riskForm.actionPlan" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="riskDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingRisk" @click="saveRisk">保存</el-button>
      </template>
    </el-dialog>

    <!-- ========== 变更登记弹窗 ========== -->
    <el-dialog v-model="changeDialogVisible" title="登记变更" width="560px" destroy-on-close>
      <el-form :model="changeForm" label-width="90px">
        <el-form-item label="变更类型">
          <el-select v-model="changeForm.changeType" placeholder="选择变更类型" style="width:100%">
            <el-option label="人员变更" value="人员变更" />
            <el-option label="内容变更" value="内容变更" />
            <el-option label="范围变更" value="范围变更" />
            <el-option label="风险变更" value="风险变更" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更字段">
          <el-input v-model="changeForm.changeField" placeholder="如: 项目经理、需求范围、风险等级" />
        </el-form-item>
        <el-form-item label="变更说明">
          <el-input v-model="changeForm.changeDesc" type="textarea" :rows="2" placeholder="变更原因及说明" />
        </el-form-item>
        <el-divider content-position="left">变更对比（可选）</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="变更前">
              <el-input v-model="changeForm.beforeValue" type="textarea" :rows="2" placeholder="原值" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="变更后">
              <el-input v-model="changeForm.afterValue" type="textarea" :rows="2" placeholder="新值" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="changeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingChange" @click="saveChange">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi, stageApi, riskApi, changeLogApi } from '../../api'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)
const loading = ref(false)

// --- 阶段枚举选项 ---
const stageOptions = ['启动', '调研', '开发', '测试验收', '上线', '试运行', '运维']

// --- 风险类型枚举 ---
const riskTypeOptions = ['技术风险', '进度风险', '需求风险', '资源风险', '质量风险', '外部风险', '成本风险', '管理风险']

// --- 项目信息 ---
const project = ref<any>({})
const satisfactionScore = ref(5)
const savingSatisfaction = ref(false)

// --- 阶段 ---
const stages = ref<any[]>([])
const stageDialogVisible = ref(false)
const savingStage = ref(false)
const stageForm = reactive<any>({
  id: 0, stageName: '', planStart: '', planEnd: '', actualStart: '', actualEnd: '',
  status: '', remark: '', planManDays: null, actualManDays: null,
  planCost: null, actualCost: null, progress: 0,
})

// --- 风险 ---
const risks = ref<any[]>([])
const riskDialogVisible = ref(false)
const savingRisk = ref(false)
const riskForm = reactive<any>({
  id: 0, description: '', type: '', severity: '', status: '', owner: '', actionPlan: '',
})

// --- 变更记录 ---
const changeLogs = ref<any[]>([])
const changeDialogVisible = ref(false)
const savingChange = ref(false)
const changeForm = reactive<any>({
  changeType: '', changeField: '', changeDesc: '', beforeValue: '', afterValue: '',
})

const activeTab = ref((route.query.tab as string) || 'stages')

// --- Helpers ---
const levelTagType = (level: number) => ({ 0: 'danger', 1: 'warning', 2: 'info' }[level] ?? 'info')
const levelLabel = (level: number) => ({ 0: 'P0', 1: 'P1', 2: 'P2' }[level] ?? `P${level}`)
const statusTagType = (s: string) => ({ '进行中': 'primary', '已完成': 'success', '已暂停': 'info' }[s] ?? 'info')
const stageStatusType = (s: string) => ({ '未开始': 'info', '进行中': 'primary', '已完成': 'success', '已延期': 'danger' }[s] ?? 'info')
const severityType = (s: string) => ({ '高': 'danger', '中': 'warning', '低': 'info' }[s] ?? 'info')
const riskStatusType = (s: string) => ({ '待处理': 'danger', '处理中': 'warning', '已解决': 'success', '已关闭': 'info' }[s] ?? 'info')
const changeTagType = (t: string) => ({ '人员变更': 'primary', '内容变更': 'warning', '范围变更': 'danger', '风险变更': 'success' }[t] ?? 'info')
const changeTypeColor = (t: string) => ({ '人员变更': 'blue', '内容变更': 'orange', '范围变更': 'red', '风险变更': 'green' }[t] ?? 'gray')

// --- 数据获取 ---
const fetchAll = async () => {
  loading.value = true
  try {
    const [projRes, stageRes, riskRes, changeRes]: any[] = await Promise.all([
      projectApi.detail(projectId),
      stageApi.listByProject(projectId),
      riskApi.listByProject(projectId),
      changeLogApi.list(projectId),
    ])
    project.value = projRes.data || {}
    satisfactionScore.value = project.value.satisfactionScore ?? 5
    stages.value = stageRes.data || []
    risks.value = riskRes.data || []
    changeLogs.value = changeRes.data || []
  } catch { /* handled */ }
  loading.value = false
}

// --- 满意度 ---
const saveSatisfaction = async () => {
  savingSatisfaction.value = true
  try {
    await projectApi.updateSatisfaction(projectId, satisfactionScore.value)
    ElMessage.success('满意度已更新')
  } catch { /* handled */ }
  savingSatisfaction.value = false
}

// --- 阶段编辑 ---
const openStageDialog = (row: any) => {
  Object.assign(stageForm, {
    id: row.id, stageName: row.stageName, planStart: row.planStart, planEnd: row.planEnd,
    actualStart: row.actualStart, actualEnd: row.actualEnd, status: row.status, remark: row.remark || '',
    planManDays: row.planManDays, actualManDays: row.actualManDays,
    planCost: row.planCost, actualCost: row.actualCost, progress: row.progress || 0,
  })
  stageDialogVisible.value = true
}

const saveStage = async () => {
  savingStage.value = true
  try {
    const { id, ...payload } = stageForm
    await stageApi.update(id, payload)
    stageDialogVisible.value = false
    ElMessage.success('阶段已更新')
    try {
      await ElMessageBox.confirm(
        '本次更新是否涉及人员或项目内容变更？',
        '提示',
        {
          confirmButtonText: '是，去登记',
          cancelButtonText: '否',
        }
      )
      activeTab.value = 'changes'
      openChangeDialog()
    } catch { /* 否 */ }
    await fetchAll()
  } catch { /* handled */ }
  savingStage.value = false
}

// --- 风险编辑 ---
const openRiskDialog = (row: any) => {
  if (row) {
    Object.assign(riskForm, {
      id: row.id, description: row.description, type: row.type, severity: row.severity,
      status: row.status, owner: row.ownerName || '', actionPlan: row.actionPlan || '',
    })
  } else {
    Object.assign(riskForm, { id: 0, description: '', type: '', severity: '', status: '待处理', owner: '', actionPlan: '' })
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
    await fetchAll()
  } catch { /* handled */ }
  savingRisk.value = false
}

// --- 变更登记 ---
const openChangeDialog = () => {
  Object.assign(changeForm, { changeType: '', changeField: '', changeDesc: '', beforeValue: '', afterValue: '' })
  changeDialogVisible.value = true
}

const saveChange = async () => {
  savingChange.value = true
  try {
    await changeLogApi.create(projectId, { ...changeForm })
    changeDialogVisible.value = false
    ElMessage.success('变更已登记')
    await fetchAll()
  } catch { /* handled */ }
  savingChange.value = false
}

onMounted(() => { fetchAll() })
</script>

<style scoped>
.project-detail-page { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.satisfaction-row { display: flex; align-items: center; }

/* 变更记录时间线 */
.change-timeline { position: relative; padding-left: 28px; }
.change-item { display: flex; margin-bottom: 20px; position: relative; }
.change-time-line { position: absolute; left: -28px; display: flex; flex-direction: column; align-items: center; }
.change-dot { width: 12px; height: 12px; border-radius: 50%; border: 2px solid #409eff; background: #fff; z-index: 1; }
.change-dot.blue { border-color: #409eff; }
.change-dot.orange { border-color: #e6a23c; }
.change-dot.red { border-color: #f56c6c; }
.change-dot.green { border-color: #67c23a; }
.change-dot.gray { border-color: #909399; }
.change-line { width: 2px; flex: 1; background: #e4e7ed; margin-top: 4px; min-height: 20px; }
.change-item:last-child .change-line { display: none; }
.change-card { flex: 1; background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 16px; transition: box-shadow 0.2s; }
.change-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.change-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.change-field { font-weight: 600; color: #303133; }
.change-time { margin-left: auto; font-size: 12px; color: #909399; }
.change-desc { color: #606266; font-size: 14px; margin-bottom: 10px; line-height: 1.6; }
.change-footer { font-size: 12px; color: #909399; margin-top: 8px; }

/* 前后对比 */
.change-diff { background: #fafafa; border-radius: 6px; padding: 12px; }
.diff-row { display: flex; align-items: center; gap: 12px; }
.diff-before, .diff-after { flex: 1; }
.diff-label { display: block; font-size: 11px; color: #909399; margin-bottom: 4px; text-transform: uppercase; letter-spacing: 1px; }
.diff-value { display: block; padding: 6px 10px; border-radius: 4px; font-size: 13px; word-break: break-all; }
.diff-value.old { background: #fef0f0; color: #f56c6c; text-decoration: line-through; }
.diff-value.new { background: #f0f9eb; color: #67c23a; font-weight: 500; }
.diff-arrow { color: #c0c4cc; font-size: 18px; flex-shrink: 0; }
</style>
