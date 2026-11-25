<script setup>
defineProps({ opt: { type: Object, required: true } })
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="输出格式">
      <el-radio-group v-model="opt.format" class="seg-group">
        <el-radio-button value="png">PNG 无损</el-radio-button>
        <el-radio-button value="jpg">JPG 轻量</el-radio-button>
      </el-radio-group>
      <div class="hint">{{ opt.format === 'png' ? '文字线条清晰无损，适合文档截图。' : '压缩体积更小，适合含大量照片的页面。' }}</div>
    </el-form-item>

    <el-form-item label="渲染清晰度 (DPI)">
      <el-select v-model="opt.dpi" class="full">
        <el-option :value="72" label="72 DPI — 快速预览" />
        <el-option :value="150" label="150 DPI — 均衡推荐" />
        <el-option :value="200" label="200 DPI — 高清文档" />
        <el-option :value="300" label="300 DPI — 超清印刷" />
      </el-select>
    </el-form-item>

    <el-form-item label="长图模式">
      <el-switch v-model="opt.longImage" active-text="将所有页面竖向拼成一张长图" />
    </el-form-item>

    <el-form-item v-if="opt.longImage" :label="`页面间距 (${opt.gap} px)`">
      <el-slider v-model="opt.gap" :min="0" :max="100" />
      <div class="hint">页数过多时请适当降低 DPI，避免生成超大尺寸图片。</div>
    </el-form-item>
  </el-form>
</template>
