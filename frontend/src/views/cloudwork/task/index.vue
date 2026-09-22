<template>
  <div class="app-container tasks-page">
    <div class="page-header">
      <div><h1>Tasks</h1><p>{{ projectName ? `${projectName} · ` : '' }}Project #{{ projectId }}</p></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">Create Task</el-button>
    </div>
    <el-alert v-if="!tenantId" title="Workspace context is missing." type="warning" show-icon :closable="false" />
    <el-row v-else :gutter="16" v-loading="loading">
      <el-col v-for="column in columns" :key="column.status" :xs="24" :md="8">
        <section class="task-column">
          <div class="column-header"><h2>{{ column.label }}</h2><el-tag size="small" effect="plain">{{ tasksByStatus(column.status).length }}</el-tag></div>
          <el-card v-for="task in tasksByStatus(column.status)" :key="task.taskId" class="task-card" shadow="hover">
            <h3>{{ task.title }}</h3>
            <p v-if="task.description">{{ task.description }}</p>
            <div class="task-meta"><el-tag size="small" :type="priorityType(task.priority)">{{ task.priority }}</el-tag><span>{{ task.status }}</span></div>
            <el-button v-if="task.status === 'TODO'" class="status-action" size="small" type="primary" plain @click="moveTask(task, 'DOING')">Start</el-button>
            <el-button v-else-if="task.status === 'DOING'" class="status-action" size="small" type="success" plain @click="moveTask(task, 'DONE')">Complete</el-button>
          </el-card>
          <el-empty v-if="!tasksByStatus(column.status).length" :image-size="60" description="No tasks" />
        </section>
      </el-col>
    </el-row>

    <el-dialog v-model="createVisible" title="Create Task" width="500px" @closed="resetCreate">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="Title" prop="title"><el-input v-model="form.title" maxlength="200" placeholder="Enter task title" /></el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" maxlength="1000" show-word-limit :rows="4" /></el-form-item>
        <el-form-item label="Priority"><el-select v-model="form.priority" class="full-width"><el-option v-for="priority in priorities" :key="priority" :label="priority" :value="priority" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">Cancel</el-button><el-button type="primary" :loading="creating" @click="submitCreate">Create</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="CloudWorkTasks">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createTask, getProject, listTasks, updateTaskStatus } from '@/api/cloudwork/project'

const route = useRoute()
const projectId = Number(route.params.projectId)
const tenantId = Number(route.query.tenantId)
const tasks = ref([])
const projectName = ref('')
const loading = ref(false)
const creating = ref(false)
const createVisible = ref(false)
const formRef = ref()
const form = ref({ title: '', description: '', priority: 'MEDIUM' })
const priorities = ['LOW', 'MEDIUM', 'HIGH']
const columns = [{ status: 'TODO', label: 'TODO' }, { status: 'DOING', label: 'DOING' }, { status: 'DONE', label: 'DONE' }]
const rules = { title: [{ required: true, message: 'Title is required', trigger: 'blur' }] }
const grouped = computed(() => ({ TODO: tasks.value.filter(item => item.status === 'TODO'), DOING: tasks.value.filter(item => item.status === 'DOING'), DONE: tasks.value.filter(item => item.status === 'DONE') }))
function tasksByStatus(status) { return grouped.value[status] || [] }
async function loadTasks() { loading.value = true; try { const response = await listTasks(tenantId, projectId); tasks.value = response.data || [] } finally { loading.value = false } }
async function loadProject() { try { const response = await getProject(projectId, tenantId); projectName.value = response.data?.projectName || '' } catch (error) { /* request interceptor reports the error */ } }
function openCreate() { createVisible.value = true }
function resetCreate() { form.value = { title: '', description: '', priority: 'MEDIUM' }; formRef.value?.clearValidate() }
async function submitCreate() { if (!await formRef.value.validate()) return; creating.value = true; try { await createTask({ ...form.value, tenantId, projectId }); ElMessage.success('Task created'); createVisible.value = false; await loadTasks() } finally { creating.value = false } }
async function moveTask(task, status) { await updateTaskStatus(task.taskId, { tenantId, status }); ElMessage.success(`Task moved to ${status}`); await loadTasks() }
function priorityType(priority) { return priority === 'HIGH' ? 'danger' : priority === 'LOW' ? 'info' : 'warning' }
onMounted(async () => { await Promise.all([loadTasks(), loadProject()]) })
</script>

<style scoped lang="scss">
.tasks-page { min-height: calc(100vh - 84px); background: #f5f7fb; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 22px; }
.page-header h1 { margin: 0 0 8px; color: #1f2d3d; font-size: 28px; }
.page-header p { margin: 0; color: #7a8795; }
.task-column { min-height: 430px; padding: 14px; background: #eef2f7; border-radius: 8px; }
.column-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.column-header h2 { margin: 0; color: #303133; font-size: 16px; }
.task-card { margin-bottom: 12px; }
.task-card h3 { margin: 0 0 8px; color: #303133; font-size: 16px; }
.task-card p { margin: 0 0 12px; color: #7a8795; line-height: 1.5; white-space: pre-wrap; }
.task-meta { display: flex; align-items: center; justify-content: space-between; color: #909399; font-size: 12px; }
.status-action { width: 100%; margin-top: 12px; }
.full-width { width: 100%; }
</style>
