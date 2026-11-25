<script setup>
import { CircleCheckFilled, Download } from '@element-plus/icons-vue'
import { formatSize, formatDuration } from '../utils'

defineProps({ result: { type: Object, required: true } })
const emit = defineEmits(['download'])
</script>

<template>
  <div class="rs">
    <el-icon class="ok" :size="30"><CircleCheckFilled /></el-icon>
    <div class="info">
      <div class="name" :title="result.filename">{{ result.filename }}</div>
      <div class="meta tnum">
        {{ formatSize(result.size) }} · 耗时 {{ formatDuration(result.durationMs) }} · 共处理 {{ result.count }} 个文件
      </div>
      <div v-if="result.extra" class="extra">{{ result.extra }}</div>
    </div>
    <el-button type="primary" plain :icon="Download" @click="emit('download')">重新下载</el-button>
  </div>
</template>

<style scoped>
.rs { display: flex; align-items: center; gap: 12px; margin-top: 14px; padding: 12px 14px; border-radius: 10px;
  background: var(--el-color-success-light-9); border: 1px solid var(--el-color-success-light-7); }
.ok { color: var(--el-color-success); flex-shrink: 0; }
.info { flex: 1; min-width: 0; }
.name { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.meta { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 2px; }
.extra { font-size: 12px; color: var(--el-color-warning-dark-2); margin-top: 2px; }
</style>

