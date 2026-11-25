<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Clock, Tickets, Files, DataAnalysis, Finished } from '@element-plus/icons-vue'
import { tools, groupedTools, toolMap } from '../tools'
import { records, stats } from '../stores/history'
import { formatTime } from '../utils'
import ToolTile from '../components/ToolTile.vue'

const router = useRouter()
const go = key => router.push('/tool/' + key)
const recent = computed(() => records.value.slice(0, 6))
</script>

<template>
  <div class="page">
    <section class="hero">
      <div>
        <h1>欢迎使用文档工具箱</h1>
        <p>PDF、图片与 Office 文档的批量处理工具集。文件只发送给本机运行的后端服务处理，不会上传到第三方。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="go('merge')">
            开始使用 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
          </el-button>
          <el-button size="large" @click="router.push('/history')">查看任务记录</el-button>
        </div>
      </div>
      <el-icon class="hero-icon"><Files /></el-icon>
    </section>

    <el-row :gutter="16" class="stats">
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="stat">
          <el-statistic title="可用工具" :value="tools.length" />
          <el-icon class="si"><Tickets /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="stat">
          <el-statistic title="累计任务" :value="stats.total" />
          <el-icon class="si"><DataAnalysis /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="stat">
          <el-statistic title="已处理文件" :value="stats.files" />
          <el-icon class="si"><Files /></el-icon>
        </el-card>
      </el-col>
      <el-col :xs="12" :md="6">
        <el-card shadow="never" class="stat">
          <el-statistic title="任务成功率" :value="stats.rate ?? 0" :suffix="stats.rate === null ? '' : '%'">
            <template v-if="stats.rate === null" #formatter>—</template>
          </el-statistic>
          <el-icon class="si"><Finished /></el-icon>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :xl="17">
        <section v-for="g in groupedTools" :key="g.key" class="group">
          <h3><span class="bar" :style="{ background: g.color }"></span>{{ g.label }}</h3>
          <div class="grid">
            <el-card v-for="t in g.tools" :key="t.key" shadow="hover" class="tool" @click="go(t.key)">
              <ToolTile :icon="t.icon" :color="g.color" />
              <div class="tt">
                <div class="tn">{{ t.label }}</div>
                <div class="td">{{ t.desc }}</div>
              </div>
            </el-card>
          </div>
        </section>
      </el-col>

      <el-col :xs="24" :xl="7">
        <el-card shadow="never" class="recent">
          <template #header>
            <div class="rh">
              <span class="panel-title"><el-icon><Clock /></el-icon>最近任务</span>
              <el-button link type="primary" @click="router.push('/history')">全部</el-button>
            </div>
          </template>
          <el-empty v-if="!recent.length" description="还没有任务记录" :image-size="72" />
          <ul v-else class="rl">
            <li v-for="r in recent" :key="r.id">
              <el-tag size="small" :type="r.status === 'success' ? 'success' : 'danger'" effect="light">
                {{ r.status === 'success' ? '成功' : '失败' }}
              </el-tag>
              <a class="rn" @click="toolMap[r.toolKey] && go(r.toolKey)">{{ r.toolLabel }}</a>
              <span class="rc tnum">{{ r.fileCount }} 个文件</span>
              <span class="rt tnum">{{ formatTime(r.time).slice(5, 16) }}</span>
            </li>
          </ul>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.hero { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; padding: 28px 32px;
  border-radius: 14px; color: #fff; overflow: hidden; position: relative;
  background: linear-gradient(120deg, #1d4fd8 0%, #2f6fed 45%, #6a4cf5 100%); }
.hero h1 { margin: 0 0 8px; font-size: 24px; font-weight: 600; }
.hero p { margin: 0 0 18px; max-width: 560px; opacity: .9; line-height: 1.7; font-size: 14px; }
.hero-actions .el-button:not(.el-button--primary) { background: #ffffff26; border-color: #ffffff55; color: #fff; }
.hero-actions .el-button--primary { background: #fff; border-color: #fff; color: #2f6fed; }
.hero-icon { font-size: 120px; opacity: .15; margin-right: 12px; }
.stats { margin-bottom: 8px; }
.stat { margin-bottom: 16px; border-radius: 10px; position: relative; }
.stat .si { position: absolute; right: 18px; top: 50%; transform: translateY(-50%); font-size: 34px;
  color: var(--el-color-primary-light-5); }
.group { margin-bottom: 8px; }
.group h3 { display: flex; align-items: center; gap: 10px; margin: 6px 0 12px; font-size: 15px; font-weight: 600; }
.group .bar { width: 4px; height: 16px; border-radius: 2px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 14px; margin-bottom: 16px; }
.tool { cursor: pointer; border-radius: 10px; transition: transform .15s; }
.tool:hover { transform: translateY(-2px); }
.tool :deep(.el-card__body) { display: flex; align-items: center; gap: 14px; padding: 16px; }
.tt { min-width: 0; }
.tn { font-weight: 600; font-size: 15px; }
.td { margin-top: 3px; font-size: 12px; line-height: 1.5; color: var(--el-text-color-secondary);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.recent { border-radius: 10px; }
.rh { display: flex; justify-content: space-between; align-items: center; }
.rl { list-style: none; margin: 0; padding: 0; }
.rl li { display: flex; align-items: center; gap: 8px; padding: 9px 0; font-size: 13px;
  border-bottom: 1px solid var(--el-border-color-extra-light); }
.rl li:last-child { border-bottom: none; }
.rn { color: var(--el-color-primary); cursor: pointer; font-weight: 500; }
.rc { color: var(--el-text-color-secondary); }
.rt { margin-left: auto; color: var(--el-text-color-placeholder); font-size: 12px; }
@media (max-width: 760px) { .hero-icon { display: none; } }
</style>

