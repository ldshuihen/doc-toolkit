<script setup>
defineProps({ opt: { type: Object, required: true } })
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="页码范围">
      <el-input v-model="opt.ranges" placeholder="例如 1-3,5,7-9；留空则每页单独拆分" clearable />
      <div class="hint">支持逗号分隔多段范围，每段导出为一个 PDF，最终打包为 zip。</div>
    </el-form-item>
  </el-form>
</template>
