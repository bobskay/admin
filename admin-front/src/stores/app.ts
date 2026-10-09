import { defineStore } from 'pinia'
import { ref } from 'vue'

import { setLocale } from '@/locales'
import {
  DEFAULT_LANGUAGE,
  DEFAULT_THEME,
  type LanguageCode,
  type ThemeName
} from '@/utils/constants'
import {
  applyThemeToDocument,
  readPersistedLanguage,
  readPersistedSidebarCollapsed,
  readPersistedTheme,
  writePersistedLanguage,
  writePersistedSidebarCollapsed,
  writePersistedTheme
} from '@/utils/storage'

/**
 * 应用级状态：主题、语言、侧边栏折叠、页面标题。
 * 全部持久化到本地存储（任务书 17 章）。
 */
export const useAppStore = defineStore('app', () => {
  /** 当前主题：light / dark / blue */
  const theme = ref<ThemeName>(readPersistedTheme())
  /** 当前语言：zh-CN / en-US */
  const language = ref<LanguageCode>(readPersistedLanguage())
  /** 侧边栏是否折叠 */
  const sidebarCollapsed = ref(readPersistedSidebarCollapsed())
  /** 当前页面标题（用于 document.title） */
  const documentTitle = ref('')

  /**
   * 初始化：把主题与语言同步到根元素与 i18n。
   * main.ts 在挂载前已用纯函数设置过一次根元素标记（防闪烁），这里做一次兜底同步。
   */
  function initTheme(): void {
    applyThemeToDocument(theme.value)
    setLocale(language.value)
  }

  /** 切换主题：写本地存储 + 立即更新根元素标记 */
  function setTheme(value: ThemeName): void {
    if (!value) {
      return
    }
    theme.value = value
    writePersistedTheme(value)
    applyThemeToDocument(value)
  }

  /** 复位为默认主题 */
  function resetTheme(): void {
    setTheme(DEFAULT_THEME)
  }

  /** 切换语言：写本地存储 + 同步 i18n 与 html lang */
  function setLanguage(value: LanguageCode): void {
    if (!value) {
      return
    }
    language.value = value
    writePersistedLanguage(value)
    setLocale(value)
  }

  /** 复位为默认语言 */
  function resetLanguage(): void {
    setLanguage(DEFAULT_LANGUAGE)
  }

  /** 折叠 / 展开侧边栏 */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
    writePersistedSidebarCollapsed(sidebarCollapsed.value)
  }

  /** 显式设置侧边栏折叠状态 */
  function setSidebarCollapsed(value: boolean): void {
    sidebarCollapsed.value = value
    writePersistedSidebarCollapsed(value)
  }

  /** 设置页面标题（document.title 由路由守卫统一设置，这里只存状态） */
  function setDocumentTitle(value: string): void {
    documentTitle.value = value
  }

  return {
    theme,
    language,
    sidebarCollapsed,
    documentTitle,
    initTheme,
    setTheme,
    resetTheme,
    setLanguage,
    resetLanguage,
    toggleSidebar,
    setSidebarCollapsed,
    setDocumentTitle
  }
})
