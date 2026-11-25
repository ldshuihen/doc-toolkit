<script setup>
import { computed } from 'vue'

const props = defineProps({ opt: { type: Object, required: true } })

const wmStyle = computed(() => ({
  color: props.opt.color || '#888888',
  opacity: (props.opt.opacity ?? 25) / 100,
  fontSize: Math.max(10, Math.min(24, Math.round((props.opt.size || 36) * 0.38))) + 'px',
  transform: `rotate(${-(props.opt.angle || 0)}deg)`
}))
</script>

<template>
  <el-form label-position="top" class="opt-form">
    <el-form-item label="水印文字">
      <el-input v-model="opt.text" placeholder="请输入水印内容" clearable />
    </el-form-item>

    <el-row :gutter="12">
      <el-col :span="12">
        <el-form-item label="字号">
          <el-input-number v-model="opt.size" :min="10" :max="120" controls-position="right" class="full" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="颜色">
          <div class="color-row">
            <el-color-picker v-model="opt.color" />
            <span class="hex">{{ opt.color }}</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item :label="`不透明度 (${opt.opacity}%)`">
      <el-slider v-model="opt.opacity" :min="5" :max="100" />
    </el-form-item>

    <el-form-item :label="`旋转角度 (${opt.angle}°)`">
      <el-slider v-model="opt.angle" :min="-90" :max="90" />
    </el-form-item>

    <el-form-item label="排版方式">
      <el-radio-group v-model="opt.tile" class="seg-group">
        <el-radio-button :value="true">全页平铺</el-radio-button>
        <el-radio-button :value="false">居中单个</el-radio-button>
      </el-radio-group>
    </el-form-item>

    <el-form-item label="纸张效果预览">
      <div class="stage">
        <div class="paper">
          <div class="doc-mock" aria-hidden="true">
            <div class="m-head">
              <span class="m-title"></span>
              <span class="m-sub"></span>
            </div>
            <div class="m-para">
              <span></span><span></span><span class="s"></span>
            </div>
            <div class="m-box">
              <i></i><i></i><i></i>
            </div>
            <div class="m-para">
              <span></span><span class="m"></span>
            </div>
            <div class="m-foot">1 / 1</div>
          </div>

          <div class="wm-layer" :class="{ tile: opt.tile }">
            <template v-if="opt.tile">
              <span v-for="n in 9" :key="n" class="wm" :style="wmStyle">{{ opt.text || '水印预览' }}</span>
            </template>
            <span v-else class="wm" :style="wmStyle">{{ opt.text || '水印预览' }}</span>
          </div>
        </div>
      </div>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.color-row { display: flex; align-items: center; gap: 10px; }
.hex { font-size: 13px; color: var(--el-text-color-secondary); font-family: monospace; }

.stage { width: 100%; padding: 14px; border-radius: 10px; display: flex; justify-content: center;
  background: var(--el-fill-color-light); border: 1px solid var(--el-border-color-lighter); }
.paper { width: 100%; max-width: 240px; height: 174px; background: #ffffff; border-radius: 4px;
  position: relative; overflow: hidden; user-select: none; border: 1px solid #e5e7eb;
  box-shadow: 0 4px 14px rgba(15, 23, 42, .08), 0 1px 2px rgba(15, 23, 42, .05); }

.doc-mock { position: absolute; inset: 0; padding: 14px 16px 10px; display: flex; flex-direction: column; gap: 9px; }
.m-head { display: flex; flex-direction: column; gap: 5px; padding-bottom: 7px; border-bottom: 1px solid #f1f5f9; }
.m-title { display: block; width: 52%; height: 7px; border-radius: 3px; background: #cbd5e1; }
.m-sub { display: block; width: 32%; height: 4px; border-radius: 2px; background: #e2e8f0; }
.m-para { display: flex; flex-direction: column; gap: 5px; }
.m-para span { display: block; height: 4px; border-radius: 2px; background: #e2e8f0; width: 100%; }
.m-para span.m { width: 78%; }
.m-para span.s { width: 62%; }
.m-box { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; padding: 7px; border-radius: 4px;
  background: #f8fafc; border: 1px solid #f1f5f9; }
.m-box i { display: block; height: 18px; border-radius: 3px; background: #e2e8f0; }
.m-foot { margin-top: auto; text-align: center; font-size: 9px; color: #cbd5e1; font-variant-numeric: tabular-nums; }

.wm-layer { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; pointer-events: none; }
.wm-layer.tile { display: grid; grid-template-columns: repeat(3, 1fr); grid-template-rows: repeat(3, 1fr); place-items: center; }
.wm { white-space: nowrap; font-weight: 600; line-height: 1; }
</style>
