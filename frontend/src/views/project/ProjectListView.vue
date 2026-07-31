<template>
  <div class="project-list-page">
    <div class="page-header">
      <h2>项目管理</h2>
      <el-button type="primary" @click="router.push('/projects/create')">
        创建项目
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="projectList"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="code" label="项目编号" min-width="120" />
      <el-table-column prop="name" label="项目名称" min-width="180" />
      <el-table-column prop="type" label="类型" min-width="100" />
      <el-table-column label="等级" min-width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.level)" disable-transitions>
            {{ levelLabel(row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" min-width="120" align="right">
        <template #default="{ row }">
          {{ row.amount != null ? `¥${row.amount.toLocaleString()}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="pmName" label="项目经理" min-width="100" />
      <el-table-column label="状态" min-width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" disable-transitions>
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentStage" label="当前阶段" min-width="120" />
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            @click="router.push(`/projects/${row.id}`)"
          >
            详情
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi } from '../../api'

interface Project {
  id: number
  code: string
  name: string
  type: string
  level: number
  amount: number
  pmName: string
  status: string
  currentStage: string
}

const router = useRouter()
const loading = ref(false)
const projectList = ref<Project[]>([])

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

const fetchProjects = async () => {
  loading.value = true
  try {
    const res: any = await projectApi.list()
    projectList.value = res.data ?? res ?? []
  } catch {
    // Error is handled by the response interceptor
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.project-list-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
}
</style>
