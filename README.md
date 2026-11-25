# 文档工具箱（doc-toolkit）

一套运行在本地的文档、PDF 与图片批量处理 Web 工具箱，基于 **Java 17 + Spring Boot 3** 与 **Vue 3 + Element Plus** 构建。所有文件均由本机后端直接处理，无需上传至第三方在线服务，兼顾隐私安全与批量处理效率。

---

## 功能列表

### PDF 工具
| 工具 | 接口 | 功能说明 |
|---|---|---|
| **PDF 合并** | `POST /api/merge` | 按列表顺序将多个 PDF 合并为一个，支持拖动或按钮调整顺序 |
| **PDF 拆分** | `POST /api/split` | 按页码范围（如 `1-3,5,7-9`）拆分，留空则每页独立导出并打包为 zip |
| **PDF 加水印** | `POST /api/watermark` | 批量添加中英文文字水印，支持字号、颜色、透明度、角度、居中/平铺及纸张效果实时预览 |
| **PDF 加密 / 解密** | `POST /api/pdfsec` | 支持 128 位加密（打开密码、权限密码、打印/复制权限控制）以及已知密码批量解密 |
| **PDF 压缩** | `POST /api/compress` | 轻度 / 均衡 / 强力三档压缩，对内嵌图片重编码并按需缩放，仅在体积变小时替换并展示前后对比 |

### 图片工具
| 工具 | 接口 | 功能说明 |
|---|---|---|
| **图片转 PDF** | `POST /api/img2pdf` | 将多张图片（JPG / PNG / GIF / BMP）按顺序合成一个 PDF，支持统一 A4 适配或保持原尺寸 |
| **PDF 转图片** | `POST /api/pdf2img` | 逐页导出为 PNG / JPG（72–300 DPI），支持自定义页间距竖向拼接为长图 |
| **图片格式转换** | `POST /api/imgconv` | PNG / JPG / BMP / GIF 批量互转（透明图转 JPG/BMP 自动填充白底），支持 JPG 质量与最大宽度等比缩放 |

### 文档与通用工具
| 工具 | 接口 | 功能说明 |
|---|---|---|
| **Word 转 PDF** | `POST /api/word2pdf` | 批量将 `.doc` / `.docx` 转换为 PDF（调用本机 Microsoft Word，失败项记录在 `_failed.txt`） |
| **批量重命名** | `POST /api/rename` | 支持前缀、后缀、查找替换、自定义起始值与位数的序号追加，并提供实时重命名预览 |

---

## 界面与工程特性

- **中后台布局体验**：深色可折叠分组侧边栏、工作台统计首页、面包屑导航、`Ctrl + K` 全局工具搜索、深色模式切换（本地持久化）。
- **实时进度与后台执行**：采用异步任务机制，展示“上传 → 处理 → 下载”三步进度与当前处理项；切换页面不中断任务，侧边栏同步显示运行中任务的实时百分比徽标。
- **文件管理体验**：统一校验拖拽与点击选择的文件类型及大小上限；顺序敏感工具支持 HTML5 拖拽排序；支持一键清除所有文件与“处理完成后自动清空”。
- **本地任务记录**：自动在浏览器 `localStorage` 保存最近 200 条处理记录，支持按工具和状态筛选与分页查看。
- **磁盘化上传与自动清理**：上传文件直接落盘至任务专属临时目录，PDF 采用随机访问与临时流缓存读取，避免大批量上传撑爆 JVM 内存；任务结束（成功或失败）自动删除临时文件。
- **服务健康监测**：顶部栏定时轮询 `GET /api/health`，实时反馈后端在线状态与活跃任务数。

---

## 技术栈

- **后端**：Java 17、Spring Boot 3.3.5、Apache PDFBox 3.0.3、Maven
- **前端**：Vue 3、Vite 5、Element Plus 2、@element-plus/icons-vue、Vue Router 4

---

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.8+
- Node.js 18+（含 npm）
- *可选*：如需使用 **Word 转 PDF**，需在 Windows 环境下安装 Microsoft Word

### 1. 启动后端（默认端口 `8080`）

```bash
cd backend
mvn spring-boot:run
```

