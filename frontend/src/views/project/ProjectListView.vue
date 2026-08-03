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
      :data="pagedList"
      border
      stripe
      style="width: 100%"
    >
      <el-table-column prop="projectCode" label="项目编号" width="140" />
      <el-table-column prop="name" label="项目名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column label="等级" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.level)" disable-transitions>
            {{ levelLabel(row.level) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="{ row }">
          {{ row.amount != null ? `¥${Number(row.amount).toLocaleString()}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="pmName" label="项目经理" width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" disable-transitions>
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="currentStage" label="当前阶段" width="120" />
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="router.push(`/projects/${row.id}`)">
            详情
          </el-button>
          <el-popconfirm
            :title="`确认删除项目「${row.name}」？将同时删除该项目下的所有阶段、风险、变更记录，此操作不可恢复。`"
            confirm-button-text="确认删除"
            cancel-button-text="取消"
            confirm-button-type="danger"
            width="360"
            @confirm="handleDelete(row)"
          >
            <template #reference>
              <el-button type="danger" link>删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="projectList.length > pageSize" class="pagination-wrap">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="projectList.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { projectApi } from '../../api'

const router = useRouter()
const loading = ref(false)
const projectList = ref<any[]>([])
const currentPage = ref(1)
const pageSize = ref(10)

// 前端分页
const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return projectList.value.slice(start, start + pageSize.value)
})

const levelTagType = (level: number) => ({ 0: 'danger', 1: 'warning', 2: 'info' }[level] ?? 'info')
const levelLabel = (level: number) => ({ 0: 'P0', 1: 'P1', 2: 'P2' }[level] ?? `P${level}`)
const statusTagType = (s: string) => ({ '进行中': 'primary', '已完成': 'success', '已暂停': 'info' }[s] ?? 'info')

const fetchProjects = async () => {
  loading.value = true
  try {
    const res: any = await projectApi.list()
    projectList.value = res.data ?? []
  } catch { /* handled */ }
  loading.value = false
}

onMounted(() => { fetchProjects() })

async function handleDelete(row: any) {
  try {
    await projectApi.delete(row.id)
    ElMessage.success(`项目「${row.name}」已删除`)
    fetchProjects()
  } catch { /* handled */ }
}
</script>

<style scoped>
.project-list-page { padding: 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0; }
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
