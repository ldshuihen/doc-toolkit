<script setup>
import { computed } from 'vue'
import { FolderOpened, Setting, VideoPlay, Delete, Refresh, InfoFilled } from '@element-plus/icons-vue'
import { toolMap, groupMap } from '../tools'
import { getWorkspace } from '../workspace'
import { formatSize } from '../utils'
import ToolTile from '../components/ToolTile.vue'
import FileDropzone from '../components/FileDropzone.vue'
import FileList from '../components/FileList.vue'
import TaskProgress from '../components/TaskProgress.vue'
import ResultPanel from '../components/ResultPanel.vue'

const props = defineProps({ toolKey: { type: String, required: true } })

const tool = toolMap[props.toolKey]
const color = groupMap[tool.group].color
const ws = getWorkspace(tool)
const exts = tool.accept === '*' ? ['任意格式'] : tool.accept.split(',')
const totalSize = computed(() => ws.items.reduce((s, it) => s + it.file.size, 0))
</script>

<template>
  <div class="page">
    <div class="page-head">
      <ToolTile :icon="tool.icon" :color="color" size="lg" />
      <div class="ph-text">
        <h2>{{ tool.label }}</h2>
        <p>{{ tool.desc }}</p>
        <div class="ph-tags">
          <el-tag size="small" effect="plain">{{ tool.single ? '单文件' : '支持批量' }}</el-tag>
          <el-tag v-if="tool.sortable" size="small" type="warning" effect="plain">顺序敏感 · 可拖动排序</el-tag>
          <el-tag v-for="e in exts" :key="e" size="small" type="info" effect="plain">{{ e }}</el-tag>
        </div>
      </div>
    </div>

    <el-row :gutter="18" class="workspace">
      <el-col :xs="24" :lg="15">
        <el-card shadow="never" class="panel">
          <template #header>
            <div class="panel-head">
              <span class="panel-title"><el-icon><FolderOpened /></el-icon>文件工作区</span>
              <span v-if="ws.items.length" class="head-meta tnum">
                {{ ws.items.length }} 个文件 · {{ formatSize(totalSize) }}
              </span>
            </div>
          </template>
          <FileDropzone :single="tool.single" :accept="tool.accept" :disabled="ws.loading" @files="ws.addFiles" />
          <FileList v-if="ws.items.length" :items="ws.items" :sortable="tool.sortable" :disabled="ws.loading"
                    @remove="ws.remove" @move="ws.move" @reorder="ws.reorder" @clear="ws.clear" />
        </el-card>

        <div v-if="tool.tips?.length" class="tips-strip">
          <el-icon class="tips-ico"><InfoFilled /></el-icon>
          <div class="tips-body">
            <div class="tips-title">使用说明</div>
            <ul>
              <li v-for="t in tool.tips" :key="t">{{ t }}</li>
            </ul>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :lg="9">
        <el-card shadow="never" class="panel inspector">
          <template #header>
            <div class="panel-head">
              <span class="panel-title">
                <el-icon><component :is="tool.options ? Setting : VideoPlay" /></el-icon>
                {{ tool.options ? '参数与执行' : '任务执行' }}
              </span>
              <el-button v-if="tool.options" link size="small" :icon="Refresh" :disabled="ws.loading"
                         @click="ws.resetOpts">重置默认</el-button>
            </div>
          </template>

          <div v-if="tool.options" class="sec-opts">
            <component :is="tool.options" :opt="ws.opts" :items="ws.items" />
          </div>

          <div class="sec-exec" :class="{ divided: !!tool.options }">
            <div class="exec-meta tnum">
              <span>待处理 <b>{{ ws.items.length }}</b> 个文件</span>
              <span class="sep">·</span>
              <span>合计 <b>{{ ws.items.length ? formatSize(totalSize) : '0 KB' }}</b></span>
            </div>

            <el-alert v-if="ws.error" type="error" :title="ws.error" show-icon closable class="err"
                      @close="ws.error = ''" />
            <TaskProgress v-if="ws.prog.show" :prog="ws.prog" :running="ws.loading" />
            <ResultPanel v-if="ws.result" :result="ws.result" @download="ws.download" />

            <div class="actions">
              <el-button type="primary" size="large" :icon="VideoPlay" :loading="ws.loading"
                         :disabled="!ws.items.length" @click="ws.submit">
                {{ ws.loading ? '处理中...' : '开始处理' }}
              </el-button>
              <el-button size="large" :icon="Delete" :disabled="ws.loading || (!ws.items.length && !ws.result)"
                         @click="ws.clear">清空</el-button>
            </div>
            <el-switch v-model="ws.autoClear" size="small" active-text="处理完成后自动清空文件列表" class="auto" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-head { display: flex; align-items: center; gap: 16px; margin-bottom: 18px; }
.ph-text h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.ph-text p { margin: 0 0 8px; color: var(--el-text-color-secondary); font-size: 14px; }
.ph-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.panel-head { display: flex; justify-content: space-between; align-items: center; gap: 10px; }
.head-meta { font-size: 12px; color: var(--el-text-color-secondary); }

.inspector { position: sticky; top: 0; }
.sec-exec.divided { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--el-border-color-lighter); }
.exec-meta { display: flex; align-items: center; gap: 6px; padding: 9px 12px; border-radius: 8px;
  background: var(--el-fill-color-light); font-size: 13px; color: var(--el-text-color-secondary); }
.exec-meta b { color: var(--el-text-color-primary); font-weight: 600; }
.exec-meta .sep { color: var(--el-border-color-darker); }
.err { margin-top: 12px; }
.actions { display: flex; gap: 10px; margin-top: 14px; }
.actions .el-button--primary { flex: 1; }
.auto { margin-top: 12px; }

.tips-strip { display: flex; align-items: flex-start; gap: 10px; padding: 12px 16px; margin-bottom: 16px;
  border-radius: 10px; background: var(--el-fill-color-lighter); border: 1px solid var(--el-border-color-lighter); }
.tips-ico { font-size: 16px; color: var(--el-color-primary); margin-top: 2px; flex-shrink: 0; }
.tips-title { font-size: 13px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 4px; }
.tips-body ul { margin: 0; padding-left: 16px; font-size: 12.5px; line-height: 1.8; color: var(--el-text-color-secondary); }
@media (max-width: 1199px) { .inspector { position: static; } }
</style>
