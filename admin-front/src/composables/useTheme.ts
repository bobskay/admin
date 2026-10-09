import { computed } from 'vue'

import { useAppStore } from '@/stores/app'
import { THEME_NAMES, type ThemeName } from '@/utils/constants'

/**
 * 主题管理组合式函数。
 * 切换时同步设置根元素 data-theme 属性与 dark 类（见 stores/app.ts 的 setTheme）。
 */
export function useTheme() {
  const appStore = useAppStore()

  /** 当前主题 */
  const theme = computed(() => appStore.theme)

  /** 可选主题列表 */
  const themes = THEME_NAMES

  /** 主题到词条键的映射（标签文案一律走 i18n） */
  const themeLabelKeys: Record<ThemeName, string> = {
    light: 'layout.themeLight',
    dark: 'layout.themeDark',
    blue: 'layout.themeBlue'
  }

  /** 是否为暗色主题 */
  const isDark = computed(() => appStore.theme === 'dark')

  /** 切换主题 */
  function setTheme(value: ThemeName): void {
    appStore.setTheme(value)
  }

  /** 在当前主题之外循环切换（供快捷键或紧凑控件使用） */
  function nextTheme(): void {
    const index = THEME_NAMES.indexOf(appStore.theme)
    const next = THEME_NAMES[(index + 1) % THEME_NAMES.length]
    appStore.setTheme(next)
  }

  return { theme, themes, themeLabelKeys, isDark, setTheme, nextTheme }
}
