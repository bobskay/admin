import {
  DARK_CLASS,
  DEFAULT_LANGUAGE,
  DEFAULT_THEME,
  LANGUAGE_CODES,
  STORAGE_KEYS,
  THEME_NAMES,
  type LanguageCode,
  type ThemeName
} from './constants'

/**
 * 本地存储与根元素标记的统一读写。
 * 不依赖 Pinia，可在 store 初始化之前（main.ts 挂载前）安全使用，避免主题闪烁。
 */

/** 读取字符串，读取失败或非浏览器环境返回 null */
export function readStorage(key: string): string | null {
  try {
    return window.localStorage.getItem(key)
  } catch {
    // 隐私模式等场景下 localStorage 可能不可用，静默降级
    return null
  }
}

/** 写入字符串，失败时静默 */
export function writeStorage(key: string, value: string): void {
  try {
    window.localStorage.setItem(key, value)
  } catch {
    // 写入失败不影响功能，忽略
  }
}

/** 删除键，失败时静默 */
export function removeStorage(key: string): void {
  try {
    window.localStorage.removeItem(key)
  } catch {
    // 忽略
  }
}

/** 判断取值是否为合法主题 */
export function isThemeName(value: unknown): value is ThemeName {
  return typeof value === 'string' && (THEME_NAMES as readonly string[]).includes(value)
}

/** 判断取值是否为合法语言 */
export function isLanguageCode(value: unknown): value is LanguageCode {
  return typeof value === 'string' && (LANGUAGE_CODES as readonly string[]).includes(value)
}

/** 读取持久化的主题，非法值回落默认主题 */
export function readPersistedTheme(): ThemeName {
  const value = readStorage(STORAGE_KEYS.THEME)
  return isThemeName(value) ? value : DEFAULT_THEME
}

/** 持久化主题 */
export function writePersistedTheme(theme: ThemeName): void {
  writeStorage(STORAGE_KEYS.THEME, theme)
}

/** 读取持久化的语言，非法值回落默认语言 */
export function readPersistedLanguage(): LanguageCode {
  const value = readStorage(STORAGE_KEYS.LANGUAGE)
  return isLanguageCode(value) ? value : DEFAULT_LANGUAGE
}

/** 持久化语言 */
export function writePersistedLanguage(language: LanguageCode): void {
  writeStorage(STORAGE_KEYS.LANGUAGE, language)
}

/** 读取「记住用户名」 */
export function readRememberedUsername(): string {
  return readStorage(STORAGE_KEYS.REMEMBER_USERNAME) ?? ''
}

/** 写入「记住用户名」 */
export function writeRememberedUsername(username: string): void {
  writeStorage(STORAGE_KEYS.REMEMBER_USERNAME, username)
}

/** 清除「记住用户名」 */
export function removeRememberedUsername(): void {
  removeStorage(STORAGE_KEYS.REMEMBER_USERNAME)
}

/** 记录本次登录成功的时间（毫秒时间戳字符串，供工作台展示「登录时间」） */
export function writeLoginTime(timestamp: number = Date.now()): void {
  writeStorage(STORAGE_KEYS.LOGIN_TIME, String(timestamp))
}

/** 读取登录时间；没有记录时返回 0（不是合法时间戳，展示层自行降级） */
export function readLoginTime(): number {
  const value = readStorage(STORAGE_KEYS.LOGIN_TIME)
  if (!value) {
    return 0
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

/** 清除登录时间（登出时用） */
export function removeLoginTime(): void {
  removeStorage(STORAGE_KEYS.LOGIN_TIME)
}

/** 读取侧边栏折叠状态（任务书 17 章要求持久化） */
export function readPersistedSidebarCollapsed(): boolean {
  return readStorage(STORAGE_KEYS.SIDEBAR_COLLAPSED) === '1'
}

/** 持久化侧边栏折叠状态 */
export function writePersistedSidebarCollapsed(collapsed: boolean): void {
  writeStorage(STORAGE_KEYS.SIDEBAR_COLLAPSED, collapsed ? '1' : '0')
}

/**
 * 把主题标记写到根元素：data-theme 属性 + dark 类名（仅黑主题）。
 * main.ts 在挂载前调用一次，实现防闪烁。
 */
export function applyThemeToDocument(theme: ThemeName): void {
  const root = document.documentElement
  root.setAttribute('data-theme', theme)
  root.classList.toggle(DARK_CLASS, theme === 'dark')
}

/** 把语言写到 html 的 lang 属性 */
export function applyLanguageToDocument(language: LanguageCode): void {
  document.documentElement.setAttribute('lang', language)
}
