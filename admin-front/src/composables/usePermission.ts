import { computed } from 'vue'

import { useUserStore } from '@/stores/user'

/**
 * 权限判断组合式函数。
 *
 * 判断顺序（任务书 19.1，指令与 store 共用**同一套**逻辑，不要写两份）：
 *   1. 传入值为空 → 直接放行；
 *   2. superAdmin 为真 → 直接放行（超管的唯一判据路径）；
 *   3. 权限集合匹配任一传入标识。
 *
 * **不要**匹配 `*:*:*` 字符串，它只是后端返回的可读展示值。
 */

/** 指令 / 判断函数接受的权限值 */
export type PermissionValue = string | string[] | null | undefined

/** 传入值是否为空（null / undefined / 空字符串 / 空数组） */
export function isEmptyPermissionValue(value: PermissionValue): boolean {
  if (value === null || value === undefined) {
    return true
  }
  if (typeof value === 'string') {
    return value.trim() === ''
  }
  if (Array.isArray(value)) {
    return value.filter((item) => typeof item === 'string' && item.trim() !== '').length === 0
  }
  return false
}

/**
 * 权限判断核心逻辑（纯函数，不依赖 store，便于指令与 store 复用）。
 */
export function checkPermission(
  superAdmin: boolean,
  perms: readonly string[] | null | undefined,
  value: PermissionValue
): boolean {
  // 1. 空值不做处理
  if (isEmptyPermissionValue(value)) {
    return true
  }
  // 2. 超级管理员直接通过
  if (superAdmin) {
    return true
  }
  // 3. 权限集合匹配
  const owned = new Set<string>()
  ;(perms ?? []).forEach((item) => {
    if (typeof item === 'string' && item.trim() !== '') {
      owned.add(item.trim())
    }
  })
  const required = (Array.isArray(value) ? value : [value as string])
    .filter((item): item is string => typeof item === 'string')
    .map((item) => item.trim())
    .filter((item) => item !== '')
  if (required.length === 0) {
    return true
  }
  return required.some((item) => owned.has(item))
}

/** 组合式函数：供少数需要条件渲染（而非移除元素）的场景使用 */
export function usePermission() {
  const userStore = useUserStore()

  const isSuperAdmin = computed(() => userStore.superAdmin)

  function hasPermission(value: PermissionValue): boolean {
    return checkPermission(userStore.superAdmin, userStore.perms, value)
  }

  return { hasPermission, isSuperAdmin }
}
