<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, Clock, Fold, Expand, Search, Moon, Sunny, Files, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { tools, toolMap, groupMap, groupedTools } from '../tools'
import { dark } from '../stores/theme'
import { health, startHealth, stopHealth } from '../stores/health'
import { anyRunning, runningJobs } from '../workspace'

const route = useRoute()
const router = useRouter()

const collapsed = ref(false)
const query = ref('')
const searchRef = ref()
const exited = ref(false)

async function exitApp() {
  try {
    stopHealth()
    const res = await fetch('/api/shutdown', { method: 'POST' })
    if (!res.ok) throw new Error('无法退出服务')
    exited.value = true
    window.close()
  } catch (e) {
    ElMessage.error(e.message || '退出失败')
    startHealth()
  }
}

const crumbs = computed(() => {
  if (route.name === 'tool') {
    const t = toolMap[route.params.key]
    return t ? [groupMap[t.group].label, t.label] : []
  }
  return route.name === 'history' ? ['任务记录'] : []
})

const healthView = computed(() => {
  if (health.online === null) return { type: 'info', text: '检测中' }
  if (!health.online) return { type: 'danger', text: '后端离线' }
  return health.activeJobs
    ? { type: 'warning', text: `处理中 ${health.activeJobs}` }
    : { type: 'success', text: '服务正常' }
})

function searchTools(s, cb) {
  const k = s.trim().toLowerCase()
  cb(tools
    .filter(t => !k || `${t.label} ${t.desc} ${t.keywords}`.toLowerCase().includes(k))
    .map(t => ({ value: t.label, tool: t })))
}
function onSelect(item) {
  query.value = ''
  searchRef.value?.blur()
  router.push('/tool/' + item.tool.key)
}

function onResize() { collapsed.value = window.innerWidth < 992 }
function onKey(e) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    searchRef.value?.focus()
  }
}
function onBeforeUnload(e) {
  if (anyRunning()) { e.preventDefault(); e.returnValue = '' }
}

