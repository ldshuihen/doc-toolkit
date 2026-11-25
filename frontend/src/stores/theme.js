import { ref, watch } from 'vue'

const KEY = 'toolkit.dark'
const PRIMARY = '#2f6fed'

export const dark = ref(localStorage.getItem(KEY) === '1')

const toRgb = h => {
  const n = parseInt(h.slice(1), 16)
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255]
}
/** 按权重 w 混合两个颜色（w 为 c1 的占比），与 Element Plus 生成色阶的算法一致 */
function mix(c1, c2, w) {
  const a = toRgb(c1), b = toRgb(c2)
  return '#' + a.map((v, i) => Math.round(v * w + b[i] * (1 - w)).toString(16).padStart(2, '0')).join('')
}

/** 应用主题：切换 dark 类，并按当前明/暗模式重新生成主色色阶 */
function apply() {
  const el = document.documentElement
  el.classList.toggle('dark', dark.value)
  const s = el.style
  const bg = dark.value ? '#141414' : '#ffffff'
  s.setProperty('--el-color-primary', PRIMARY)
  for (const i of [3, 5, 7, 8, 9]) s.setProperty(`--el-color-primary-light-${i}`, mix(bg, PRIMARY, i / 10))
  s.setProperty('--el-color-primary-dark-2', mix(dark.value ? '#ffffff' : '#000000', PRIMARY, 0.2))
}

export function initTheme() {
  apply()
  watch(dark, v => {
    localStorage.setItem(KEY, v ? '1' : '0')
    apply()
  })
}

