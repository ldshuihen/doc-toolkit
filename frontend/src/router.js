import { createRouter, createWebHashHistory } from 'vue-router'
import { toolMap } from './tools'
import { groups } from './tools'
import HomeView from './views/HomeView.vue'
import ToolView from './views/ToolView.vue'
import HistoryView from './views/HistoryView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView, meta: { title: '工作台' } },
    {
      path: '/tool/:key', name: 'tool', component: ToolView,
      props: route => ({ toolKey: route.params.key }),
      beforeEnter: to => (toolMap[to.params.key] ? true : '/')
    },
    { path: '/history', name: 'history', component: HistoryView, meta: { title: '任务记录' } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.afterEach(to => {
  const t = to.name === 'tool' ? toolMap[to.params.key] : null
  const g = t ? groups.find(x => x.key === t.group) : null
  document.title = (t ? `${t.label} - ` : to.meta.title ? `${to.meta.title} - ` : '') + '文档工具箱'
  void g
})

export default router

