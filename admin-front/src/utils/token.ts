import { STORAGE_KEYS } from './constants'
import { removeStorage, readStorage, writeStorage } from './storage'

/**
 * token 存取。键名固定 admin_token（任务书 16.2）。
 * 使用 localStorage 而非 sessionStorage，便于多标签页同步监听 storage 事件。
 */

/** 读取 token，不存在返回空字符串 */
export function getToken(): string {
  return readStorage(STORAGE_KEYS.TOKEN) ?? ''
}

/** 写入 token */
export function setToken(token: string): void {
  if (!token) {
    removeToken()
    return
  }
  writeStorage(STORAGE_KEYS.TOKEN, token)
}

/** 清除 token */
export function removeToken(): void {
  removeStorage(STORAGE_KEYS.TOKEN)
}

/** 是否存在 token */
export function hasToken(): boolean {
  return getToken().length > 0
}
