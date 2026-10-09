import type { PermissionItem, SysMenuRoute, SysMenuSaveParams, ValidationRuleItem } from '@/types'
import { post } from '@/utils/request'

/** 菜单管理接口地址（任务书 13.4） */
export const MENU_API = {
  tree: '/admin/system/sysmenu/tree',
  list: '/admin/system/sysmenu/list',
  perms: '/admin/system/sysmenu/perms',
  rules: '/admin/system/sysmenu/rules',
  detail: '/admin/system/sysmenu/detail',
  create: '/admin/system/sysmenu/create',
  update: '/admin/system/sysmenu/update',
  remove: '/admin/system/sysmenu/delete'
} as const

/** 全量菜单树（菜单管理页用） */
export function treeMenu(): Promise<SysMenuRoute[]> {
  return post<SysMenuRoute[]>(MENU_API.tree)
}

/** 全部启用菜单的树（角色分配权限用，只校验登录） */
export function listMenu(): Promise<SysMenuRoute[]> {
  return post<SysMenuRoute[]>(MENU_API.list)
}

/** 权限标识清单（菜单表单的权限标识下拉用，只校验登录） */
export function permsMenu(): Promise<PermissionItem[]> {
  return post<PermissionItem[]>(MENU_API.perms)
}

/** 校验规则清单（开发期比对词条用，只校验登录） */
export function rulesMenu(): Promise<ValidationRuleItem[]> {
  return post<ValidationRuleItem[]>(MENU_API.rules)
}

/** 菜单详情（只校验登录） */
export function detailMenu(id: string): Promise<SysMenuRoute> {
  return post<SysMenuRoute>(MENU_API.detail, { id })
}

/** 新增菜单 */
export function createMenu(data: SysMenuSaveParams): Promise<void> {
  return post<void>(MENU_API.create, data)
}

/** 编辑菜单 */
export function updateMenu(data: SysMenuSaveParams): Promise<void> {
  return post<void>(MENU_API.update, data)
}

/** 批量删除菜单（入参与其他模块统一用 ids） */
export function deleteMenu(ids: string[]): Promise<void> {
  return post<void>(MENU_API.remove, { ids })
}
