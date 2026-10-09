import type {
  PageResult,
  SysRoleBrief,
  SysRoleInfo,
  SysRolePageParams,
  SysRoleSaveParams
} from '@/types'
import { post } from '@/utils/request'

/** 角色管理接口地址（任务书 13.3） */
export const ROLE_API = {
  page: '/admin/system/sysrole/page',
  list: '/admin/system/sysrole/list',
  detail: '/admin/system/sysrole/detail',
  create: '/admin/system/sysrole/create',
  update: '/admin/system/sysrole/update',
  remove: '/admin/system/sysrole/delete',
  menuIds: '/admin/system/sysrole/menuids',
  assignMenus: '/admin/system/sysrole/assignmenus'
} as const

/** 分页查询角色 */
export function pageRole(data: SysRolePageParams): Promise<PageResult<SysRoleInfo>> {
  return post<PageResult<SysRoleInfo>>(ROLE_API.page, data)
}

/** 全部启用角色（下拉选择用，只校验登录） */
export function listRole(): Promise<SysRoleBrief[]> {
  return post<SysRoleBrief[]>(ROLE_API.list)
}

/** 角色详情（只校验登录） */
export function detailRole(id: string): Promise<SysRoleInfo> {
  return post<SysRoleInfo>(ROLE_API.detail, { id })
}

/** 新增角色 */
export function createRole(data: SysRoleSaveParams): Promise<void> {
  return post<void>(ROLE_API.create, data)
}

/** 编辑角色 */
export function updateRole(data: SysRoleSaveParams): Promise<void> {
  return post<void>(ROLE_API.update, data)
}

/** 批量删除角色 */
export function deleteRole(ids: string[]): Promise<void> {
  return post<void>(ROLE_API.remove, { ids })
}

/** 角色已勾选的菜单 id 列表（只校验登录） */
export function getRoleMenuIds(roleId: string): Promise<string[]> {
  return post<string[]>(ROLE_API.menuIds, { roleId })
}

/** 分配菜单权限（menuIds 提交前去重） */
export function assignRoleMenus(roleId: string, menuIds: string[]): Promise<void> {
  const unique = Array.from(new Set(menuIds))
  return post<void>(ROLE_API.assignMenus, { roleId, menuIds: unique })
}
