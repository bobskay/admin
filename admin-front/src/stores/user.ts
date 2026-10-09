import { defineStore } from 'pinia'
import { ref } from 'vue'

import { getMe, login as loginApi, logout as logoutApi } from '@/api/auth'
import { checkPermission, type PermissionValue } from '@/composables/usePermission'
import type { LoginUser, SysMe, SysMeUser } from '@/types'
import { getToken, removeToken, setToken } from '@/utils/token'
import { removeLoginTime, writeLoginTime } from '@/utils/storage'

import { usePermissionStore } from './permission'

/**
 * 用户 store（任务书 17 章）：token、用户信息、角色、超管标志、权限集合。
 * 组合式写法，全部带类型。
 */
export const useUserStore = defineStore('user', () => {
  /** 登录 token（与本地存储保持一致） */
  const token = ref<string>(getToken())
  /** 当前用户基本信息 */
  const userInfo = ref<SysMeUser | null>(null)
  /** 角色编码数组 */
  const roles = ref<string[]>([])
  /** 是否超级管理员（唯一判据，禁止匹配 *:*:*） */
  const superAdmin = ref(false)
  /** 权限标识集合 */
  const perms = ref<string[]>([])
  /** 用户信息是否已加载（守卫据此判断是否需要调 me） */
  const infoLoaded = ref(false)

  /** 登录：保存 tokenId 与本次登录时间（工作台展示「登录时间」用） */
  async function login(username: string, password: string): Promise<LoginUser> {
    const result = await loginApi({ username, password })
    setToken(result.tokenId)
    token.value = result.tokenId
    writeLoginTime()
    return result
  }

  /**
   * 拉取当前用户信息，并生成动态路由。
   * 注意：这里只**生成**，注册由路由守卫在拿到结果后完成（避免 store 依赖 router 实例）。
   */
  async function fetchUserInfo(): Promise<SysMe> {
    const me = await getMe()
    userInfo.value = me.user ?? null
    roles.value = me.roles ?? []
    superAdmin.value = Boolean(me.superAdmin)
    perms.value = me.perms ?? []
    infoLoaded.value = true
    usePermissionStore().generateRoutes(me.menus ?? [])
    return me
  }

  /** 登出：调后端接口（失败也继续），然后清空状态并重置动态路由 */
  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 登出接口失败不阻断前端登出流程
    }
    clear()
  }

  /** 清空登录态：token、用户信息、权限集合、菜单与动态路由 */
  function clear(): void {
    removeToken()
    removeLoginTime()
    token.value = ''
    userInfo.value = null
    roles.value = []
    superAdmin.value = false
    perms.value = []
    infoLoaded.value = false
    const permissionStore = usePermissionStore()
    permissionStore.resetRoutes()
    permissionStore.clearMenus()
  }

  /**
   * 权限判断（与 v-has 指令共用同一套逻辑）：
   * 空值 → true；superAdmin → true；否则权限集合匹配。
   */
  function hasPermission(value: PermissionValue): boolean {
    return checkPermission(superAdmin.value, perms.value, value)
  }

  return {
    token,
    userInfo,
    roles,
    superAdmin,
    perms,
    infoLoaded,
    login,
    fetchUserInfo,
    logout,
    clear,
    hasPermission
  }
})
