<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span><el-icon><User /></el-icon> 角色管理</span>
          <el-button type="primary" @click="openDialog(null)">新增角色</el-button>
        </div>
      </template>

      <el-table :data="roles" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="roleName" label="角色名称" width="150" />
        <el-table-column prop="roleKey" label="角色标识" width="140">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.roleKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openDialog(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="openPermDialog(row)">分配权限</el-button>
            <el-popconfirm title="确认删除此角色？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button type="danger" link size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="如: 项目经理" />
        </el-form-item>
        <el-form-item label="角色标识" prop="roleKey">
          <el-input v-model="form.roleKey" placeholder="如: pm (英文标识)" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permDialogVisible" :title="`分配权限 - ${currentRole?.roleName || ''}`" width="520px" destroy-on-close>
      <div v-loading="permLoading" class="perm-tree-wrap">
        <el-tree
          ref="treeRef"
          :data="permTree"
          show-checkbox
          node-key="id"
          :default-checked-keys="checkedKeys"
          :props="{ label: 'permName', children: 'children' }"
          default-expand-all
        >
          <template #default="{ data }">
            <div class="perm-node">
              <span>{{ data.permName }}</span>
              <el-tag v-if="data.type === 'menu'" size="small" type="primary" style="margin-left:8px">页面</el-tag>
              <el-tag v-else size="small" type="warning" style="margin-left:8px">按钮</el-tag>
            </div>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingPerm" @click="handleSavePerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import api from '../../api'

const loading = ref(false)
const submitting = ref(false)
const roles = ref<any[]>([])

// 角色表单
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({ roleName: '', roleKey: '', sortOrder: 0, status: 1, remark: '' })
const rules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleKey: [{ required: true, message: '请输入角色标识', trigger: 'blur' }],
}

// 权限分配
const permDialogVisible = ref(false)
const permLoading = ref(false)
const savingPerm = ref(false)
const permTree = ref<any[]>([])
const checkedKeys = ref<number[]>([])
const currentRole = ref<any>(null)
const treeRef = ref()

onMounted(() => { loadRoles() })

async function loadRoles() {
  loading.value = true
  try {
    const res: any = await api.get('/roles')
    roles.value = res.data || []
  } catch { /* handled */ }
  loading.value = false
}

function openDialog(row: any | null) {
  if (row) {
    editingId.value = row.id
    isEdit.value = true
    Object.assign(form, { roleName: row.roleName, roleKey: row.roleKey, sortOrder: row.sortOrder, status: row.status, remark: row.remark || '' })
  } else {
    editingId.value = null
    isEdit.value = false
    Object.assign(form, { roleName: '', roleKey: '', sortOrder: 0, status: 1, remark: '' })
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await api.put(`/roles/${editingId.value}`, form)
      ElMessage.success('角色更新成功')
    } else {
      await api.post('/roles', form)
      ElMessage.success('角色创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch { /* handled */ }
  submitting.value = false
}

async function handleDelete(id: number) {
  try {
    await api.delete(`/roles/${id}`)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch { /* handled */ }
}

async function openPermDialog(row: any) {
  currentRole.value = row
  permDialogVisible.value = true
  permLoading.value = true
  try {
    const [treeRes, checkedRes]: any[] = await Promise.all([
      api.get('/permissions/tree'),
      api.get(`/roles/${row.id}/permissions`),
    ])
    permTree.value = treeRes.data || []
    checkedKeys.value = checkedRes.data || []
    await nextTick()
    treeRef.value?.setCheckedKeys(checkedKeys.value)
  } catch { /* handled */ }
  permLoading.value = false
}

async function handleSavePerm() {
  if (!currentRole.value) return
  savingPerm.value = true
  try {
    const checked = treeRef.value.getCheckedKeys(false)
    const halfChecked = treeRef.value.getHalfCheckedKeys()
    const allIds = [...checked, ...halfChecked]
    await api.put(`/roles/${currentRole.value.id}/permissions`, { permissionIds: allIds })
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
  } catch { /* handled */ }
  savingPerm.value = false
}
</script>

<style scoped>
.perm-tree-wrap {
  max-height: 420px;
  overflow-y: auto;
  padding: 8px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
}
.perm-node {
  display: flex;
  align-items: center;
}
</style>
