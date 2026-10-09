import { createRouter, createWebHistory } from 'vue-router'

import { setupRouterGuard } from './guard'
import { staticRoutes } from './routes'

/**
 * 路由实例。
 * 动态路由（来自后端菜单树）由守卫在拿到 me 的结果后通过 permission store 注册。
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: staticRoutes,
  scrollBehavior: () => ({ left: 0, top: 0 })
})

setupRouterGuard(router)

export default router
