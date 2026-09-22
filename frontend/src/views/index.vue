<template>
  <div class="app-container cloudwork-home">
    <section class="hero">
      <p class="eyebrow">CLOUDWORK</p>
      <h1>CloudWork</h1>
      <p class="subtitle">Enterprise SaaS Collaboration Platform</p>
      <p class="intro">Bring your teams, workspaces and delivery plans together in one focused workspace.</p>
    </section>
    <el-row :gutter="20" class="capability-grid">
      <el-col v-for="capability in capabilities" :key="capability.title" :xs="24" :sm="12" :md="8">
        <el-card class="capability-card" shadow="hover">
          <div class="capability-icon" :class="capability.className"><component :is="capability.icon" /></div>
          <h2>{{ capability.title }}</h2>
          <p>{{ capability.description }}</p>
          <el-button v-if="capability.available" type="primary" link @click="router.push('/cloudwork/workspaces')">
            Open Workspaces <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-tag v-else type="info" effect="plain">Coming Next</el-tag>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import { useRouter } from 'vue-router'
import { OfficeBuilding, Folder, List, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const capabilities = [
  { title: 'Workspace', description: 'Create and organize the collaboration spaces your teams need.', available: true, icon: OfficeBuilding, className: 'workspace' },
  { title: 'Projects', description: 'Plan initiatives and keep delivery work visible to everyone.', available: false, icon: Folder, className: 'projects' },
  { title: 'Tasks', description: 'Turn plans into clear, accountable work for every team.', available: false, icon: List, className: 'tasks' }
]
</script>

<style scoped lang="scss">
.cloudwork-home { min-height: calc(100vh - 84px); background: #f5f7fb; }
.hero { padding: 34px 8px 28px; }
.eyebrow { margin: 0 0 10px; color: #409eff; font-size: 12px; font-weight: 700; letter-spacing: .18em; }
.hero h1 { margin: 0; color: #1f2d3d; font-size: 38px; line-height: 1.2; }
.subtitle { margin: 8px 0 0; color: #5c6b7a; font-size: 18px; }
.intro { max-width: 620px; margin: 16px 0 0; color: #7a8795; line-height: 1.7; }
.capability-grid { margin-top: 4px; }
.capability-card { min-height: 220px; margin-bottom: 20px; border: 0; }
.capability-icon { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; margin-bottom: 16px; border-radius: 12px; font-size: 23px; }
.capability-icon.workspace { color: #409eff; background: #ecf5ff; }
.capability-icon.projects { color: #67c23a; background: #f0f9eb; }
.capability-icon.tasks { color: #e6a23c; background: #fdf6ec; }
.capability-card h2 { margin: 0 0 8px; color: #303133; font-size: 20px; }
.capability-card p { min-height: 44px; margin: 0 0 18px; color: #7a8795; line-height: 1.6; }
.el-icon { margin-left: 4px; vertical-align: middle; }
</style>
