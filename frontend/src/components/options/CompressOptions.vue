<script setup>
defineProps({ opt: { type: Object, required: true } })

const levels = [
  { value: 'low', title: '轻度压缩', desc: '保留较高清晰度（质量 82%，上限 2400px），适合打印存档' },
  { value: 'medium', title: '均衡压缩（推荐）', desc: '清晰度与体积兼顾（质量 65%，上限 1600px）' },
  { value: 'high', title: '强力压缩', desc: '优先减小体积（质量 45%，上限 1200px），适合邮件/网页传输' }
]
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="压缩强度">
      <div class="levels">
        <div v-for="lv in levels" :key="lv.value" class="lv" :class="{ on: opt.level === lv.value }"
             @click="opt.level = lv.value">
          <div class="lv-t">{{ lv.title }}</div>
          <div class="lv-d">{{ lv.desc }}</div>
        </div>
      </div>
    </el-form-item>

    <el-alert type="warning" :closable="false" show-icon title="压缩原理与局限">
      <template #default>
        仅对 PDF 内嵌的<b>图片</b>重新编码并按需降采样，文字与矢量图形保持不变。
        扫描件、多图文档压缩效果明显；<b>纯文字 PDF</b>（如 Word 直接导出）基本无法再压小，仅当图片变小时才会替换。
      </template>
    </el-alert>
  </el-form>
</template>

<style scoped>
.levels { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.lv { padding: 10px 12px; border-radius: 8px; cursor: pointer; transition: .15s;
  border: 1px solid var(--el-border-color-light); background: var(--el-fill-color-blank); }
.lv:hover { border-color: var(--el-color-primary-light-5); }
.lv.on { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
.lv-t { font-size: 13.5px; font-weight: 600; color: var(--el-text-color-primary); }
.lv-d { margin-top: 2px; font-size: 12px; color: var(--el-text-color-secondary); line-height: 1.5; }
</style>
