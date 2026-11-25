import { ref, computed } from 'vue'

const KEY = 'toolkit.history'
const MAX = 200

function load() {
  try { return JSON.parse(localStorage.getItem(KEY) || '[]') } catch { return [] }
}

/** 任务记录，保存在浏览器本地，仅用于展示，不会上传 */
export const records = ref(load())

function save() { localStorage.setItem(KEY, JSON.stringify(records.value)) }

export function addRecord(r) {
  records.value.unshift({ id: Date.now() + '-' + Math.random().toString(36).slice(2, 6), time: Date.now(), ...r })
  if (records.value.length > MAX) records.value.length = MAX
  save()
}

export function clearRecords() {
  records.value = []
  save()
}

export const stats = computed(() => {
  const list = records.value
  const ok = list.filter(r => r.status === 'success')
  return {
    total: list.length,
    files: ok.reduce((s, r) => s + (r.fileCount || 0), 0),
    rate: list.length ? Math.round(ok.length * 100 / list.length) : null
  }
})