onMounted(() => {
  onResize()
  startHealth()
  window.addEventListener('resize', onResize)
  window.addEventListener('keydown', onKey)
  window.addEventListener('beforeunload', onBeforeUnload)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  window.removeEventListener('keydown', onKey)
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<template>
  <el-container class="shell">
    <el-aside class="aside" :width="collapsed ? '64px' : '236px'">
      <div class="brand" @click="router.push('/')">
        <div class="logo"><el-icon :size="18"><Files /></el-icon></div>
        <div v-show="!collapsed" class="brand-text">
          <div class="b1">文档工具箱</div>
          <div class="b2">DOC TOOLKIT</div>
        </div>
      </div>

      <el-menu class="menu" :default-active="route.path" router :collapse="collapsed" :collapse-transition="false"
               background-color="transparent" text-color="#a6b2c8" active-text-color="#ffffff">
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item-group v-for="g in groupedTools" :key="g.key">
          <template #title>{{ g.label }}</template>
          <el-menu-item v-for="t in g.tools" :key="t.key" :index="'/tool/' + t.key"
                        :class="{ 'is-running': !!runningJobs[t.key] }">
            <el-icon>
              <component :is="t.icon" />
              <span v-if="collapsed && runningJobs[t.key]" class="run-dot-mini"></span>
            </el-icon>
            <template #title>
              <span class="nav-label">{{ t.label }}</span>
              <span v-if="runningJobs[t.key]" class="run-badge tnum">
                <span class="run-pulse"></span>{{ runningJobs[t.key].percent }}%
              </span>
            </template>
          </el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group>
          <template #title>系统</template>
          <el-menu-item index="/history">
            <el-icon><Clock /></el-icon>
            <template #title>任务记录</template>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </el-aside>

    <el-container class="right">
      <el-header class="header" height="56px">
        <div class="h-left">
          <el-icon class="fold" :size="20" @click="collapsed = !collapsed">
            <component :is="collapsed ? Expand : Fold" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">工作台</el-breadcrumb-item>
            <el-breadcrumb-item v-for="c in crumbs" :key="c">{{ c }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="h-right">
          <el-autocomplete ref="searchRef" v-model="query" class="search" :fetch-suggestions="searchTools"
                           :prefix-icon="Search" placeholder="搜索工具（Ctrl + K）" :trigger-on-focus="true"
                           clearable @select="onSelect">
            <template #default="{ item }">
              <div class="sg">
                <el-icon><component :is="item.tool.icon" /></el-icon>
                <span>{{ item.tool.label }}</span>
                <small>{{ groupMap[item.tool.group].label }}</small>
              </div>
            </template>
          </el-autocomplete>
          <el-tag :type="healthView.type" effect="light" round>
            <span class="dot" :class="healthView.type"></span>{{ healthView.text }}
          </el-tag>
          <el-switch v-model="dark" inline-prompt :active-icon="Moon" :inactive-icon="Sunny" title="切换深色模式" />
          <el-popconfirm v-if="health.desktop" width="240" confirm-button-text="退出" cancel-button-text="取消"
                         confirm-button-type="danger"
                         :title="anyRunning() ? '当前仍有任务在处理中，确定退出吗？' : '确定退出文档工具箱吗？'"
                         @confirm="exitApp">
            <template #reference>
              <el-button type="danger" plain size="small" :icon="SwitchButton">退出程序</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-header>

      <el-main class="main">
        <div v-if="exited" class="exited">
          <el-result icon="success" title="文档工具箱已安全退出" sub-title="后台服务已停止，您现在可以关闭此浏览器标签页。" />
        </div>
        <router-view v-else v-slot="{ Component, route: r }">
          <transition name="fade" mode="out-in">
            <component :is="Component" :key="r.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
.shell { height: 100vh; }
.aside { background: #0f1c33; display: flex; flex-direction: column; transition: width .2s; overflow: hidden; }
.brand { height: 56px; display: flex; align-items: center; gap: 12px; padding: 0 16px; cursor: pointer; flex-shrink: 0;
  border-bottom: 1px solid #ffffff12; }
.logo { width: 32px; height: 32px; border-radius: 9px; display: grid; place-items: center; color: #fff; flex-shrink: 0;
  background: linear-gradient(135deg, #4f8bff, #2f6fed 60%, #6a4cf5); box-shadow: 0 4px 12px #2f6fed66; }
.brand-text { white-space: nowrap; line-height: 1.2; }
.b1 { color: #fff; font-weight: 600; font-size: 15px; }
.b2 { color: #5f7396; font-size: 10px; letter-spacing: 1.5px; }

.menu { border-right: none !important; flex: 1; overflow-y: auto; overflow-x: hidden; padding: 6px 0 16px;
  --el-menu-hover-bg-color: rgba(255, 255, 255, .07); --el-menu-item-height: 42px; }
.aside .el-menu:not(.el-menu--collapse) .el-menu-item { margin: 2px 10px; border-radius: 8px; width: auto; }
.aside .el-menu--collapse .el-menu-item { margin: 2px 0; }
.aside .el-menu-item.is-active { background: var(--el-color-primary) !important; box-shadow: 0 4px 12px #2f6fed55; }
.aside .el-menu-item-group__title { color: #5f7396; font-size: 12px; padding: 14px 20px 6px; letter-spacing: .5px; }
.aside .el-menu--collapse .el-menu-item-group__title { display: none; }
.aside .el-menu--collapse .el-menu-item-group + .el-menu-item-group { border-top: 1px solid #ffffff12; margin-top: 6px; padding-top: 6px; }
.aside .el-menu-item .el-icon { position: relative; }
.nav-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.run-badge { margin-left: auto; display: inline-flex; align-items: center; gap: 5px; padding: 1px 7px; border-radius: 999px;
  font-size: 11px; line-height: 16px; color: #93c5fd; background: rgba(59, 130, 246, .22); flex-shrink: 0; }
.el-popper .run-badge { margin-left: 8px; }
.aside .el-menu-item.is-active .run-badge { color: #fff; background: rgba(255, 255, 255, .24); }
.run-pulse { width: 6px; height: 6px; border-radius: 50%; background: #38bdf8; box-shadow: 0 0 0 0 rgba(56, 189, 248, .7);
  animation: pulse-ring 1.4s infinite; }
.aside .el-menu-item.is-active .run-pulse { background: #fff; }
.run-dot-mini { position: absolute; top: -2px; right: -3px; width: 7px; height: 7px; border-radius: 50%;
  background: #38bdf8; box-shadow: 0 0 0 2px #0f1c33; animation: pulse-ring 1.4s infinite; }
@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, .7); }
  70% { box-shadow: 0 0 0 5px rgba(56, 189, 248, 0); }
  100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0); }
}

.right { min-width: 0; }
.header { display: flex; align-items: center; justify-content: space-between; background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light); padding: 0 20px; }
.h-left, .h-right { display: flex; align-items: center; gap: 16px; }
.fold { cursor: pointer; color: var(--el-text-color-regular); }
.fold:hover { color: var(--el-color-primary); }
.search { width: 260px; }
.sg { display: flex; align-items: center; gap: 8px; }
.sg small { margin-left: auto; color: var(--el-text-color-placeholder); }
.dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 6px; background: currentColor; }
.main { background: var(--el-bg-color-page); padding: 20px 24px 32px; overflow-y: auto; }
.exited { display: flex; align-items: center; justify-content: center; min-height: 60vh; }
@media (max-width: 760px) { .search { width: 150px; } .h-left .el-breadcrumb { display: none; } }
</style>

