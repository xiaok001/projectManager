<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 用户管理</span>
          <el-button type="primary" @click="openDialog(null)">
            <el-icon><Plus /></el-icon> 新增用户
          </el-button>
        </div>
      </template>

      <el-table :data="users" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'DEPT_MANAGER' ? 'danger' : 'primary'" size="small">
              {{ getRoleName(row.roleId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm
              v-if="row.id !== currentUserId"
              title="确认禁用此用户？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link size="small">禁用</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item :label="isEdit ? '新密码' : '密码'" :prop="isEdit ? '' : 'password'">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空则不修改密码' : '请输入密码'"
          />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roleList" :key="r.id" :value="r.id" :label="r.roleName" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { authApi } from '../../api'
import { useUserStore } from '../../store/user'
import api from '../../api'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.userId)

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const users = ref<any[]>([])
const roleList = ref<any[]>([])
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const isEdit = computed(() => editingId.value !== null)

const form = ref({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  role: 'PM',
  roleId: null as number | null,
  status: 1,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function getRoleName(roleId: number | null) {
  if (!roleId) return '-'
  const r = roleList.value.find(r => r.id === roleId)
  return r ? r.roleName : '-'
}

onMounted(async () => {
  loadUsers()
  // 加载角色列表
  try {
    const res: any = await api.get('/roles')
    roleList.value = res.data || []
  } catch { /* ignore */ }
})

async function loadUsers() {
  loading.value = true
  try {
    const res: any = await authApi.getUsers()
    users.value = res.data
  } catch (e) { /* ignore */ }
  loading.value = false
}

function openDialog(row: any | null) {
  if (row) {
    editingId.value = row.id
    form.value = {
      username: row.username,
      password: '',
      realName: row.realName,
      email: row.email || '',
      phone: row.phone || '',
      role: row.role,
      roleId: row.roleId || null,
      status: row.status,
    }
  } else {
    editingId.value = null
    form.value = {
      username: '',
      password: '',
      realName: '',
      email: '',
      phone: '',
      role: 'PM',
      roleId: null,
      status: 1,
    }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      const payload: any = { ...form.value }
      if (!payload.password) delete payload.password
      await api.put(`/users/${editingId.value}`, payload)
      ElMessage.success('用户更新成功')
    } else {
      await api.post('/users', form.value)
      ElMessage.success('用户创建成功')
    }
    dialogVisible.value = false
    loadUsers()
  } catch (e) { /* handled by interceptor */ }
  submitting.value = false
}

async function handleDelete(id: number) {
  try {
    await api.delete(`/users/${id}`)
    ElMessage.success('用户已禁用')
    loadUsers()
  } catch (e) { /* ignore */ }
}
</script>
