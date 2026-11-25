import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { runJob } from './api'
import { MAX_FILE_SIZE, MAX_TOTAL_SIZE, extOf, formatSize, briefNames } from './utils'
import { addRecord } from './stores/history'

/**
 * 每个工具一个“工作区”，状态保存在模块级别：
 * 切换页面再回来，已选文件、参数、进度和结果都还在；任务运行中离开页面也不会中断。
 */
const spaces = new Map()
let seq = 0

/** 正在运行中的工具任务：{ [toolKey]: { percent, phase } }，供侧边栏实时展示运行徽标 */
export const runningJobs = reactive({})

export function getWorkspace(tool) {
  if (!spaces.has(tool.key)) spaces.set(tool.key, create(tool))
  return spaces.get(tool.key)
}

export function anyRunning() {
  return Object.keys(runningJobs).length > 0
}

function saveBlob(blob, filename) {
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  URL.revokeObjectURL(a.href)
}

function create(tool) {
  const ws = reactive({
    items: [],                 // [{ id, file }]
    opts: tool.defaults(),
    loading: false,
    autoClear: false,
    error: '',
    prog: { show: false, phase: 'idle', percent: 0, label: '', detail: '' },
    result: null,              // { filename, blob, size, durationMs, extra, count, inBytes }
    addFiles, remove, move, reorder, clear, resetOpts, submit, download
  })

  /** 加入文件：统一做类型、单文件大小、总大小校验，拖拽和点击选择走同一条路径 */
  function addFiles(list) {
    const exts = tool.accept === '*' ? null : tool.accept.split(',').map(s => s.trim().toLowerCase())
    const badType = [], tooBig = [], overTotal = [], ok = []
    let total = tool.single ? 0 : ws.items.reduce((s, it) => s + it.file.size, 0)

    for (const f of list) {
      if (exts && !exts.includes(extOf(f.name))) { badType.push(f.name); continue }
      if (f.size > MAX_FILE_SIZE) { tooBig.push(f.name); continue }
      if (total + f.size > MAX_TOTAL_SIZE) { overTotal.push(f.name); continue }
      total += f.size
      ok.push({ id: ++seq, file: f })
    }

    if (tool.single) { if (ok.length) ws.items = ok.slice(0, 1) }
    else ws.items.push(...ok)
    if (ok.length) { ws.error = ''; ws.result = null }

    const warns = []
    if (badType.length) warns.push(`已忽略 ${briefNames(badType)}：仅支持 ${tool.accept.split(',').join(' ')}`)
    if (tooBig.length) warns.push(`${briefNames(tooBig)} 超过单个文件上限 ${formatSize(MAX_FILE_SIZE)}`)
    if (overTotal.length) warns.push(`${briefNames(overTotal)} 未加入：总大小不能超过 ${formatSize(MAX_TOTAL_SIZE)}`)
    if (warns.length) ElMessage({ type: 'warning', message: warns.join('；'), duration: 6000, showClose: true })
  }

  function remove(i) { ws.items.splice(i, 1) }

  function move(i, d) {
    const j = i + d
    if (j < 0 || j >= ws.items.length) return
    const a = ws.items
    ;[a[i], a[j]] = [a[j], a[i]]
  }

  function reorder(from, to) {
    const [it] = ws.items.splice(from, 1)
    ws.items.splice(to, 0, it)
  }

  function clear() {
    ws.items = []
    ws.error = ''
    ws.result = null
    ws.prog.show = false
  }

  function resetOpts() {
    Object.assign(ws.opts, tool.defaults())
    ElMessage.info('已恢复默认参数')
  }

  function download() {
    if (ws.result) saveBlob(ws.result.blob, ws.result.filename)
  }

  async function submit() {
    if (ws.loading) return
    if (!ws.items.length) return ElMessage.warning('请先选择文件')
    const problem = tool.validate?.(ws.opts, ws.items)
    if (problem) return ElMessage.warning(problem)

    const fd = new FormData()
    ws.items.forEach(it => fd.append('files', it.file))
    tool.build(fd, ws.opts)

    const count = ws.items.length
    const inBytes = ws.items.reduce((s, it) => s + it.file.size, 0)
    const startedAt = Date.now()
    const base = { toolKey: tool.key, toolLabel: tool.label, fileCount: count, inBytes }

    ws.loading = true
    ws.error = ''
    ws.result = null
    Object.assign(ws.prog, { show: true, phase: 'upload', percent: 0, label: '正在上传', detail: '' })
    runningJobs[tool.key] = { percent: 0, phase: 'upload' }
    try {
      const { blob, filename } = await runJob('/api/' + tool.key, fd, {
        onUpload(p) {
          ws.prog.phase = 'upload'
          ws.prog.percent = p
          ws.prog.label = p >= 100 ? '上传完成，等待处理' : `正在上传 ${p}%`
          runningJobs[tool.key] = { percent: p, phase: 'upload' }
        },
        onProgress(s) {
          ws.prog.phase = 'process'
          ws.prog.percent = s.total ? Math.round(s.done * 100 / s.total) : 0
          ws.prog.label = s.status === 'DONE' ? '处理完成' : `处理中 ${s.done} / ${s.total}`
          ws.prog.detail = s.current
          runningJobs[tool.key] = { percent: ws.prog.percent, phase: 'process' }
        }
      })
      Object.assign(ws.prog, { phase: 'download', percent: 100, label: '正在下载结果' })
      runningJobs[tool.key] = { percent: 100, phase: 'download' }
      const extra = tool.afterDone?.(ws.opts, ws.items, blob) ?? ''
      const durationMs = Date.now() - startedAt
      saveBlob(blob, filename)
      ws.result = { filename, blob, size: blob.size, durationMs, extra, count, inBytes }
      Object.assign(ws.prog, { phase: 'done', label: '处理完成' })
      addRecord({ ...base, status: 'success', outName: filename, outBytes: blob.size, durationMs, note: extra })
      ElMessage.success(`处理完成，已开始下载 ${filename}`)
      if (ws.autoClear) ws.items = []
    } catch (e) {
      ws.error = e.message
      ws.prog.show = false
      addRecord({ ...base, status: 'failed', durationMs: Date.now() - startedAt, note: e.message })
      ElMessage.error(e.message)
    } finally {
      ws.loading = false
      delete runningJobs[tool.key]
    }
  }

  return ws
}

