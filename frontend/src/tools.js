import {
  CopyDocument, Scissor, Stamp, Lock, Box, Picture, Film, Switch, Document, EditPen
} from '@element-plus/icons-vue'
import { formatSize } from './utils'
import RenameOptions from './components/options/RenameOptions.vue'
import SplitOptions from './components/options/SplitOptions.vue'
import Img2PdfOptions from './components/options/Img2PdfOptions.vue'
import Pdf2ImgOptions from './components/options/Pdf2ImgOptions.vue'
import WatermarkOptions from './components/options/WatermarkOptions.vue'
import PdfSecOptions from './components/options/PdfSecOptions.vue'
import CompressOptions from './components/options/CompressOptions.vue'
import ImgConvOptions from './components/options/ImgConvOptions.vue'

/** 工具分组（侧边栏与工作台的分组顺序），color 用于图标底色 */
export const groups = [
  { key: 'pdf', label: 'PDF 工具', color: '#e5484d' },
  { key: 'image', label: '图片工具', color: '#1e9e5a' },
  { key: 'doc', label: '文档工具', color: '#2f6fed' },
  { key: 'common', label: '通用工具', color: '#8b5cf6' }
]

/**
 * 工具定义。新增工具只需：
 *  1) 在 components/options 下写一个参数面板（可选）；
 *  2) 在此处增加一项；key 即后端接口 /api/{key}。
 *
 * 字段说明：
 *  group / icon / label / desc / keywords / tips  展示与搜索
 *  accept    允许的扩展名，'*' 表示不限
 *  single    只允许选择一个文件
 *  sortable  文件顺序影响结果（支持拖动排序）
 *  options   参数面板组件
 *  defaults  参数默认值（每个工具各自保存，切换页面不会丢）
 *  validate  提交前校验，返回错误文案
 *  build     把参数写入 FormData（字段名需与后端一致）
 *  afterDone 处理完成后追加到结果里的说明文字
 */
