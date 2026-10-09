import { MENU_API, rulesMenu } from '@/api/menu'
import type { ValidationRuleItem } from '@/types'

import enUSField from './en-US/field'
import enUSValidation from './en-US/validation'
import zhCNError from './zh-CN/error'
import zhCNField from './zh-CN/field'
import zhCNValidation from './zh-CN/validation'

/**
 * 开发期词条比对（任务书 7.3）。
 * 拉一次后端校验规则清单，与本地 field.* / validation.* 词条比对，缺哪条就 warn。
 * 生产环境不请求、不输出。
 */

function warnMissing(key: string): void {
  console.warn(`missing i18n key: ${key}`)
}

/** 比对一份语言的 field / validation 词条 */
function checkLanguage(
  language: string,
  fieldMessages: Record<string, string>,
  validationMessages: Record<string, string>,
  rules: ValidationRuleItem[]
): void {
  rules.forEach((rule) => {
    const fieldKey = rule.labelKey || `field.${rule.field}`
    if (!fieldMessages[rule.field]) {
      warnMissing(`${fieldKey} (${language})`)
    }
    if (!validationMessages[rule.reason]) {
      warnMissing(`validation.${rule.reason} (${language})`)
    }
  })
}

/** 执行比对；任何异常都不影响应用启动 */
export async function checkLocaleKeys(): Promise<void> {
  if (!import.meta.env.DEV) {
    return
  }
  try {
    const rules = await rulesMenu()
    if (!Array.isArray(rules) || rules.length === 0) {
      return
    }
    checkLanguage(
      'zh-CN',
      zhCNField as Record<string, string>,
      zhCNValidation as Record<string, string>,
      rules
    )
    checkLanguage(
      'en-US',
      enUSField as Record<string, string>,
      enUSValidation as Record<string, string>,
      rules
    )
    if (!(zhCNError as Record<string, string>)['100004']) {
      warnMissing('error.100004 (zh-CN)')
    }
  } catch {
    // 开发期比对失败不阻断启动（后端可能尚未启动）
    console.warn(`locale rules check skipped: ${MENU_API.rules} unavailable`)
  }
}
