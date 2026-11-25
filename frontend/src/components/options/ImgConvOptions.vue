<script setup>
defineProps({ opt: { type: Object, required: true } })
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="目标格式">
      <el-radio-group v-model="opt.format" class="seg-group">
        <el-radio-button value="jpg">JPG</el-radio-button>
        <el-radio-button value="png">PNG</el-radio-button>
        <el-radio-button value="bmp">BMP</el-radio-button>
        <el-radio-button value="gif">GIF</el-radio-button>
      </el-radio-group>
      <div class="hint">
        {{ opt.format === 'jpg' || opt.format === 'bmp' ? '透明背景将自动填充白色底色。' : '保留原图色彩格式。' }}
      </div>
    </el-form-item>

    <el-form-item v-if="opt.format === 'jpg'" :label="`JPG 质量 (${opt.quality}%)`">
      <el-slider v-model="opt.quality" :min="10" :max="100" />
    </el-form-item>

    <el-form-item label="最大宽度（像素，可选）">
      <el-input-number v-model="opt.maxWidth" :min="1" :max="20000" controls-position="right"
                       placeholder="不限制（原尺寸）" class="full" />
      <div class="hint">仅当原图宽度超过该值时等比缩小，不会放大图片。</div>
    </el-form-item>
  </el-form>
</template>
