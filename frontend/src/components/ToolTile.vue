<script setup>
defineProps({
  icon: { type: [Object, Function], required: true },
  color: { type: String, default: '#2f6fed' },
  size: { type: String, default: 'md' }   // md | lg
})
</script>

<template>
  <div class="tile" :class="size" :style="{ '--c': color }">
    <el-icon><component :is="icon" /></el-icon>
  </div>
</template>

<style scoped>
.tile { display: grid; place-items: center; flex-shrink: 0; border-radius: 10px; color: var(--c);
  background: color-mix(in srgb, var(--c) 14%, transparent); }
.tile.md { width: 40px; height: 40px; font-size: 20px; }
.tile.lg { width: 52px; height: 52px; font-size: 26px; border-radius: 13px; }
</style>

