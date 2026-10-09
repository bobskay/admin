import type { LoginParams, LoginUser, SysCount, SysMe } from '@/types'
import { post } from '@/utils/request'

/**
 * 认证模块接口地址（任务书 13.1）。
 * 地址只在这里定义一次，页面与指令一律引用该常量。
 * 注意：这里不带 /api 前缀（由 axios 的 baseURL 统一提供）。
 */
export const AUTH_API = {
  login: '/admin/system/sysauth/login',
  logout: '/admin/system/sysauth/logout',
  me: '/admin/system/sysauth/me',
  count: '/admin/system/sysauth/count'
} as const

/** 登录：返回 tokenId 与过期秒数 */
export function login(data: LoginParams): Promise<LoginUser> {
  return post<LoginUser>(AUTH_API.login, data)
}

/** 登出（白名单接口，后端内部自行校验 token） */
export function logout(): Promise<void> {
  return post<void>(AUTH_API.logout)
}

/** 当前登录用户信息（含角色、superAdmin、权限标识、菜单树） */
export function getMe(): Promise<SysMe> {
  return post<SysMe>(AUTH_API.me)
}

/** 工作台统计（只校验登录，不参与权限控制） */
export function getCount(): Promise<SysCount> {
  return post<SysCount>(AUTH_API.count)
}