export const tools = [
  {
    key: 'merge', group: 'pdf', icon: CopyDocument, label: 'PDF 合并',
    desc: '按列表顺序把多个 PDF 合并成一个，可拖动调整顺序',
    keywords: 'merge combine 合并 拼接',
    tips: ['按列表从上到下的顺序合并，可拖动或使用 ↑↓ 调整顺序', '已加密的 PDF 需要先解密再合并'],
    accept: '.pdf', sortable: true,
    defaults: () => ({}), build() {}
  },
  {
    key: 'split', group: 'pdf', icon: Scissor, label: 'PDF 拆分',
    desc: '按页码范围拆分，例如 1-3,5,7-9；留空则每页拆成一个文件',
    keywords: 'split extract 拆分 提取 分割',
    tips: ['页码范围示例：1-3,5,7-9，每一段生成一个 PDF', '留空则每页拆分为一个独立 PDF，结果打包为 zip'],
    accept: '.pdf', single: true,
    options: SplitOptions,
    defaults: () => ({ ranges: '' }),
    build(fd, o) { fd.append('ranges', o.ranges) }
  },
  {
    key: 'watermark', group: 'pdf', icon: Stamp, label: 'PDF 加水印',
    desc: '批量给 PDF 添加文字水印，支持中文、透明度、角度与平铺',
    keywords: 'watermark 水印',
    tips: ['中文水印依赖服务器上的中文字体（Windows 默认可用）', '处理多个文件时结果打包为 zip'],
    accept: '.pdf',
    options: WatermarkOptions,
    defaults: () => ({ text: '仅供内部参考', size: 36, opacity: 25, angle: 35, tile: true, color: '#888888' }),
    validate(o) { return o.text.trim() ? '' : '请输入水印文字' },
    build(fd, o) {
      fd.append('text', o.text); fd.append('fontSize', o.size)
      fd.append('opacity', (o.opacity / 100).toFixed(2)); fd.append('angle', o.angle)
      fd.append('tile', o.tile); fd.append('color', o.color)
    }
  },
  {
    key: 'pdfsec', group: 'pdf', icon: Lock, label: 'PDF 加密/解密',
    desc: '批量给 PDF 设置打开/权限密码，或输入密码解除加密',
    keywords: 'encrypt decrypt password 加密 解密 密码',
    tips: ['权限密码用于限制打印/复制，不填则与打开密码相同', '解密需提供正确的打开密码；仅有权限限制的文件可留空'],
    accept: '.pdf',
    options: PdfSecOptions,
    defaults: () => ({ mode: 'encrypt', userPwd: '', ownerPwd: '', allowPrint: true, allowCopy: true }),
    validate(o) {
      return o.mode === 'encrypt' && !o.userPwd && !o.ownerPwd ? '请至少设置打开密码或权限密码' : ''
    },
    build(fd, o) {
      fd.append('mode', o.mode); fd.append('userPwd', o.userPwd); fd.append('ownerPwd', o.ownerPwd)
      fd.append('allowPrint', o.allowPrint); fd.append('allowCopy', o.allowCopy)
    }
  },
  {
    key: 'compress', group: 'pdf', icon: Box, label: 'PDF 压缩',
    desc: '重编码并缩放内嵌图片以减小体积（纯文字 PDF 基本无法再压缩）',
    keywords: 'compress shrink 压缩 瘦身 减小',
    tips: ['仅重新编码内嵌图片，纯文字 PDF 效果有限', '扫描件、含大量照片的 PDF 压缩率最高'],
    accept: '.pdf',
    options: CompressOptions,
    defaults: () => ({ level: 'medium' }),
    build(fd, o) { fd.append('level', o.level) },
    afterDone(o, items, blob) {
      const before = items.reduce((s, it) => s + it.file.size, 0)
      if (blob.size >= before * 0.97) {
        return '压缩效果不明显：该 PDF 可能以文字/矢量为主，缺少可压缩的图片'
      }
      const pct = Math.round((1 - blob.size / before) * 100)
      return `体积 ${formatSize(before)} → ${formatSize(blob.size)}，减少 ${pct}%`
    }
  },
  {
    key: 'img2pdf', group: 'image', icon: Picture, label: '图片转 PDF',
    desc: '把多张图片按顺序合成一个 PDF（支持 jpg / png / gif / bmp）',
    keywords: 'image picture jpg png 图片 转pdf',
    tips: ['按列表顺序生成页面，可拖动调整顺序', '适配 A4 时，横向图片会自动使用横向页面'],
    accept: '.jpg,.jpeg,.png,.gif,.bmp', sortable: true,
    options: Img2PdfOptions,
    defaults: () => ({ fit: true }),
    build(fd, o) { fd.append('fit', o.fit) }
  },
  {
    key: 'pdf2img', group: 'image', icon: Film, label: 'PDF 转图片',
    desc: '将 PDF 逐页转为 PNG / JPG 图片，支持拼成一张长图（可批量）',
    keywords: 'pdf image long 长图 转图片 截图',
    tips: ['长图模式会把所有页面竖向拼接，页数很多时请降低 DPI', '单个 PDF 的长图直接下载图片，批量处理时打包为 zip'],
    accept: '.pdf',
    options: Pdf2ImgOptions,
    defaults: () => ({ format: 'png', dpi: 150, longImage: false, gap: 0 }),
    build(fd, o) {
      fd.append('format', o.format); fd.append('dpi', o.dpi)
      fd.append('longImage', o.longImage); fd.append('gap', o.gap)
    }
  },
  {
    key: 'imgconv', group: 'image', icon: Switch, label: '图片格式转换',
    desc: '批量转换图片格式（PNG / JPG / BMP / GIF），支持缩放与 JPG 质量调节',
    keywords: 'convert format resize 格式 转换 缩放 压缩',
    tips: ['透明背景转为 JPG / BMP 时会自动填充白底', '填写最大宽度可等比缩小图片，不会放大'],
    accept: '.jpg,.jpeg,.png,.gif,.bmp',
    options: ImgConvOptions,
    defaults: () => ({ format: 'jpg', quality: 85, maxWidth: null }),
    build(fd, o) {
      fd.append('format', o.format); fd.append('quality', o.quality)
      if (o.maxWidth) fd.append('maxWidth', o.maxWidth)
    }
  },
  {
    key: 'word2pdf', group: 'doc', icon: Document, label: 'Word 转 PDF',
    desc: '批量将 .doc / .docx 转换为 PDF（需本机安装 Word）',
    keywords: 'word docx doc office 文档 转pdf',
    tips: ['调用本机 Microsoft Word 转换，需要 Windows 并已安装 Word', '转换失败的文件会记录在压缩包内的 _failed.txt'],
    accept: '.doc,.docx',
    defaults: () => ({}), build() {},
    afterDone() { return '如有失败项，请查看压缩包内的 _failed.txt' }
  },
  {
    key: 'rename', group: 'common', icon: EditPen, label: '批量重命名',
    desc: '添加前后缀、查找替换、追加序号（可拖动调整序号顺序）',
    keywords: 'rename 重命名 改名 序号',
    tips: ['在文件名（不含扩展名）上查找替换，再追加前后缀与序号', '参数区下方会预览前 3 个文件的重命名结果'],
    accept: '*', sortable: true,
    options: RenameOptions,
    defaults: () => ({ prefix: '', suffix: '', find: '', replace: '', useSeq: false, startNo: 1, digits: 3 }),
    build(fd, o) {
      fd.append('prefix', o.prefix); fd.append('suffix', o.suffix)
      fd.append('find', o.find); fd.append('replace', o.replace)
      if (o.useSeq) { fd.append('startNo', o.startNo ?? 0); fd.append('digits', o.digits ?? 3) }
    }
  }
]

export const toolMap = Object.fromEntries(tools.map(t => [t.key, t]))
export const groupMap = Object.fromEntries(groups.map(g => [g.key, g]))
export const groupedTools = groups.map(g => ({ ...g, tools: tools.filter(t => t.group === g.key) }))
