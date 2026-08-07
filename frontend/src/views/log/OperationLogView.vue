<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><Notebook /></el-icon> 操作日志</span>
          <div style="display:flex;gap:12px">
            <el-select v-model="filterModule" placeholder="操作模块" clearable style="width:130px" @change="loadData">
              <el-option value="" label="全部模块" />
              <el-option value="项目" label="项目" />
              <el-option value="阶段" label="阶段" />
              <el-option value="风险" label="风险" />
              <el-option value="系统管理" label="系统管理" />
              <el-option value="认证" label="认证" />
              <el-option value="AI建议" label="AI建议" />
            </el-select>
            <el-select v-model="filterOperation" placeholder="操作类型" clearable style="width:130px" @change="loadData">
              <el-option value="" label="全部类型" />
              <el-option value="新增" label="新增" />
              <el-option value="修改" label="修改" />
              <el-option value="删除" label="删除" />
              <el-option value="登录" label="登录" />
              <el-option value="登出" label="登出" />
            </el-select>
          </div>
        </div>
      </template>

      <el-table :data="logs" v-loading="loading" stripe border style="min-width:1100px">
        <el-table-column prop="id" label="ID" width="60" fixed="left" />
        <el-table-column label="操作模块" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="opTagType(row.operation)">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="操作描述" min-width="200" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人" width="90">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="方法" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="methodTagType(row.requestMethod)" effect="plain">{{ row.requestMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求路径" min-width="220" :show-overflow-tooltip="{ popperClass: 'pm-tooltip' }">
          <template #default="{ row }">{{ row.requestUrl || '-' }}</template>
        </el-table-column>
        <el-table-column label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="80" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.executionTime > 1000 ? '#f56c6c' : '' }">
              {{ row.executionTime || 0 }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="170" />
      </el-table>

      <div style="margin-top:16px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../api'

const loading = ref(false)
const logs = ref<any[]>([])
const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filterModule = ref('')
const filterOperation = ref('')

onMounted(() => {
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }
    if (filterModule.value) params.module = filterModule.value
    if (filterOperation.value) params.operation = filterOperation.value

    const res: any = await api.get('/operation-logs', { params })
    logs.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (e) { /* ignore */ }
  loading.value = false
}

function opTagType(op: string) {
  const map: Record<string, string> = {
    '新增': 'success', '修改': 'warning', '删除': 'danger',
    '登录': 'primary', '登出': 'info', '查询': '',
  }
  return map[op] || ''
}

function methodTagType(method: string) {
  const map: Record<string, string> = {
    'GET': 'success', 'POST': 'primary', 'PUT': 'warning', 'DELETE': 'danger',
  }
  return map[method] || 'info'
}
</script>
