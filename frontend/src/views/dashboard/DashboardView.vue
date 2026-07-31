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
        >
          <el-table-column prop="projectName" label="项目" width="150" />
          <el-table-column prop="riskCode" label="风险编号" width="120" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="severity" label="严重程度" width="100">
            <template #default="{ row }">
              <el-tag :type="getSeverityType(row.severity)">
                {{ row.severity }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column prop="owner" label="责任人" width="100" />
          <el-table-column prop="staleDays" label="停滞天数" width="100">
            <template #default="{ row }">
              <span :class="{ 'stale-warning': row.staleDays > 7 }">
                {{ row.staleDays }}天
              </span>
            </template>
          </el-table-column>
        </el-table>
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
        >
          <el-table-column prop="projectName" label="项目" width="200" />
          <el-table-column prop="phase" label="阶段" width="150" />
          <el-table-column prop="plannedDate" label="计划完成日期" width="150" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.isOverdue ? 'danger' : 'success'">
                {{ row.isOverdue ? '已逾期' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- Section 3: 项目健康度总览 -->
      <el-card class="dashboard-section">
        <template #header>
          <div class="section-header">
            <el-icon><DataAnalysis /></el-icon>
            <span>项目健康度总览</span>
          </div>
        </template>
        <div class="health-grid">
          <el-card
            v-for="project in dashboardData.projectHealthList"
            :key="project.projectId"
            class="health-card"
            shadow="hover"
          >
            <div class="health-card-content">
              <div class="project-name">{{ project.projectName }}</div>
              <div :class="['health-score', getHealthColorClass(project.healthColor)]">
                {{ project.healthScore }}
              </div>
              <div class="current-stage">
                <el-tag size="small">{{ project.currentStage }}</el-tag>
              </div>
              <div class="risk-icons">
                <el-tooltip
                  v-for="(risk, index) in project.risks"
                  :key="index"
                  :content="risk.description"
                  placement="top"
                >
                  <el-icon
                    :class="getRiskIconClass(risk.severity)"
                    class="risk-icon"
                  >
                    <Warning />
                  </el-icon>
                </el-tooltip>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Warning, Calendar, DataAnalysis } from '@element-plus/icons-vue'
import { dashboardApi } from '../../api'

interface Risk {
  projectId: string
  projectName: string
  riskCode: string
  description: string
  type: string
  severity: '高' | '中' | '低'
  status: string
  owner: string
  staleDays: number
}

interface Milestone {
  projectId: string
  projectName: string
  phase: string
  plannedDate: string
  status: string
  isOverdue: boolean
}

interface RiskIcon {
  severity: '高' | '中' | '低'
  description: string
}

interface ProjectHealth {
  projectId: string
  projectName: string
  healthScore: number
  healthColor: 'green' | 'yellow' | 'red'
  currentStage: string
  risks: RiskIcon[]
}

interface DashboardData {
  riskAggregation: Risk[]
  futureNodes: Milestone[]
  projectHealthList: ProjectHealth[]
}

const loading = ref(false)
const dashboardData = ref<DashboardData>({
  riskAggregation: [],
  futureNodes: [],
  projectHealthList: []
})

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const response = await dashboardApi.summary()
    dashboardData.value = response.data
  } catch (error) {
    console.error('Failed to fetch dashboard data:', error)
  } finally {
    loading.value = false
  }
}

const getSeverityType = (severity: string) => {
  const typeMap: Record<string, string> = {
    '高': 'danger',
    '中': 'warning',
    '低': 'info'
  }
  return typeMap[severity] || 'info'
}

const getRiskRowClass = ({ row }: { row: Risk }) => {
  return row.staleDays > 7 ? 'stale-risk-row' : ''
}

const getMilestoneRowClass = ({ row }: { row: Milestone }) => {
  return row.isOverdue ? 'overdue-row' : ''
}

const getHealthColorClass = (color: string) => {
  const classMap: Record<string, string> = {
    'green': 'health_green',
    'yellow': 'health_yellow',
    'red': 'health_red'
  }
  return classMap[color] || 'health_green'
}

const getRiskIconClass = (severity: string) => {
  const classMap: Record<string, string> = {
    '高': 'risk-high',
    '中': 'risk-medium',
    '低': 'risk-low'
  }
  return classMap[severity] || 'risk-low'
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

/* 风险表格样式 */
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

/* 里程碑表格样式 */
:deep(.overdue-row) {
  background-color: #fef0f0 !important;
}

:deep(.overdue-row:hover td) {
  background-color: #fde2e2 !important;
}

/* 健康度网格 */
.health-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.health-card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
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
  margin-bottom: 16px;
}

.health-score {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 16px;
}

.health_green {
  color: #67c23a;
}

.health_yellow {
  color: #e6a23c;
}

.health_red {
  color: #f56c6c;
}

.current-stage {
  margin-bottom: 16px;
}

.risk-icons {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.risk-icon {
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.risk-icon:hover {
  transform: scale(1.2);
}

.risk-high {
  color: #f56c6c;
}

.risk-medium {
  color: #e6a23c;
}

.risk-low {
  color: #909399;
}
</style>
