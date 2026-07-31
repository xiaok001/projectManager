<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Document /></el-icon> 项目报告</span>
          <div>
            <el-select
              v-model="selectedProject"
              placeholder="选择项目"
              clearable
              filterable
              style="margin-right:12px;width:240px"
            >
              <el-option label="全部项目" :value="undefined" />
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
      <div v-if="filteredHealthList.length" style="margin-bottom:24px">
        <h3 style="margin-bottom:16px">
          项目健康度总览
          <span style="font-size:13px;color:#909399;font-weight:normal;margin-left:8px">
            {{ selectedProject ? '当前项目' : `全部 ${filteredHealthList.length} 个项目` }}
          </span>
        </h3>
        <el-row :gutter="16">
          <el-col
            v-for="h in filteredHealthList"
            :key="h.projectId"
            :xs="24" :sm="12" :md="8"
            style="margin-bottom:16px"
          >
            <el-card shadow="hover" class="health-report-card">
              <div style="text-align:center">
                <h4>{{ h.projectName }}</h4>
                <div :class="'health-' + healthColorMap(h.healthColor)"
                     style="font-size:36px;line-height:1.4">
                  {{ h.healthScore }}
                </div>
                <div style="color:#909399;font-size:13px;margin-top:4px">
                  时间:{{ h.timeScore }} &nbsp; 风险:{{ h.riskScore }} &nbsp; 交付:{{ h.deliveryScore }}
                </div>
                <div style="margin-top:8px">
                  <el-tag size="small">{{ h.currentStage || '暂无阶段' }}</el-tag>
                  <el-tag
                    v-if="h.satisfactionScore"
                    size="small"
                    type="info"
                    style="margin-left:6px"
                  >
                    满意度: {{ h.satisfactionScore }}/10
                  </el-tag>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <div v-else-if="!loading" style="text-align:center;color:#909399;padding:24px 0">
        {{ selectedProject ? '该项目暂无健康度数据' : '暂无项目数据' }}
      </div>

      <el-divider />

      <div style="text-align:center;color:#909399;padding:40px 0">
        <el-icon :size="48"><Document /></el-icon>
        <p style="margin-top:16px">
          {{ reportType === 'weekly' ? '周报' : '月报' }}生成功能将在Sprint 5完成，<br>
          包含AI叙述性总结，敬请期待。
        </p>
        <el-button type="primary" disabled style="margin-top:12px">
          生成{{ reportType === 'weekly' ? '周报' : '月报' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { dashboardApi, projectApi } from '../../api'

const loading = ref(false)
const reportType = ref('weekly')
const selectedProject = ref<number | undefined>()
const projects = ref<any[]>([])
const healthList = ref<any[]>([])

// 根据选中项目过滤健康度列表
const filteredHealthList = computed(() => {
  if (!selectedProject.value) {
    return healthList.value
  }
  return healthList.value.filter(h => h.projectId === selectedProject.value)
})

function healthColorMap(color: string) {
  const map: Record<string, string> = { '绿': 'green', '黄': 'yellow', '红': 'red' }
  return map[color] || 'green'
}

onMounted(async () => {
  loading.value = true
  try {
    const [projRes, healthRes]: any[] = await Promise.all([
      projectApi.list(),
      dashboardApi.healthList(),
    ])
    projects.value = projRes.data
    healthList.value = healthRes.data
  } catch (e) { /* ignore */ }
  loading.value = false
})
</script>

<style scoped>
.health-report-card {
  transition: transform 0.2s;
}
.health-report-card:hover {
  transform: translateY(-2px);
}
</style>
