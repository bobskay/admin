import { createI18n } from 'vue-i18n'

import { DEFAULT_LANGUAGE, type LanguageCode } from '@/utils/constants'
import { applyLanguageToDocument, readPersistedLanguage } from '@/utils/storage'

import enUSCommon from './en-US/common'
import enUSDashboard from './en-US/dashboard'
import enUSError from './en-US/error'
import enUSField from './en-US/field'
import enUSLayout from './en-US/layout'
import enUSLog from './en-US/log'
import enUSLogin from './en-US/login'
import enUSMenu from './en-US/menu'
import enUSRole from './en-US/role'
import enUSUser from './en-US/user'
import enUSValidation from './en-US/validation'
import zhCNCommon from './zh-CN/common'
import zhCNDashboard from './zh-CN/dashboard'
import zhCNError from './zh-CN/error'
import zhCNField from './zh-CN/field'
import zhCNLayout from './zh-CN/layout'
import zhCNLog from './zh-CN/log'
import zhCNLogin from './zh-CN/login'
import zhCNMenu from './zh-CN/menu'
import zhCNRole from './zh-CN/role'
import zhCNUser from './zh-CN/user'
import zhCNValidation from './zh-CN/validation'
import type { FieldErrorItem } from '@/types'

/** 中文词条：按模块拆分后汇总，键名结构与英文完全对齐 */
export const zhCN = {
  common: zhCNCommon,
  login: zhCNLogin,
  layout: zhCNLayout,
  dashboard: zhCNDashboard,
  user: zhCNUser,
  role: zhCNRole,
  menu: zhCNMenu,
  log: zhCNLog,
  field: zhCNField,
  validation: zhCNValidation,
  error: zhCNError
}

/** 英文词条 */
export const enUS = {
  common: enUSCommon,
  login: enUSLogin,
  layout: enUSLayout,
  dashboard: enUSDashboard,
  user: enUSUser,
  role: enUSRole,
  menu: enUSMenu,
  log: enUSLog,
  field: enUSField,
  validation: enUSValidation,
  error: enUSError
}

/** i18n 实例：默认中文，语言取自本地存储（防闪烁） */
const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: readPersistedLanguage(),
  fallbackLocale: DEFAULT_LANGUAGE,
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export default i18n

/** 判断词条是否存在（缺失时不能直接 t，否则控制台会刷警告） */
export function hasMessage(key: string): boolean {
  return i18n.global.te(key)
}

/** 统一取词条：缺失时回显键名，便于开发期发现漏配 */
export function translate(key: string, named?: Record<string, unknown>): string {
  if (!hasMessage(key)) {
    return key
  }
  return named ? i18n.global.t(key, named) : i18n.global.t(key)
}

/** 当前语言 */
export function getLocale(): LanguageCode {
  return i18n.global.locale.value as LanguageCode
}

/** 切换语言：同步 html lang 属性 */
export function setLocale(language: LanguageCode): void {
  i18n.global.locale.value = language
  applyLanguageToDocument(language)
}

/** 按错误码取文案，缺失时用通用未知错误文案兜底 */
export function resolveErrorMessage(code: number | string): string {
  const key = `error.${code}`
  return hasMessage(key) ? translate(key) : translate('error.unknown')
}

/**
 * 中英文的「字段标签 + 校验原因」连接方式：
 * 中文直接拼接（用户名 + 不能为空），英文用空格连接（Username + is required）。
 */
function joinLabelAndReason(label: string, reason: string): string {
  return getLocale().startsWith('zh') ? `${label}${reason}` : `${label} ${reason}`
}

/**
 * 按任务书 19.2 的顺序拼装参数校验错误文案，取到即停：
 * 1. validation.{field}.{reason}  整句特例
 * 2. field.{field} + validation.{reason}
 * 3. error.100004
 * 4. 通用未知错误文案
 */
export function resolveValidationMessage(
  field: string | null | undefined,
  reason: string | null | undefined
): string {
  const fieldName = (field ?? '').trim()
  const reasonName = (reason ?? '').trim()

  if (fieldName && reasonName) {
    // 1. 整句特例
    const specificKey = `validation.${fieldName}.${reasonName}`
    if (hasMessage(specificKey)) {
      return translate(specificKey)
    }
    // 2. 常规路径：字段标签 + 校验原因
    const fieldKey = `field.${fieldName}`
    const reasonKey = `validation.${reasonName}`
    if (hasMessage(fieldKey) && hasMessage(reasonKey)) {
      return joinLabelAndReason(translate(fieldKey), translate(reasonKey))
    }
  }

  // 3. 兜底
  if (hasMessage('error.100004')) {
    return translate('error.100004')
  }
  // 4. 通用未知
  return translate('error.unknown')
}

/** 把后端返回的校验错误对象转成可展示文案 */
export function resolveFieldError(data: FieldErrorItem | null | undefined): string {
  if (!data) {
    return resolveErrorMessage(100004)
  }
  return resolveValidationMessage(data.field, data.reason)
}

/**
 * 表单校验消息转文案。
 * yup 规则里的 message 写的是 i18n 键：既可能是 `validation.{reason}`（走与后端一致的拼装规则），
 * 也可能是完整键名（直接翻译）。两者都不是时原样返回。
 */
export function resolveFormMessage(field: string, raw: string | undefined): string {
  const message = (raw ?? '').trim()
  if (!message) {
    return ''
  }
  const reasonMatch = /^validation\.(required|len|pattern|email|range|type)$/.exec(message)
  if (reasonMatch) {
    return resolveValidationMessage(field, reasonMatch[1])
  }
  if (hasMessage(message)) {
    return translate(message)
  }
  return message
}
