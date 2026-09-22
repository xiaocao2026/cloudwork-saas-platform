<template>
  <div class="app-container projects-page">
    <div class="page-header">
      <div><h1>Projects</h1><p>Organize delivery work inside the selected Workspace.</p></div>
      <div class="header-actions">
        <el-select v-model="selectedTenantId" placeholder="Select Workspace" value-key="tenantId" class="workspace-select" @change="loadProjects">
          <el-option v-for="workspace in workspaces" :key="workspace.tenantId" :label="workspace.tenantName" :value="workspace.tenantId" />
        </el-select>
        <el-button v-if="selectedTenantId" type="primary" :icon="Plus" @click="openCreate">Create Project</el-button>
      </div>
    </div>

    <el-card v-if="!loadingWorkspaces && !workspaces.length" class="empty-card" shadow="never">
      <el-empty description="Create a workspace first."><el-button type="primary" @click="router.push('/cloudwork/workspaces')">Go to Workspaces</el-button></el-empty>
    </el-card>
    <el-table v-else v-loading="loadingProjects" :data="projects" class="project-table">
      <el-table-column prop="projectName" label="Project Name" min-width="220" />
      <el-table-column prop="projectCode" label="Project Code" min-width="250" />
      <el-table-column prop="description" label="Description" min-width="280" show-overflow-tooltip />
      <el-table-column label="Status" width="130"><template #default="{ row }"><el-tag type="success">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="Action" width="150"><template #default="{ row }"><el-button link type="primary" @click="openTasks(row)">Open Tasks</el-button></template></el-table-column>
      <template #empty><el-empty description="No projects in this Workspace yet." /></template>
    </el-table>

    <el-dialog v-model="createVisible" title="Create Project" width="500px" @closed="resetCreate">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="Project Name" prop="projectName"><el-input v-model="form.projectName" maxlength="100" placeholder="Enter project name" /></el-form-item>
        <el-form-item label="Description" prop="description"><el-input v-model="form.description" type="textarea" maxlength="500" show-word-limit :rows="4" placeholder="Describe this project" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">Cancel</el-button><el-button type="primary" :loading="creating" @click="submitCreate">Create</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="CloudWorkProjects">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createProject, listProjects } from '@/api/cloudwork/project'
import { listMyWorkspaces } from '@/api/cloudwork/tenant'

const route = useRoute()
const router = useRouter()
const workspaces = ref([])
const projects = ref([])
const selectedTenantId = ref(null)
const loadingWorkspaces = ref(false)
const loadingProjects = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const formRef = ref()
const form = ref({ projectName: '', description: '' })
const rules = { projectName: [{ required: true, message: 'Project Name is required', trigger: 'blur' }] }

async function loadWorkspaces() {
  loadingWorkspaces.value = true
  try {
    const response = await listMyWorkspaces()
    workspaces.value = response.data || []
    const requested = Number(route.query.tenantId)
    selectedTenantId.value = workspaces.value.some(item => item.tenantId === requested) ? requested : workspaces.value[0]?.tenantId || null
    if (selectedTenantId.value) await loadProjects()
  } finally { loadingWorkspaces.value = false }
}
async function loadProjects() {
  if (!selectedTenantId.value) { projects.value = []; return }
  loadingProjects.value = true
  try { const response = await listProjects(selectedTenantId.value); projects.value = response.data || [] } finally { loadingProjects.value = false }
}
function openCreate() { createVisible.value = true }
function resetCreate() { form.value = { projectName: '', description: '' }; formRef.value?.clearValidate() }
async function submitCreate() {
  if (!await formRef.value.validate()) return
  creating.value = true
  try { await createProject({ ...form.value, tenantId: selectedTenantId.value }); ElMessage.success('Project created'); createVisible.value = false; await loadProjects() } finally { creating.value = false }
}
function openTasks(project) { router.push({ path: `/cloudwork/projects/${project.projectId}/tasks`, query: { tenantId: selectedTenantId.value } }) }
function statusLabel(status) { return status === 0 || status === '0' ? 'Active' : 'Archived' }
onMounted(loadWorkspaces)
</script>

<style scoped lang="scss">
.projects-page { min-height: calc(100vh - 84px); background: #f5f7fb; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.page-header h1 { margin: 0 0 8px; color: #1f2d3d; font-size: 28px; }
.page-header p { margin: 0; color: #7a8795; }
.header-actions { display: flex; align-items: center; gap: 12px; }
.workspace-select { width: 220px; }
.project-table { border-radius: 6px; }
.empty-card { min-height: 320px; }
@media (max-width: 760px) { .page-header, .header-actions { align-items: flex-start; flex-direction: column; } .workspace-select { width: 100%; } }
</style>
