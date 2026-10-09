import { MENU_TYPE, ROOT_PARENT_ID, VISIBLE } from './constants'
import type { SysMenuRoute } from '@/types'

/**
 * 菜单树工具。
 * 注意：后端菜单树有**两套用途** —— 侧边栏用原始层级树，路由用扁平化结果（任务书 17 章）。
 */

/** 按 sort 升序、id 升序排序（id 是字符串形式的数字，需按数值比较） */
function bySortThenId(a: SysMenuRoute, b: SysMenuRoute): number {
  const sortDiff = (a.sort ?? 0) - (b.sort ?? 0)
  if (sortDiff !== 0) {
    return sortDiff
  }
  const idA = Number(a.id)
  const idB = Number(b.id)
  if (Number.isFinite(idA) && Number.isFinite(idB) && idA !== idB) {
    return idA - idB
  }
  return String(a.id).localeCompare(String(b.id))
}

/**
 * 把菜单树扁平化为一维数组（深度优先，保持 sort 顺序）。
 * 目录与菜单都在结果里，由调用方按 type 过滤。
 */
export function toFlatRoutes(menus: SysMenuRoute[] | null | undefined): SysMenuRoute[] {
  const result: SysMenuRoute[] = []
  const walk = (nodes: SysMenuRoute[]): void => {
    const sorted = [...nodes].sort(bySortThenId)
    sorted.forEach((node) => {
      // 每项都复制一份且不带 children，避免扁平结果里出现嵌套引用
      const { children, ...rest } = node
      result.push({ ...rest })
      if (children && children.length > 0) {
        walk(children)
      }
    })
  }
  if (menus && menus.length > 0) {
    walk(menus)
  }
  return result
}

/**
 * 反查某菜单 id 的祖先链（面包屑用），从根到该节点本身。
 * 找不到返回空数组（不抛异常）。
 */
export function findMenuPath(
  menus: SysMenuRoute[] | null | undefined,
  id: string | number | null | undefined
): SysMenuRoute[] {
  if (!menus || menus.length === 0 || id === null || id === undefined || id === '') {
    return []
  }
  const target = String(id)
  const path: SysMenuRoute[] = []

  const walk = (nodes: SysMenuRoute[]): boolean => {
    const sorted = [...nodes].sort(bySortThenId)
    for (const node of sorted) {
      path.push(node)
      if (String(node.id) === target) {
        return true
      }
      if (node.children && node.children.length > 0 && walk(node.children)) {
        return true
      }
      path.pop()
    }
    return false
  }

  return walk(menus) ? path : []
}

/**
 * 过滤菜单树：去掉 visible=0 的节点、按钮节点，以及过滤后没有任何可展示子节点的目录。
 * 返回新的树，不修改入参。
 */
export function filterVisibleMenus(menus: SysMenuRoute[] | null | undefined): SysMenuRoute[] {
  if (!menus || menus.length === 0) {
    return []
  }
  const result: SysMenuRoute[] = []
  ;[...menus].sort(bySortThenId).forEach((node) => {
    if (node.type === MENU_TYPE.BUTTON || node.visible === VISIBLE.HIDE) {
      return
    }
    const children = filterVisibleMenus(node.children)
    if (node.type === MENU_TYPE.DIRECTORY && children.length === 0) {
      // 空目录不渲染
      return
    }
    result.push({ ...node, children })
  })
  return result
}

/**
 * 构建侧边栏渲染用的树。
 * 相比 filterVisibleMenus 多做一件事：把父节点缺失（数据异常或父节点被删）的**孤儿节点按顶级处理**，
 * 不因为找不到父节点而丢菜单（任务书 11.3 / 18.3）。
 */
export function buildSidebarTree(menus: SysMenuRoute[] | null | undefined): SysMenuRoute[] {
  if (!menus || menus.length === 0) {
    return []
  }
  const flat = toFlatRoutes(menus)
  const nodeMap = new Map<string, SysMenuRoute>()
  flat.forEach((node) => {
    nodeMap.set(String(node.id), { ...node, children: [] })
  })

  const roots: SysMenuRoute[] = []
  nodeMap.forEach((node) => {
    const parentId = node.parentId === null || node.parentId === undefined ? ROOT_PARENT_ID : String(node.parentId)
    const parent = parentId === ROOT_PARENT_ID ? undefined : nodeMap.get(parentId)
    if (parent && String(parent.id) !== String(node.id)) {
      parent.children = parent.children ?? []
      parent.children.push(node)
    } else {
      // 父节点不存在（或被自己引用）时按顶级处理
      roots.push(node)
    }
  })

  return filterVisibleMenus(roots)
}

/**
 * 计算菜单的完整路径。
 * 菜单表里配置的多为绝对路径（/system/user）；若配置的是相对路径则与父级路径拼接。
 */
export function resolveMenuPath(parentPath: string, path: string | null | undefined): string {
  const current = (path ?? '').trim()
  if (current.startsWith('/')) {
    return current
  }
  const parent = (parentPath ?? '').replace(/\/+$/, '')
  if (!current) {
    return parent || '/'
  }
  return `${parent}/${current}`
}
