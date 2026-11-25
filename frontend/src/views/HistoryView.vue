<script setup>
import { ref, computed, watch } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { tools } from '../tools'
import { records, clearRecords } from '../stores/history'
import { formatSize, formatDuration, formatTime } from '../utils'

const toolFilter = ref('')
const statusFilter = ref('')
const page = ref(1)
const pageSize = 10

const filtered = computed(() => records.value.filter(r =>
  (!toolFilter.value || r.toolKey === toolFilter.value) &&
  (!statusFilter.value || r.status === statusFilter.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize, page.value * pageSize))

watch([toolFilter, statusFilter], () => { page.value = 1 })

function clearAll() {
  clearRecords()
  ElMessage.success('已清空任务记录')
}
</script>

<template>
  <div class="page">
    <div class="head">
      <div>
        <h2>任务记录</h2>
        <p>最近 200 条处理记录，保存在当前浏览器中，不会上传到服务器。</p>
      </div>
    </div>

    <el-card shadow="never" class="panel">
      <div class="toolbar">
        <el-select v-model="toolFilter" placeholder="全部工具" clearable style="width: 180px">
          <el-option v-for="t in tools" :key="t.key" :label="t.label" :value="t.key" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 130px">
          <el-option label="成功" value="success" />
          <el-option label="失败" value="failed" />
        </el-select>
        <span class="grow"></span>
        <el-popconfirm title="确定清空全部任务记录吗？" confirm-button-text="清空" cancel-button-text="取消"
                       confirm-button-type="danger" @confirm="clearAll">
          <template #reference>
            <el-button :icon="Delete" :disabled="!records.length">清空记录</el-button>
          </template>
        </el-popconfirm>
      </div>

      <el-table :data="paged" stripe style="width: 100%" empty-text="暂无任务记录">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.time) }}</template>
        </el-table-column>
        <el-table-column prop="toolLabel" label="工具" min-width="130" />
        <el-table-column label="文件数" width="80" align="right">
          <template #default="{ row }">{{ row.fileCount }}</template>
        </el-table-column>
        <el-table-column label="输入大小" width="100" align="right">
          <template #default="{ row }">{{ formatSize(row.inBytes || 0) }}</template>
        </el-table-column>
        <el-table-column label="输出" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.outName">{{ row.outName }}（{{ formatSize(row.outBytes || 0) }}）</span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100" align="right">
          <template #default="{ row }">{{ formatDuration(row.durationMs || 0) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'success' ? 'success' : 'danger'">
              {{ row.status === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span :class="{ err: row.status === 'failed' }">{{ row.note || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="filtered.length"
                       layout="total, prev, pager, next" background hide-on-single-page />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.head h2 { margin: 0 0 4px; font-size: 20px; font-weight: 600; }
.head p { margin: 0 0 18px; color: var(--el-text-color-secondary); font-size: 14px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; align-items: center; }
.grow { flex: 1; }
.pager { display: flex; justify-content: flex-end; margin-top: 14px; }
.muted { color: var(--el-text-color-placeholder); }
.err { color: var(--el-color-danger); }
</style>

