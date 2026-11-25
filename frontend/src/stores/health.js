import { reactive } from 'vue'

/** 后端健康状态：online 为 null 表示尚未检测；desktop 表示是否为绿色 EXE 桌面模式 */
export const health = reactive({ online: null, activeJobs: 0, desktop: false })

let timer = null

async function ping() {
  try {
    const res = await fetch('/api/health')
    if (!res.ok) throw new Error()
    const j = await res.json()
    health.online = true
    health.activeJobs = j.activeJobs || 0
    health.desktop = !!j.desktop
  } catch {
    health.online = false
    health.activeJobs = 0
  }
}

export function startHealth() {
  if (timer) return
  ping()
  timer = setInterval(ping, 8000)
}

export function stopHealth() {
  if (!timer) return
  clearInterval(timer)
  timer = null
}
