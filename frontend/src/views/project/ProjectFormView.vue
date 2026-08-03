<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <span>{{ isEdit ? '编辑项目' : '创建项目' }}</span>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        style="max-width: 700px"
      >
        <el-form-item label="项目编号" prop="projectCode">
          <el-input v-model="form.projectCode" placeholder="如 PRJ-A01" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目类型" prop="type">
          <el-input v-model="form.type" placeholder="默认: 软件开发" />
        </el-form-item>
        <el-form-item label="项目等级" prop="level">
          <el-select v-model="form.level" placeholder="请选择">
            <el-option :value="0" label="P0-紧急" />
            <el-option :value="1" label="P1-重要" />
            <el-option :value="2" label="P2-一般" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10000" style="width:300px" />
        </el-form-item>
        <el-form-item label="项目经理" prop="pmId">
          <el-select v-model="form.pmId" placeholder="请选择" filterable>
            <el-option v-for="u in users" :key="u.id" :value="u.id" :label="u.realName" />
          </el-select>
        </el-form-item>
        <el-form-item label="立项日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="预期结束日期">
          <el-date-picker v-model="form.expectedEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择预期结束日期" />
        </el-form-item>

        <!-- 编辑模式下显示状态选择 -->
        <template v-if="isEdit">
          <el-divider content-position="left">项目状态</el-divider>
          <el-form-item label="当前状态">
            <el-select v-model="form.status" placeholder="请选择状态" style="width:200px">
              <el-option value="进行中" label="进行中" />
              <el-option value="已暂停" label="已暂停" />
              <el-option value="已完成" label="已完成" />
            </el-select>
          </el-form-item>

          <!-- 选择"已完成"时的阶段检查提示 -->
          <el-alert
            v-if="form.status === '已完成' && incompleteStages.length > 0"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom:16px"
          >
            <template #title>
              以下阶段尚未完成，请先更新阶段数据或在阶段备注中说明原因
            </template>
            <template #default>
              <div style="margin-top:8px">
                <el-tag
                  v-for="s in incompleteStages"
                  :key="s.id"
                  type="warning"
                  size="small"
                  style="margin:2px 4px"
                >
                  {{ s.stageName }}（{{ s.status }}）
                </el-tag>
              </div>
            </template>
          </el-alert>
        </template>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '创建项目' }}
          </el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { projectApi, stageApi, authApi } from '../../api'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const users = ref<any[]>([])
const incompleteStages = ref<any[]>([])

const isEdit = computed(() => !!route.params.id)

const form = ref({
  projectCode: '',
  name: '',
  type: '软件开发',
  level: 2,
  amount: undefined as number | undefined,
  pmId: undefined as number | undefined,
  startDate: '',
  expectedEndDate: '',
  status: '进行中',
})

const rules: FormRules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择项目等级', trigger: 'change' }],
  pmId: [{ required: true, message: '请选择项目经理', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择立项日期', trigger: 'change' }],
}

// 监听状态变更，检查未完成阶段
watch(() => form.value.status, async (newStatus) => {
  if (newStatus === '已完成' && isEdit.value) {
    await checkIncompleteStages()
  } else {
    incompleteStages.value = []
  }
})

async function checkIncompleteStages() {
  try {
    const res: any = await stageApi.listByProject(Number(route.params.id))
    const stages = res.data || []
    incompleteStages.value = stages.filter((s: any) => s.status !== '已完成' && s.stageName !== '运维')
  } catch { /* ignore */ }
}

onMounted(async () => {
  try {
    const res: any = await authApi.getUsers()
    users.value = res.data
  } catch { /* ignore */ }

  if (isEdit.value) {
    try {
      const res: any = await projectApi.detail(Number(route.params.id))
      const p = res.data
      form.value = {
        projectCode: p.projectCode,
        name: p.name,
        type: p.type,
        level: p.level,
        amount: p.amount,
        pmId: p.pmId,
        startDate: p.startDate,
        expectedEndDate: p.expectedEndDate || '',
        status: p.status || '进行中',
      }
    } catch {
      ElMessage.error('加载项目信息失败')
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  // 选择"已完成"且有未完成阶段时，二次确认
  if (form.value.status === '已完成' && incompleteStages.value.length > 0) {
    const stageNames = incompleteStages.value.map((s: any) => s.stageName).join('、')
    try {
      await ElMessageBox.confirm(
        `当前项目存在未完成的阶段（${stageNames}），确定要将项目状态设为「已完成」吗？（运维阶段无需完成即可结束项目）`,
        '确认结束项目',
        { confirmButtonText: '确定结束', cancelButtonText: '取消', type: 'warning' }
      )
    } catch { return }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await projectApi.update(Number(route.params.id), form.value)
      ElMessage.success('项目更新成功')
    } else {
      await projectApi.create(form.value)
      ElMessage.success('项目创建成功')
    }
    router.push('/projects')
  } catch { /* handled */ }
  submitting.value = false
}
</script>