或打包为可执行 JAR 后启动：

```bash
cd backend
mvn -DskipTests package
java -jar target/tool-kit-1.0.0.jar
```

### 2. 启动前端（默认端口 `5173`）

```bash
cd frontend
npm install
npm run dev
```

Vite 开发服务器已配置 `/api` 代理转发至 `http://localhost:8080`。启动后在浏览器访问：

```
http://localhost:5173
```

---

## 项目结构

```text
doc-toolkit/
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/toolkit/
│       │   ├── ToolKitApplication.java   # Spring Boot 启动类
│       │   ├── ToolController.java       # REST 接口与异步任务提交/查询/下载
│       │   ├── ApiExceptionHandler.java  # 上传超限等统一异常处理
│       │   ├── Job.java                  # 任务状态与进度模型
│       │   ├── JobService.java           # 异步任务调度与临时目录清理
│       │   ├── DiskFile.java             # 磁盘临时文件封装
│       │   ├── FileUtil.java             # 临时目录、ZIP 打包与重名去重
│       │   ├── PdfService.java           # PDF 合并/拆分/图片互转/水印/加解密/压缩
│       │   ├── ImageService.java         # 图片格式转换与缩放
│       │   ├── WordToPdfService.java     # 调用本机 Word COM 批量转 PDF
│       │   └── RenameService.java        # 批量重命名
│       └── resources/
│           └── application.properties    # 端口与上传大小配置
└── frontend/
    ├── index.html
    ├── package.json
    ├── vite.config.js
    ├── public/favicon.svg
    └── src/
        ├── main.js / router.js / api.js / tools.js / workspace.js / utils.js / styles.css
        ├── layout/AppLayout.vue          # 顶部栏 + 分组侧边栏框架
        ├── stores/                       # 主题(theme)、健康监测(health)、历史记录(history)
        ├── views/                        # 工作台(HomeView)、工具区(ToolView)、任务记录(HistoryView)
        └── components/                   # 拖拽上传、文件列表、进度/结果面板及 8 个工具参数面板
```

---

## 异步任务接口协议

所有处理接口遵循统一的三步协议：

1. **提交任务**：`POST /api/{tool}`（`multipart/form-data`，文件字段名 `files`）→ 返回 `{ "jobId": "..." }`
2. **轮询进度**：`GET /api/jobs/{jobId}` → 返回 `{ "status": "RUNNING|DONE|FAILED", "total": 4, "done": 2, "current": "...", "error": null }`
3. **下载结果**：`GET /api/jobs/{jobId}/result` → 返回二进制文件流（`Content-Disposition` 携带文件名），下载后自动清理任务；未取走的结果将在 30 分钟后自动过期清理。
4. **健康检查**：`GET /api/health` → 返回 `{ "status": "UP", "activeJobs": 0 }`

---

## 扩展新工具

1. **后端**：在 `com.toolkit` 中添加业务方法（按条目调用 `job.step(...)` 更新进度），并在 `ToolController` 中注册 `POST /api/{key}`。
2. **前端**：在 `frontend/src/components/options/` 中新建参数面板组件（可选），然后在 `frontend/src/tools.js` 的 `tools` 数组中追加一项配置即可自动出现在工作台、侧边栏和全局搜索中。

---

## 使用说明与限制

- **上传大小限制**：默认单个文件不超过 `200MB`，单次提交总计不超过 `600MB`（可在 `backend/src/main/resources/application.properties` 与 `frontend/src/utils.js` 中同步调整）。
- **Word 转 PDF**：依赖 Windows 下的 PowerShell 与本机 Microsoft Word COM 组件，非 Windows 环境或未安装 Word 时不可用。
- **PDF 中文字体水印**：默认从 `C:\Windows\Fonts\` 加载系统黑体/楷体/仿宋 TTF 字体并按需嵌入子集；Linux/macOS 部署时若需中文水印，需将字体路径调整为对应系统字体。
- **PDF 压缩效果**：仅针对内嵌图片重编码与降采样，扫描件及多图 PDF 效果显著；纯文字或矢量为主的 PDF 本身体积较小，压缩幅度有限。
