import { reactive } from 'vue'

/** 后端健康状态：online 为 null 表示尚未检测 */
export const health = reactive({ online: null, activeJobs: 0 })

let timer = null

async function ping() {
  try {
    const res = await fetch('/api/health')
    if (!res.ok) throw new Error()
    const j = await res.json()
    health.online = true
    health.activeJobs = j.activeJobs || 0
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

