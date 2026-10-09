import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { RouteRecordRaw, Router } from 'vue-router'

import { resolveViewComponent } from '@/router/componentLoader'
import type { SysMenuRoute } from '@/types'
import {
  KEEP_ALIVE,
  MENU_ROUTE_PREFIX,
  MENU_TYPE,
  ROOT_ROUTE_NAME,
  VISIBLE
} from '@/utils/constants'
import { toFlatRoutes } from '@/utils/tree'

/**
 * 权限 / 菜单 store（任务书 17、18 章）。
 *
 * **两套用途要分清**：
 * - `menuTree`：后端原始菜单树，保留层级，供侧边栏渲染（不要拿扁平化结果渲染侧边栏）；
 * - `routes`：由菜单树生成的**扁平化**动态路由，统一挂在名为 Root 的布局路由下。
 */
export const usePermissionStore = defineStore('permission', () => {
  /** 后端原始菜单树（侧边栏用） */
  const menuTree = ref<SysMenuRoute[]>([])
  /** 动态路由（扁平化） */
  const routes = ref<RouteRecordRaw[]>([])
  /** 本次已注册的动态路由名，注册前用于移除 */
  const registeredNames = ref<string[]>([])

  /** 路由实例：registerRoutes 时记住，便于 resetRoutes 无需再传参 */
  let savedRouter: Router | null = null

  /** 把菜单节点转为一条扁平的动态路由 */
  function toRouteRecord(node: SysMenuRoute): RouteRecordRaw | null {
    const rawPath = (node.path ?? '').trim()
    if (!rawPath) {
      return null
    }
    // 去掉开头斜杠，作为 Root 的相对子路径，最终 URL 就是菜单配置的完整路径
    const path = rawPath.replace(/^\/+/, '')
    if (!path) {
      return null
    }
    return {
      // name 用 menu-{菜单id}，保证唯一稳定，供页面缓存 include 使用
      path,
      name: `${MENU_ROUTE_PREFIX}${node.id}`,
      component: resolveViewComponent(node.component, node.path, `${MENU_ROUTE_PREFIX}${node.id}`),
      meta: {
        // 菜单名是「数据」，直接展示，不再走 i18n
        title: node.name,
        rawTitle: true,
        icon: node.icon ?? null,
        keepAlive: node.keepAlive === KEEP_ALIVE.YES,
        visible: node.visible === VISIBLE.SHOW,
        menuId: String(node.id)
      }
    }
  }

  /**
   * 由菜单树生成动态路由（只处理 type 1 / 2；目录本身不生成路由，只用于侧边栏分组）。
   * 这里只生成、不注册，注册由守卫在拿到结果后调用 registerRoutes 完成。
   */
  function generateRoutes(menus: SysMenuRoute[] | null | undefined): RouteRecordRaw[] {
    const tree = menus ?? []
    menuTree.value = tree

    const generated: RouteRecordRaw[] = []
    toFlatRoutes(tree).forEach((node) => {
      // 只处理菜单节点：目录不生成路由，按钮只贡献权限标识
      if (node.type !== MENU_TYPE.MENU) {
        return
      }
      const record = toRouteRecord(node)
      if (record) {
        generated.push(record)
      }
    })

    routes.value = generated
    return generated
  }

  /**
   * 注册动态路由。注册前先移除上一次注册的动态路由，
   * 避免重复 addRoute 造成的警告与缓存错乱（任务书 18.3.8）。
   */
  function registerRoutes(router: Router): void {
    savedRouter = router
    resetRoutes(router)
    const names: string[] = []
    routes.value.forEach((route) => {
      router.addRoute(ROOT_ROUTE_NAME, route)
      if (route.name) {
        names.push(String(route.name))
      }
    })
    registeredNames.value = names
  }

  /** 移除已注册的动态路由（不传 router 时使用最近一次注册的实例） */
  function resetRoutes(router?: Router): void {
    const target = router ?? savedRouter
    if (target) {
      registeredNames.value.forEach((name) => {
        if (target.hasRoute(name)) {
          target.removeRoute(name)
        }
      })
    }
    registeredNames.value = []
  }

  /** 清空菜单与动态路由（登出时用） */
  function clearMenus(): void {
    menuTree.value = []
    routes.value = []
  }

  return {
    menuTree,
    routes,
    registeredNames,
    generateRoutes,
    registerRoutes,
    resetRoutes,
    clearMenus
  }
})
