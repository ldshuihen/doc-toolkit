<script setup>
defineProps({ opt: { type: Object, required: true } })
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="页面尺寸">
      <el-radio-group v-model="opt.fit" class="seg-group">
        <el-radio-button :value="true">适配 A4 页面</el-radio-button>
        <el-radio-button :value="false">保持原尺寸</el-radio-button>
      </el-radio-group>
      <div class="hint">
        {{ opt.fit ? '按图片宽高比等比缩放到 A4 页面内，横向图片自动使用横向页面。' : '每一页的 PDF 尺寸完全等于对应图片的像素尺寸，不做缩放。' }}
      </div>
    </el-form-item>
  </el-form>
</template>
