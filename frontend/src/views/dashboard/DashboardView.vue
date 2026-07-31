<template>
  <div class="dashboard-container">
    <div v-loading="loading" class="dashboard-content">
      <!-- Section 1: 风险/问题聚合区 -->
      <el-card class="dashboard-section">
        <template #header>
          <div class="section-header">
            <el-icon><Warning /></el-icon>
            <span>风险/问题聚合区</span>
          </div>
        </template>
        <el-table
          :data="dashboardData.riskAggregation"
          style="width: 100%"
          :row-class-name="getRiskRowClass"
          @row-click="handleRiskRowClick"
          highlight-current-row
        >
          <el-table-column prop="projectName" label="项目" width="150">
            <template #default="{ row }">
              <span class="link-text">{{ row.projectName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="riskCode" label="风险编号" width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="riskType" label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="row.riskType === '风险' ? 'warning' : 'danger'">{{ row.riskType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="severity" label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="getSeverityType(row.severity)">{{ row.severity }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="getStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ownerName" label="责任人" width="90" />
          <el-table-column prop="staleDays" label="停滞天数" width="90">
            <template #default="{ row }">
              <span :class="{ 'stale-warning': row.staleDays > 7 }">
                {{ row.staleDays }}天
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click.stop="goToRisk(row.projectId)">
                去处理
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="!loading && dashboardData.riskAggregation.length === 0" class="empty-tip">
          暂无待处理风险
        </div>
      </el-card>

      <!-- Section 2: 未来关键节点 -->
      <el-card class="dashboard-section">
        <template #header>
          <div class="section-header">
            <el-icon><Calendar /></el-icon>
            <span>未来关键节点</span>
          </div>
        </template>
        <el-table
          :data="dashboardData.futureNodes"
          style="width: 100%"
          :row-class-name="getMilestoneRowClass"
          @row-click="handleNodeRowClick"
        >
          <el-table-column prop="projectName" label="项目" width="200">
            <template #default="{ row }">
              <span class="link-text">{{ row.projectName }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="stageName" label="阶段" width="150" />
          <el-table-column prop="planEnd" label="计划完成日期" width="150" />
          <el-table-column prop="stageStatus" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.isOverdue ? 'danger' : 'success'">
                {{ row.isOverdue ? '已逾期' : row.stageStatus }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="data-source-tip">
          <el-icon><InfoFilled /></el-icon>
          数据来源：所有项目中计划结束日期在未来14天内的阶段节点，以及已逾期未完成的阶段（置顶显示）。数据基于各项目经理填报的阶段计划时间自动汇总。
        </div>
      </el-card>

      <!-- Section 3: 项目健康度总览 -->
      <el-card class="dashboard-section">
        <template #header>
          <div class="section-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>项目健康度总览</span>
            <span class="section-subtitle">（点击卡片查看项目详情）</span>
          </div>
        </template>
        <div class="health-grid">
          <el-card
            v-for="project in dashboardData.projectHealthList"
            :key="project.projectId"
            class="health-card"
            shadow="hover"
            @click="goToProject(project.projectId)"
          >
            <div class="health-card-content">
              <div class="project-name">{{ project.projectName }}</div>
              <div :class="['health-score', getHealthColorClass(project.healthColor)]">
                {{ project.healthScore }}
              </div>
              <div class="health-label">健康评分</div>
              <div class="current-stage">
                <el-tag size="small">{{ project.currentStage || '暂无阶段' }}</el-tag>
              </div>
              <div class="risk-summary">
                <span v-if="project.highRiskCount > 0" class="risk-badge high">
                  <el-icon><Warning /></el-icon> {{ project.highRiskCount }}个高危
                </span>
                <span v-if="project.staleRiskCount > 0" class="risk-badge stale">
                  <el-icon><Clock /></el-icon> {{ project.staleRiskCount }}个停滞
                </span>
                <span v-if="!project.highRiskCount && !project.staleRiskCount" class="risk-badge safe">
                  <el-icon><CircleCheck /></el-icon> 暂无异常
                </span>
              </div>
            </div>
          </el-card>
        </div>
        <div v-if="!loading && dashboardData.projectHealthList.length === 0" class="empty-tip">
          暂无项目数据
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Warning, Calendar, DataAnalysis, InfoFilled, Clock, CircleCheck } from '@element-plus/icons-vue'
import { dashboardApi } from '../../api'

const router = useRouter()
const loading = ref(false)
const dashboardData = ref<any>({
  riskAggregation: [],
  futureNodes: [],
  projectHealthList: []
})

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const response: any = await dashboardApi.summary()
    dashboardData.value = response.data
  } catch (error) {
    console.error('获取Dashboard数据失败:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到项目详情
function goToProject(projectId: number) {
  router.push(`/projects/${projectId}`)
}

// 跳转到项目详情的风险Tab
function goToRisk(projectId: number) {
  router.push({ path: `/projects/${projectId}`, query: { tab: 'risk' } })
}

// 点击风险行
function handleRiskRowClick(row: any) {
  goToRisk(row.projectId)
}

// 点击关键节点行
function handleNodeRowClick(row: any) {
  goToProject(row.projectId)
}

function getSeverityType(severity: string) {
  const map: Record<string, string> = { '高': 'danger', '中': 'warning', '低': 'info' }
  return map[severity] || 'info'
}

function getStatusType(status: string) {
  const map: Record<string, string> = {
    '待处理': 'danger', '处理中': 'warning', '已解决': 'success', '已关闭': 'info'
  }
  return map[status] || 'info'
}

function getRiskRowClass({ row }: { row: any }) {
  return row.staleDays > 7 ? 'stale-risk-row clickable-row' : 'clickable-row'
}

function getMilestoneRowClass({ row }: { row: any }) {
  return row.isOverdue ? 'overdue-row clickable-row' : 'clickable-row'
}

function getHealthColorClass(color: string) {
  const map: Record<string, string> = { '绿': 'health_green', '黄': 'health_yellow', '红': 'health_red' }
  return map[color] || 'health_green'
}

onMounted(() => {
  fetchDashboardData()
})
</script>

<style scoped>
.dashboard-container {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.dashboard-content {
  max-width: 1400px;
  margin: 0 auto;
}

.dashboard-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.section-header .el-icon {
  font-size: 20px;
  color: #409eff;
}

.section-subtitle {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 8px;
}

/* 链接文字 */
.link-text {
  color: #409eff;
  cursor: pointer;
}

.link-text:hover {
  text-decoration: underline;
}

/* 可点击行 */
:deep(.clickable-row) {
  cursor: pointer;
}

/* 风险停滞行 */
:deep(.stale-risk-row) {
  background-color: #fef0f0 !important;
}

:deep(.stale-risk-row:hover td) {
  background-color: #fde2e2 !important;
}

.stale-warning {
  color: #f56c6c;
  font-weight: 600;
}

/* 逾期行 */
:deep(.overdue-row) {
  background-color: #fef0f0 !important;
}

:deep(.overdue-row:hover td) {
  background-color: #fde2e2 !important;
}

/* 数据来源提示 */
.data-source-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  background: #f9fafb;
  border-radius: 4px;
  line-height: 1.6;
}

.data-source-tip .el-icon {
  flex-shrink: 0;
  color: #c0c4cc;
}

/* 空数据提示 */
.empty-tip {
  text-align: center;
  padding: 32px 0;
  color: #c0c4cc;
  font-size: 14px;
}

/* 健康度网格 */
.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.health-card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.health-card:hover {
  transform: translateY(-4px);
}

.health-card-content {
  text-align: center;
  padding: 20px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.health-score {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}

.health-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 12px;
}

.health_green { color: #67c23a; }
.health_yellow { color: #e6a23c; }
.health_red { color: #f56c6c; }

.current-stage {
  margin-bottom: 12px;
}

.risk-summary {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.risk-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
}

.risk-badge.high {
  color: #f56c6c;
  background: #fef0f0;
}

.risk-badge.stale {
  color: #e6a23c;
  background: #fdf6ec;
}

.risk-badge.safe {
  color: #67c23a;
  background: #f0f9eb;
}
</style>
