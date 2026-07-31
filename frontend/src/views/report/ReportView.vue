<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Document /></el-icon> 项目报告</span>
          <div>
            <el-select v-model="selectedProject" placeholder="选择项目" clearable style="margin-right:12px;width:200px">
              <el-option v-for="p in projects" :key="p.id" :value="p.id" :label="p.name" />
            </el-select>
            <el-radio-group v-model="reportType">
              <el-radio-button value="weekly">周报</el-radio-button>
              <el-radio-button value="monthly">月报</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 项目健康度概览 -->
      <div v-if="healthList.length" style="margin-bottom:24px">
        <h3 style="margin-bottom:16px">项目健康度总览</h3>
        <el-row :gutter="16">
          <el-col v-for="h in healthList" :key="h.projectId" :span="8" style="margin-bottom:16px">
            <el-card shadow="hover">
              <div style="text-align:center">
                <h4>{{ h.projectName }}</h4>
                <div :class="'health-' + (h.healthColor === '绿' ? 'green' : h.healthColor === '黄' ? 'yellow' : 'red')"
                     style="font-size:36px;line-height:1.4">
                  {{ h.healthScore }}
                </div>
                <div style="color:#909399;font-size:13px">
                  时间:{{ h.timeScore }} 风险:{{ h.riskScore }} 交付:{{ h.deliveryScore }}
                </div>
                <el-tag size="small" style="margin-top:8px">{{ h.currentStage || '无阶段' }}</el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <el-divider />

      <div style="text-align:center;color:#909399;padding:40px 0">
        <el-icon :size="48"><Document /></el-icon>
        <p style="margin-top:16px">
          {{ reportType === 'weekly' ? '周报' : '月报' }}生成功能将在Sprint 5完成，<br>
          包含AI叙述性总结，敬请期待。
        </p>
        <el-button type="primary" disabled style="margin-top:12px">生成{{ reportType === 'weekly' ? '周报' : '月报' }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { dashboardApi, projectApi } from '../../api'

const reportType = ref('weekly')
const selectedProject = ref<number | undefined>()
const projects = ref<any[]>([])
const healthList = ref<any[]>([])

onMounted(async () => {
  try {
    const [projRes, healthRes]: any[] = await Promise.all([
      projectApi.list(),
      dashboardApi.healthList(),
    ])
    projects.value = projRes.data
    healthList.value = healthRes.data
  } catch (e) { /* ignore */ }
})
</script>
