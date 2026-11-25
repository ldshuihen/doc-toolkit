<script setup>
import { computed } from 'vue'

const props = defineProps({
  opt: { type: Object, required: true },
  items: { type: Array, default: () => [] }
})

/** 与后端 RenameService 保持一致的命名计算，用于实时预览前 3 个文件 */
const previews = computed(() => {
  const o = props.opt
  const source = props.items.length
    ? props.items.slice(0, 3).map(it => it.file.name)
    : ['示例文档.pdf', '项目截图.png']
  const start = o.startNo ?? 1
  const digits = Math.max(1, o.digits ?? 3)
  return source.map((name, idx) => {
    const dot = name.lastIndexOf('.')
    let base = dot > 0 ? name.slice(0, dot) : name
    const ext = dot > 0 ? name.slice(dot) : ''
    if (o.find) base = base.split(o.find).join(o.replace || '')
    const seq = o.useSeq ? String(start + idx).padStart(digits, '0') : ''
    const next = ((o.prefix || '') + base + (o.suffix || '') + seq + ext).replace(/[\\/:*?"<>|]/g, '_')
    return { before: name, after: next }
  })
})
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-row :gutter="12">
      <el-col :span="12">
        <el-form-item label="前缀">
          <el-input v-model="opt.prefix" placeholder="可选" clearable />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="后缀">
          <el-input v-model="opt.suffix" placeholder="可选" clearable />
        </el-form-item>
      </el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-form-item label="查找文字">
          <el-input v-model="opt.find" placeholder="文件名中的内容" clearable />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="替换为">
          <el-input v-model="opt.replace" placeholder="留空表示删除" clearable />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="追加序号">
      <el-switch v-model="opt.useSeq" active-text="按列表顺序在末尾追加编号" />
    </el-form-item>

    <el-row v-if="opt.useSeq" :gutter="12">
      <el-col :span="12">
        <el-form-item label="起始编号">
          <el-input-number v-model="opt.startNo" :min="0" controls-position="right" class="full" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="编号位数">
          <el-input-number v-model="opt.digits" :min="1" :max="8" controls-position="right" class="full" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item :label="items.length ? '重命名预览（前 3 项）' : '重命名预览（示例）'">
      <div class="pv">
        <div v-for="(p, i) in previews" :key="i" class="pv-row">
          <span class="old" :title="p.before">{{ p.before }}</span>
          <span class="arr">→</span>
          <span class="new" :title="p.after">{{ p.after }}</span>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.pv { width: 100%; padding: 8px 10px; border-radius: 8px; background: var(--el-fill-color-light); font-size: 12px; }
.pv-row { display: flex; align-items: center; gap: 6px; padding: 3px 0; }
.old { color: var(--el-text-color-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 44%; }
.arr { color: var(--el-text-color-placeholder); flex-shrink: 0; }
.new { color: var(--el-color-primary); font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
</style>
