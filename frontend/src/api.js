/**
 * 与后端交互：上传 → 轮询进度 → 下载结果。
 * 统一把各种失败（后端未启动、重启后任务丢失、文件过大等）转换成用户看得懂的提示。
 */
const MSG_OFFLINE = '无法连接后端服务，请确认后端已启动'
const MSG_GONE = '任务已失效（后端可能已重启或任务已过期），请重新提交'
const MSG_TOO_LARGE = '上传的文件过大：单个文件不能超过 200MB，一次上传总计不能超过 600MB'

const sleep = ms => new Promise(r => setTimeout(r, ms))

function httpMessage(status, serverMsg) {
  if (serverMsg) return serverMsg
  if (status === 413) return MSG_TOO_LARGE
  if (status >= 500) return `${MSG_OFFLINE}（HTTP ${status}）`
  return '服务返回 ' + status
}

/** 用 XHR 上传以获得上传进度；返回服务端 JSON */
export function upload(url, formData, onProgress) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', url)
    xhr.upload.onprogress = e => {
      if (e.lengthComputable) onProgress?.(Math.round(e.loaded * 100 / e.total))
    }
    xhr.onload = () => {
      let body = {}
      try { body = JSON.parse(xhr.responseText) } catch { /* ignore */ }
      if (xhr.status < 300) resolve(body)
      else reject(new Error(httpMessage(xhr.status, body.message)))
    }
    xhr.onerror = () => reject(new Error(MSG_OFFLINE))
    xhr.send(formData)
  })
}

/**
 * GET 请求。网络中断或 5xx（后端重启期间 Vite 代理会返回 500）时短暂重试，
 * 仍失败则提示后端不可用；404 表示任务已不存在。
 */
async function get(url, retries = 5) {
  for (let i = 0; ; i++) {
    let res
    try {
      res = await fetch(url)
    } catch {
      if (i >= retries) throw new Error(MSG_OFFLINE)
      await sleep(1000)
      continue
    }
    if (res.status === 404) throw new Error(MSG_GONE)
    if (res.status >= 500) {
      if (i >= retries) throw new Error(`${MSG_OFFLINE}（HTTP ${res.status}）`)
      await sleep(1000)
      continue
    }
    if (!res.ok) throw new Error('服务返回 ' + res.status)
    return res
  }
}

/** 从 Content-Disposition 取服务端给出的文件名 */
function filenameFrom(res, fallback) {
  const cd = res.headers.get('Content-Disposition') || ''
  const m = /filename\*=UTF-8''([^;]+)/i.exec(cd) || /filename="?([^";]+)"?/i.exec(cd)
  if (!m) return fallback
  try { return decodeURIComponent(m[1]) } catch { return m[1] }
}

/**
 * 提交一个批处理任务并等待完成。
 * @param hooks.onUpload   (percent) 上传进度
 * @param hooks.onProgress (status)  处理进度 {status,total,done,current}
 * @returns {{blob: Blob, filename: string}}
 */
export async function runJob(endpoint, formData, hooks = {}) {
  const { jobId } = await upload(endpoint, formData, hooks.onUpload)
  if (!jobId) throw new Error('服务返回异常，请重试')
  for (;;) {
    const s = await (await get('/api/jobs/' + jobId)).json()
    if (s.status === 'FAILED') throw new Error(s.error || '处理失败')
    hooks.onProgress?.(s)
    if (s.status === 'DONE') break
    await sleep(300)
  }
  const res = await get('/api/jobs/' + jobId + '/result')
  return { blob: await res.blob(), filename: filenameFrom(res, 'result') }
}

