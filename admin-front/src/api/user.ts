import type {
  PageResult,
  SysUserAssignRolesParams,
  SysUserCreateParams,
  SysUserInfo,
  SysUserPageParams,
  SysUserResetPasswordParams,
  SysUserUpdateParams
} from '@/types'
import { post } from '@/utils/request'

/**
 * 用户管理接口地址（任务书 13.2）。
 * 权限标识默认等于接口地址，因此这些常量同时用于 v-has 指令。
 */
export const USER_API = {
  page: '/admin/system/sysuser/page',
  detail: '/admin/system/sysuser/detail',
  create: '/admin/system/sysuser/create',
  update: '/admin/system/sysuser/update',
  remove: '/admin/system/sysuser/delete',
  resetPassword: '/admin/system/sysuser/resetpassword',
  changeStatus: '/admin/system/sysuser/changestatus',
  assignRoles: '/admin/system/sysuser/assignroles'
} as const

/** 分页查询用户 */
export function pageUser(data: SysUserPageParams): Promise<PageResult<SysUserInfo>> {
  return post<PageResult<SysUserInfo>>(USER_API.page, data)
}

/** 用户详情（只校验登录） */
export function detailUser(id: string): Promise<SysUserInfo> {
  return post<SysUserInfo>(USER_API.detail, { id })
}

/** 新增用户 */
export function createUser(data: SysUserCreateParams): Promise<void> {
  return post<void>(USER_API.create, data)
}

/** 编辑用户（不含密码与角色） */
export function updateUser(data: SysUserUpdateParams): Promise<void> {
  return post<void>(USER_API.update, data)
}

/** 批量删除用户（单条也传单元素数组） */
export function deleteUser(ids: string[]): Promise<void> {
  return post<void>(USER_API.remove, { ids })
}

/** 重置密码 */
export function resetUserPassword(id: string, newPassword: string): Promise<void> {
  return post<void>(USER_API.resetPassword, { id, newPassword } satisfies SysUserResetPasswordParams)
}

/** 启用 / 禁用 */
export function changeUserStatus(id: string, status: number): Promise<void> {
  return post<void>(USER_API.changeStatus, { id, status })
}

/** 分配角色 */
export function assignUserRoles(userId: string, roleIds: string[]): Promise<void> {
  return post<void>(USER_API.assignRoles, { userId, roleIds } satisfies SysUserAssignRolesParams)
}
