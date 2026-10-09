import NProgress from 'nprogress'
import type { RouteLocationNormalized, Router } from 'vue-router'

import { translate } from '@/locales'
import { useAppStore } from '@/stores/app'
import { usePermissionStore } from '@/stores/permission'
import { useUserStore } from '@/stores/user'
import { HOME_PATH, LOGIN_PATH, NOT_FOUND_PATH, WHITE_LIST_PATHS } from '@/utils/constants'
import { getToken } from '@/utils/token'

/**
 * 路由守卫（严格按任务书 18.4 的 5 条顺序实现）。
 */

NProgress.configure({ showSpinner: false, minimum: 0.15 })

/** 应用名：优先取构建期注入的环境变量 */
function appName(): string {
  return import.meta.env.VITE_APP_TITLE || translate('common.appName')
}

/**
 * 设置文档标题：页面标题 - 应用名。
 * 菜单名是「数据」，直接展示；静态路由的 meta.title 是 i18n 键，需要翻译。
 */
function applyDocumentTitle(to: RouteLocationNormalized): void {
  const meta = to.meta
  let pageTitle = ''
  if (meta?.title) {
    pageTitle = meta.rawTitle ? String(meta.title) : translate(String(meta.title))
  }
  if (!pageTitle) {
    pageTitle = translate('common.appName')
  }
  const title = `${pageTitle} - ${appName()}`
  document.title = title
  useAppStore().setDocumentTitle(title)
}

export function setupRouterGuard(router: Router): void {
  router.beforeEach(async (to) => {
    NProgress.start()

    const token = getToken()
    const userStore = useUserStore()
    const permissionStore = usePermissionStore()
    const isWhiteList = WHITE_LIST_PATHS.includes(to.path)

    // 情况一：已有 token
    if (token) {
      // 目标是登录页 → 直接跳首页
      if (to.path === LOGIN_PATH) {
        return { path: HOME_PATH, replace: true }
      }

      // 用户信息未加载 → 调 me，生成并注册动态路由，再用 replace 重新进入目标路由
      if (!userStore.infoLoaded) {
        try {
          await userStore.fetchUserInfo()
          // 注册前会先移除上一次注册的动态路由，避免重复 addRoute
          permissionStore.registerRoutes(router)
          return { path: to.path, query: to.query, hash: to.hash, replace: true }
        } catch {
          // 失败（含 token 过期）→ 清 token 回登录页
          userStore.clear()
          NProgress.done()
          return { path: LOGIN_PATH, query: { redirect: to.fullPath }, replace: true }
        }
      }

      // 用户信息已加载 → 放行；但目标路由仍未匹配时兜底跳 404，不白屏
      if (to.matched.length === 0) {
        return { path: NOT_FOUND_PATH, replace: true }
      }
      return true
    }

    // 情况二：没有 token
    if (isWhiteList) {
      return true
    }
    return { path: LOGIN_PATH, query: { redirect: to.fullPath }, replace: true }
  })

  router.afterEach((to) => {
    applyDocumentTitle(to)
    NProgress.done()
  })

  router.onError(() => {
    NProgress.done()
  })
}

export default setupRouterGuard
