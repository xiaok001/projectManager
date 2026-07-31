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
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="10000" style="width: 300px" />
        </el-form-item>
        <el-form-item label="项目经理" prop="pmId">
          <el-select v-model="form.pmId" placeholder="请选择" filterable>
            <el-option
              v-for="u in users"
              :key="u.id"
              :value="u.id"
              :label="u.realName"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="立项日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
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
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { projectApi, authApi } from '../../api'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const users = ref<any[]>([])

const isEdit = computed(() => !!route.params.id)

const form = ref({
  projectCode: '',
  name: '',
  type: '软件开发',
  level: 2,
  amount: undefined as number | undefined,
  pmId: undefined as number | undefined,
  startDate: '',
})

const rules: FormRules = {
  projectCode: [{ required: true, message: '请输入项目编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择项目等级', trigger: 'change' }],
  pmId: [{ required: true, message: '请选择项目经理', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择立项日期', trigger: 'change' }],
}

onMounted(async () => {
  // 加载用户列表
  try {
    const res: any = await authApi.getUsers()
    users.value = res.data
  } catch (e) { /* ignore */ }

  // 编辑模式: 加载项目数据
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
      }
    } catch (e) {
      ElMessage.error('加载项目信息失败')
    }
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

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
  } catch (e) {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>
