<template>
  <div class="page-container">
    <div class="page-header">
      <h2>项目周报</h2>
      <div style="display:flex;gap:12px">
        <el-select v-model="selectedProject" placeholder="全部项目" clearable filterable style="width:220px">
          <el-option label="全部项目" :value="undefined" />
          <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="generateReport">
          <el-icon><Document /></el-icon> 生成周报
        </el-button>
      </div>
    </div>

    <!-- 报告内容 -->
    <el-card shadow="never">
      <div v-if="!report && !loading" style="text-align:center;padding:60px 0;color:#909399">
        <el-icon :size="48"><Document /></el-icon>
        <p style="margin-top:16px">请选择项目范围后点击「生成周报」</p>
      </div>

      <!-- 周报内容 -->
      <div v-if="report" v-loading="loading" class="report-content">
        <!-- 报告头 -->
        <div class="report-header">
          <h2>{{ report.projectName ? report.projectName + ' - ' : '' }}项目周报</h2>
          <p class="period">报告周期：{{ report.period }}</p>
        </div>

        <!-- AI总结 -->
        <div v-if="report.aiSummary" class="ai-summary">
          <div class="ai-summary-header">
            <el-icon><Cpu /></el-icon> AI 智能总结
          </div>
          <div class="ai-summary-body">{{ report.aiSummary }}</div>
        </div>

        <!-- 统计卡片 -->
        <el-row :gutter="16" class="stat-row">
          <el-col :span="6">
            <div class="report-stat-card">
              <div class="stat-num blue">{{ report.completedStages?.length || 0 }}</div>
              <div class="stat-desc">本周完成阶段</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-stat-card">
              <div class="stat-num orange">{{ report.newRisks?.length || 0 }}</div>
              <div class="stat-desc">本周新增风险</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-stat-card">
              <div class="stat-num green">{{ report.closedRisks?.length || 0 }}</div>
              <div class="stat-desc">本周关闭风险</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="report-stat-card">
              <div class="stat-num" :class="report.todoStats?.overdue > 0 ? 'red' : 'gray'">{{ report.todoStats?.overdue || 0 }}</div>
              <div class="stat-desc">待办逾期</div>
            </div>
          </el-col>
        </el-row>

        <!-- 本周完成阶段 -->
        <div class="report-section">
          <h3><el-icon><CircleCheck /></el-icon> 本周完成阶段</h3>
          <el-table v-if="report.completedStages?.length" :data="report.completedStages" border size="small">
            <el-table-column prop="projectName" label="项目" width="160" />
            <el-table-column prop="stageName" label="阶段" width="120" />
            <el-table-column prop="actualEnd" label="完成日期" width="120" />
          </el-table>
          <el-empty v-else description="本周无完成阶段" :image-size="60" />
        </div>

        <!-- 本周新增风险 -->
        <div class="report-section">
          <h3><el-icon><Warning /></el-icon> 本周新增风险（{{ report.newRisks?.length || 0 }}条）</h3>
          <el-table v-if="report.newRisks?.length" :data="report.newRisks" border size="small">
            <el-table-column prop="projectName" label="项目" width="140" />
            <el-table-column prop="riskCode" label="风险编号" width="160" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="severity" label="严重程度" width="90">
              <template #default="{ row }">
                <el-tag :type="row.severity==='高'?'danger':row.severity==='中'?'warning':'info'" size="small">{{ row.severity }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="本周无新增风险" :image-size="60" />
        </div>

        <!-- 当前风险统计 -->
        <div class="report-section">
          <h3><el-icon><DataAnalysis /></el-icon> 当前未关闭风险统计</h3>
          <el-row :gutter="12">
            <el-col :span="4"><el-statistic title="总计" :value="report.riskStats?.total || 0" /></el-col>
            <el-col :span="4"><el-statistic title="高危" :value="report.riskStats?.high || 0" value-style="color:#f56c6c" /></el-col>
            <el-col :span="4"><el-statistic title="中危" :value="report.riskStats?.medium || 0" value-style="color:#e6a23c" /></el-col>
            <el-col :span="4"><el-statistic title="低危" :value="report.riskStats?.low || 0" /></el-col>
            <el-col :span="4"><el-statistic title="停滞" :value="report.riskStats?.stale || 0" value-style="color:#f56c6c" /></el-col>
          </el-row>
        </div>

        <!-- 下周计划节点 -->
        <div class="report-section">
          <h3><el-icon><Calendar /></el-icon> 下周计划节点</h3>
          <el-table v-if="report.upcomingStages?.length" :data="report.upcomingStages" border size="small">
            <el-table-column prop="projectName" label="项目" width="160" />
            <el-table-column prop="stageName" label="阶段" width="120" />
            <el-table-column prop="planEnd" label="计划完成" width="120" />
            <el-table-column prop="status" label="当前状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status==='已延期'?'danger':'primary'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="下周无计划节点" :image-size="60" />
        </div>

        <!-- 待办统计 -->
        <div class="report-section">
          <h3><el-icon><Finished /></el-icon> 待办事项统计</h3>
          <el-row :gutter="12">
            <el-col :span="6"><el-statistic title="总计" :value="report.todoStats?.total || 0" /></el-col>
            <el-col :span="6"><el-statistic title="已完成" :value="report.todoStats?.completed || 0" value-style="color:#67c23a" /></el-col>
            <el-col :span="6"><el-statistic title="进行中" :value="report.todoStats?.inProgress || 0" value-style="color:#409eff" /></el-col>
            <el-col :span="6"><el-statistic title="逾期" :value="report.todoStats?.overdue || 0" value-style="color:#f56c6c" /></el-col>
          </el-row>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { projectApi, reportApi } from '../../api'

const loading = ref(false)
const projects = ref<any[]>([])
const selectedProject = ref<number | undefined>()
const report = ref<any>(null)

onMounted(async () => {
  try {
    const res: any = await projectApi.list()
    projects.value = res.data || []
  } catch { /* ignore */ }
})

async function generateReport() {
  // 检查选中的项目是否已结束
  if (selectedProject.value) {
    const p = projects.value.find((x: any) => x.id === selectedProject.value)
    if (p && p.status === '已完成') {
      try {
        await ElMessageBox.confirm(
          `项目「${p.name}」已处于结束状态，生成的周报可能无最新数据。是否继续生成？`,
          '提示',
          { confirmButtonText: '继续生成', cancelButtonText: '取消', type: 'warning' }
        )
      } catch { return }
    }
  }

  loading.value = true
  try {
    const res: any = await reportApi.weekly(selectedProject.value)
    report.value = res.data
    ElMessage.success('周报生成完成')
  } catch { /* handled */ }
  loading.value = false
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
.report-content { max-width: 1000px; }
.report-header { text-align: center; margin-bottom: 24px; }
.report-header h2 { font-size: 22px; margin: 0; }
.period { color: #909399; font-size: 14px; margin-top: 4px; }

.ai-summary {
  background: linear-gradient(135deg, #f0f5ff, #e8f4fd);
  border: 1px solid #d6e4ff;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 24px;
}
.ai-summary-header {
  display: flex; align-items: center; gap: 6px;
  font-weight: 600; color: #1677ff; margin-bottom: 8px; font-size: 15px;
}
.ai-summary-body { color: #303133; line-height: 1.8; font-size: 14px; white-space: pre-wrap; }

.stat-row { margin-bottom: 24px; }
.report-stat-card {
  text-align: center; padding: 16px;
  background: #fff; border: 1px solid #e4e7ed; border-radius: 8px;
}
.stat-num { font-size: 28px; font-weight: 700; }
.stat-num.blue { color: #409eff; }
.stat-num.orange { color: #e6a23c; }
.stat-num.green { color: #67c23a; }
.stat-num.red { color: #f56c6c; }
.stat-num.gray { color: #909399; }
.stat-desc { font-size: 13px; color: #909399; margin-top: 4px; }

.report-section { margin-bottom: 24px; }
.report-section h3 {
  display: flex; align-items: center; gap: 6px;
  font-size: 16px; margin: 0 0 12px; color: #303133;
}
</style>
