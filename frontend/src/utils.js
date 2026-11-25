/** 与后端 application.properties 保持一致的上传限制 */
export const MAX_FILE_SIZE = 200 * 1024 * 1024
export const MAX_TOTAL_SIZE = 600 * 1024 * 1024

export function formatSize(n) {
  if (n >= 1073741824) return (n / 1073741824).toFixed(2) + ' GB'
  if (n >= 1048576) return (n / 1048576).toFixed(1) + ' MB'
  return Math.max(1, Math.round(n / 1024)) + ' KB'
}

/** 取小写扩展名（含点），如 ".pdf"；没有扩展名返回空串 */
export function extOf(name) {
  const i = name.lastIndexOf('.')
  return i < 0 ? '' : name.slice(i).toLowerCase()
}

/** 把文件名列表缩略成 "a.pdf、b.pdf 等 5 个" */
export function briefNames(names, max = 3) {
  const head = names.slice(0, max).join('、')
  return names.length > max ? `${head} 等 ${names.length} 个` : head
}

export function formatDuration(ms) {
  if (ms < 1000) return ms + ' 毫秒'
  if (ms < 60000) return (ms / 1000).toFixed(1) + ' 秒'
  return `${Math.floor(ms / 60000)} 分 ${Math.round((ms % 60000) / 1000)} 秒`
}

const pad = n => String(n).padStart(2, '0')

export function formatTime(ts) {
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
