<template>
  <div class="app-container workspace-page">
    <div class="page-header">
      <div><h1>Workspaces</h1><p>Manage the collaboration spaces available to your account.</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">Create Workspace</el-button>
    </div>

    <el-table v-loading="loading" :data="workspaces" class="workspace-table" @row-click="openDetail">
      <el-table-column prop="tenantName" label="Workspace Name" min-width="220" />
      <el-table-column prop="tenantCode" label="Workspace Code" min-width="260" />
      <el-table-column prop="roleCode" label="Role" width="150" />
      <el-table-column label="Status" width="140"><template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="Action" width="120"><template #default="{ row }"><el-button link type="primary" @click.stop="openDetail(row)">View</el-button></template></el-table-column>
      <template #empty><el-empty description="No workspaces yet. Create one to get started." /></template>
    </el-table>

    <el-dialog v-model="createVisible" title="Create Workspace" width="460px" @closed="resetCreate">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="130px">
        <el-form-item label="Workspace name" prop="tenantName"><el-input v-model="createForm.tenantName" maxlength="100" show-word-limit placeholder="Enter a workspace name" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">Cancel</el-button><el-button type="primary" :loading="creating" @click="submitCreate">Create</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="Workspace Details" width="460px">
      <el-skeleton v-if="detailLoading" :rows="4" animated />
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="Workspace Name">{{ detail.tenantName }}</el-descriptions-item>
        <el-descriptions-item label="Workspace Code">{{ detail.tenantCode }}</el-descriptions-item>
        <el-descriptions-item label="Role">{{ detail.roleCode }}</el-descriptions-item>
        <el-descriptions-item label="Status"><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup name="CloudWorkWorkspace">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createWorkspace, getWorkspace, listMyWorkspaces } from '@/api/cloudwork/tenant'

const loading = ref(false)
const creating = ref(false)
const workspaces = ref([])
const createVisible = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const createFormRef = ref()
const createForm = ref({ tenantName: '' })
const detail = ref({})
const createRules = { tenantName: [{ required: true, message: 'Workspace name is required', trigger: 'blur' }, { min: 2, max: 100, message: 'Use 2 to 100 characters', trigger: 'blur' }] }

async function loadWorkspaces() {
  loading.value = true
  try { const response = await listMyWorkspaces(); workspaces.value = response.data || [] } finally { loading.value = false }
}

function openCreate() { createVisible.value = true }
function resetCreate() { createForm.value = { tenantName: '' }; createFormRef.value?.clearValidate() }

async function submitCreate() {
  if (!await createFormRef.value.validate()) return
  creating.value = true
  try { await createWorkspace(createForm.value); ElMessage.success('Workspace created'); createVisible.value = false; await loadWorkspaces() } finally { creating.value = false }
}

async function openDetail(row) {
  detailVisible.value = true; detailLoading.value = true
  try { const response = await getWorkspace(row.tenantId); detail.value = response.data || {} } finally { detailLoading.value = false }
}

function statusLabel(status) { return status === 0 || status === '0' ? 'Active' : status === 1 || status === '1' ? 'Inactive' : String(status ?? '-') }
function statusType(status) { return status === 0 || status === '0' ? 'success' : 'info' }
onMounted(loadWorkspaces)
</script>

<style scoped lang="scss">
.workspace-page { min-height: calc(100vh - 84px); background: #f5f7fb; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.page-header h1 { margin: 0 0 8px; color: #1f2d3d; font-size: 28px; }
.page-header p { margin: 0; color: #7a8795; }
.workspace-table { border-radius: 6px; }
</style>
