import { computed } from 'vue'

import { useAppStore } from '@/stores/app'
import { LANGUAGE_CODES, type LanguageCode } from '@/utils/constants'

/**
 * 语言管理组合式函数。
 * 切换时同步：本地存储、vue-i18n 的语言、html lang 属性、axios 的 accept-language 请求头。
 * 说明：axios 请求头是在每次请求的拦截器里按当前语言实时写入的（见 utils/request.ts），
 * 因此这里只要改本地存储即可让下一个请求生效。
 */
export function useLocale() {
  const appStore = useAppStore()

  /** 当前语言 */
  const language = computed(() => appStore.language)

  /** 可选语言列表 */
  const languages = LANGUAGE_CODES

  /** 语言到词条键的映射 */
  const languageLabelKeys: Record<LanguageCode, string> = {
    'zh-CN': 'layout.langZhCN',
    'en-US': 'layout.langEnUS'
  }

  /** 切换语言 */
  function setLanguage(value: LanguageCode): void {
    appStore.setLanguage(value)
  }

  /** 中英互换 */
  function toggleLanguage(): void {
    setLanguage(appStore.language === 'zh-CN' ? 'en-US' : 'zh-CN')
  }

  return { language, languages, languageLabelKeys, setLanguage, toggleLanguage }
}
