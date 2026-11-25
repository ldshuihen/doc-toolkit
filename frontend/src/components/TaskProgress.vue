<script setup>
import { computed } from 'vue'

const props = defineProps({
  prog: { type: Object, required: true },   // { phase, percent, label, detail }
  running: Boolean
})
const step = computed(() => ({ upload: 0, process: 1, download: 2, done: 3 }[props.prog.phase] ?? 0))
</script>

<template>
  <div class="tp">
    <el-steps :active="step" finish-status="success" align-center class="steps">
      <el-step title="上传" />
      <el-step title="处理" />
      <el-step title="下载" />
    </el-steps>
    <el-progress :percentage="prog.percent" :stroke-width="14" :text-inside="true"
                 :status="prog.phase === 'done' ? 'success' : ''" :striped="running" :striped-flow="running" />
    <div class="line tnum">
      <b>{{ prog.label }}</b>
      <span class="detail" :title="prog.detail">{{ prog.detail }}</span>
    </div>
  </div>
</template>

<style scoped>
.tp { margin-top: 14px; }
.steps { margin-bottom: 14px; }
.line { display: flex; gap: 10px; margin-top: 8px; font-size: 13px; color: var(--el-text-color-regular); }
.detail { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  color: var(--el-text-color-secondary); }
</style>

