<script setup>
import { ref, computed } from 'vue'
import { Rank, ArrowUp, ArrowDown, Close, Delete } from '@element-plus/icons-vue'
import { formatSize, extOf } from '../utils'

const props = defineProps({
  items: { type: Array, required: true },   // [{ id, file }]
  sortable: Boolean,
  disabled: Boolean
})
const emit = defineEmits(['remove', 'move', 'reorder', 'clear'])

const canDrag = computed(() => props.sortable && !props.disabled && props.items.length > 1)
const totalSize = computed(() => props.items.reduce((s, it) => s + it.file.size, 0))
const from = ref(null)
const over = ref(null)

const IMG = ['.jpg', '.jpeg', '.png', '.gif', '.bmp']
function typeOf(name) {
  const e = extOf(name)
  if (e === '.pdf') return { type: 'danger', label: 'PDF' }
  if (e === '.doc' || e === '.docx') return { type: 'primary', label: e.slice(1).toUpperCase() }
  if (IMG.includes(e)) return { type: 'success', label: e.slice(1).toUpperCase() }
  return { type: 'info', label: e ? e.slice(1).toUpperCase() : '文件' }
}

function onStart(i, e) {
  if (!canDrag.value) return
  from.value = i
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(i)) // Firefox 需要设置数据才会触发拖动
}
function onOver(i, e) {
  if (from.value === null) return
  e.dataTransfer.dropEffect = 'move'
  over.value = i
}
function onDrop(i) {
  if (from.value !== null && from.value !== i) emit('reorder', from.value, i)
  reset()
}
function reset() { from.value = null; over.value = null }

/** 拖到目标上方（向上移动）或下方（向下移动）时画一条指示线 */
function lineClass(i) {
  if (from.value === null || over.value !== i || from.value === i) return ''
  return from.value > i ? 'line-top' : 'line-bottom'
}
</script>

<template>
  <div class="fl" :class="{ sortable }">
    <div class="fl-bar">
      <span class="fl-sum tnum">
        文件列表 <b>{{ items.length }}</b> 个 · 合计 {{ formatSize(totalSize) }}
        <el-tag v-if="canDrag" size="small" type="info" effect="plain">可拖动排序</el-tag>
      </span>
      <el-button link type="danger" :icon="Delete" :disabled="disabled" @click="emit('clear')">一键清除所有文件</el-button>
    </div>

    <div class="row head">
      <span v-if="sortable"></span>
      <span>#</span>
      <span>文件名</span>
      <span class="col-type">类型</span>
      <span class="r">大小</span>
      <span class="r">操作</span>
    </div>
    <ul class="rows">
      <li v-for="(it, i) in items" :key="it.id" class="row" :draggable="canDrag"
          :class="[{ dragging: from === i, draggable: canDrag }, lineClass(i)]"
          @dragstart="onStart(i, $event)" @dragover.prevent="onOver(i, $event)"
          @drop.prevent="onDrop(i)" @dragend="reset">
        <span v-if="sortable" class="grip" :class="{ off: !canDrag }"><el-icon><Rank /></el-icon></span>
        <span class="no tnum">{{ i + 1 }}</span>
        <span class="name" :title="it.file.name">{{ it.file.name }}</span>
        <span class="col-type"><el-tag size="small" :type="typeOf(it.file.name).type" effect="plain">{{ typeOf(it.file.name).label }}</el-tag></span>
        <span class="r sz tnum">{{ formatSize(it.file.size) }}</span>
        <span class="r act">
          <template v-if="sortable">
            <el-button link :icon="ArrowUp" :disabled="i === 0 || disabled" @click="emit('move', i, -1)" />
            <el-button link :icon="ArrowDown" :disabled="i === items.length - 1 || disabled" @click="emit('move', i, 1)" />
          </template>
          <el-button link type="danger" :icon="Close" :disabled="disabled" @click="emit('remove', i)" />
        </span>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.fl { margin-top: 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; overflow: hidden; }
.fl-bar { display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap; padding: 8px 14px;
  background: var(--el-fill-color-light); font-size: 13px; color: var(--el-text-color-regular); }
.fl-sum { display: inline-flex; align-items: center; gap: 6px; }
.row { display: grid; grid-template-columns: 32px minmax(0, 1fr) 62px 82px 40px; align-items: center; gap: 8px;
  padding: 8px 14px; border-top: 1px solid var(--el-border-color-extra-light); background: var(--el-bg-color); font-size: 13px; }
.fl.sortable .row { grid-template-columns: 20px 28px minmax(0, 1fr) 62px 82px 88px; }
.row.head { padding: 7px 14px; font-size: 12px; color: var(--el-text-color-secondary); border-top: none;
  background: var(--el-fill-color-lighter); }
.rows { list-style: none; margin: 0; padding: 0; max-height: 360px; overflow-y: auto; }
.rows .row:first-child { border-top: none; }
.rows .row:hover { background: var(--el-fill-color-lighter); }
.row.draggable { cursor: grab; }
.row.dragging { opacity: .4; }
.row.line-top { box-shadow: inset 0 2px 0 var(--el-color-primary); }
.row.line-bottom { box-shadow: inset 0 -2px 0 var(--el-color-primary); }
.grip { color: var(--el-text-color-placeholder); display: flex; }
.grip.off { opacity: .35; }
.no { color: var(--el-text-color-secondary); }
.name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--el-text-color-primary); }
.r { text-align: right; justify-self: end; }
.sz { color: var(--el-text-color-secondary); }
.act { display: flex; gap: 2px; }
.act :deep(.el-button + .el-button) { margin-left: 0; }
@media (max-width: 640px) {
  .col-type { display: none; }
  .row { grid-template-columns: 26px minmax(0, 1fr) 68px 34px; padding: 8px 10px; gap: 6px; }
  .fl.sortable .row { grid-template-columns: 18px 24px minmax(0, 1fr) 68px 80px; }
}
</style>
