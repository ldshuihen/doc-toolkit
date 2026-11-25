<script setup>
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'

const props = defineProps({
  single: Boolean,
  accept: { type: String, default: '*' },
  disabled: Boolean
})
const emit = defineEmits(['files'])
const over = ref(false)

// 只有真正拖入“文件”时才高亮，避免列表内部拖动排序时误触发
const hasFiles = e => [...(e.dataTransfer?.types || [])].includes('Files')

function onOver(e) { if (hasFiles(e) && !props.disabled) over.value = true }
function onDrop(e) {
  over.value = false
  const fs = [...(e.dataTransfer?.files || [])]
  if (fs.length && !props.disabled) emit('files', fs)
}
function onPick(e) {
  emit('files', [...e.target.files])
  e.target.value = ''
}
</script>

<template>
  <label class="dz" :class="{ over, disabled }"
         @dragover.prevent="onOver" @dragleave="over = false" @drop.prevent="onDrop">
    <el-icon class="dz-icon"><UploadFilled /></el-icon>
    <div class="dz-title">将{{ single ? '一个' : '多个' }}文件拖到此处，或 <span class="dz-link">点击选择文件</span></div>
    <div class="dz-hint">
      <span>支持格式</span>
      <el-tag v-if="accept === '*'" size="small" type="info" effect="plain">任意格式</el-tag>
      <el-tag v-for="e in (accept === '*' ? [] : accept.split(','))" :key="e" size="small" type="info" effect="plain">{{ e }}</el-tag>
      <span class="sep">·</span>
      <span>单个 ≤ 200MB，总计 ≤ 600MB</span>
    </div>
    <input type="file" hidden :multiple="!single" :accept="accept === '*' ? undefined : accept"
           :disabled="disabled" @change="onPick" />
  </label>
</template>

<style scoped>
.dz { display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 26px 16px; cursor: pointer;
  border: 1.5px dashed var(--el-border-color); border-radius: 10px; background: var(--el-fill-color-lighter);
  transition: border-color .15s, background .15s; }
.dz:hover, .dz.over { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.dz.disabled { opacity: .55; cursor: not-allowed; }
.dz-icon { font-size: 44px; color: var(--el-color-primary); }
.dz-title { font-size: 15px; color: var(--el-text-color-primary); }
.dz-link { color: var(--el-color-primary); font-weight: 600; }
.dz-hint { display: flex; flex-wrap: wrap; justify-content: center; align-items: center; gap: 6px;
  font-size: 12px; color: var(--el-text-color-secondary); }
.sep { margin: 0 2px; }
</style>
