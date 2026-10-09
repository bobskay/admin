import { boolean, array, number, object, string, type AnyObjectSchema, type StringSchema, type NumberSchema } from 'yup'
import { STATUS, VISIBLE, KEEP_ALIVE, MENU_TYPE } from './constants'

/**
 * yup 通用校验规则。
 *
 * 关键约定：**规则里的 message 一律写 i18n 键**（即 `validation.{reason}`），
 * 由 useFormValidation 在展示时按「field.{field} + validation.{reason}」拼装成完整文案，
 * 这样切换语言无需重建 schema，且与后端 100004 的文案拼装规则完全一致（任务书 19.2）。
 */

/** 受控词表对应的 i18n 键 */
export const VALIDATION_KEYS = {
  required: 'validation.required',
  len: 'validation.len',
  pattern: 'validation.pattern',
  email: 'validation.email',
  range: 'validation.range',
  type: 'validation.type'
} as const

/** 密码：8-32 位，至少一个字母与一个数字 */
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[^\s]{8,32}$/
/** 用户名：字母、数字、下划线、减号，2-50 位 */
export const USERNAME_PATTERN = /^[A-Za-z0-9_-]{2,50}$/
/** 手机号：中国大陆手机号 */
export const PHONE_PATTERN = /^1[3-9]\d{9}$/
/** 路由路径：以斜杠开头 */
export const PATH_PATTERN = /^\//
/** 角色编码：字母数字下划线冒号 */
export const ROLE_CODE_PATTERN = /^[A-Za-z0-9_:]+$/

/** 必填字符串 */
export function requiredString(): StringSchema {
  return string().trim().required(VALIDATION_KEYS.required)
}

/** 可选字符串，可限制最大长度 */
export function optionalString(max?: number): StringSchema {
  const schema = string().trim().defined()
  return max ? schema.max(max, VALIDATION_KEYS.len) : schema
}

/** 用户名 */
export function usernameRule(): StringSchema {
  return string()
    .trim()
    .required(VALIDATION_KEYS.required)
    .max(50, VALIDATION_KEYS.len)
    .matches(USERNAME_PATTERN, VALIDATION_KEYS.pattern)
}

/** 密码：必填 + 8-32 且含字母与数字 */
export function passwordRule(): StringSchema {
  return string()
    .required(VALIDATION_KEYS.required)
    .min(8, VALIDATION_KEYS.len)
    .max(32, VALIDATION_KEYS.len)
    .matches(PASSWORD_PATTERN, VALIDATION_KEYS.pattern)
}

/** 邮箱：可空，填了必须合法 */
export function emailRule(): StringSchema {
  return string().trim().email(VALIDATION_KEYS.email).max(100, VALIDATION_KEYS.len).defined()
}

/** 手机号：可空，填了必须合法 */
export function phoneRule(): StringSchema {
  return string().trim().matches(PHONE_PATTERN, VALIDATION_KEYS.pattern).defined()
}

/** 路由路径：必填且以斜杠开头 */
export function pathRule(): StringSchema {
  return string()
    .trim()
    .required(VALIDATION_KEYS.required)
    .max(200, VALIDATION_KEYS.len)
    .matches(PATH_PATTERN, VALIDATION_KEYS.pattern)
}

/** 组件路径：选填 */
export function componentRule(): StringSchema {
  return string().trim().max(200, VALIDATION_KEYS.len).defined()
}

/** 角色编码 */
export function roleCodeRule(): StringSchema {
  return string()
    .trim()
    .required(VALIDATION_KEYS.required)
    .max(50, VALIDATION_KEYS.len)
    .matches(ROLE_CODE_PATTERN, VALIDATION_KEYS.pattern)
}

/** 昵称：必填，2-50 位 */
export function nicknameRule(): StringSchema {
  return string().trim().required(VALIDATION_KEYS.required).max(50, VALIDATION_KEYS.len)
}

/** 备注 */
export function remarkRule(): StringSchema {
  return string().trim().max(255, VALIDATION_KEYS.len).defined()
}

/** 状态（0 / 1） */
export function statusRule(): NumberSchema {
  return number()
    .required(VALIDATION_KEYS.required)
    .oneOf([STATUS.ENABLED, STATUS.DISABLED], VALIDATION_KEYS.type)
}

/** 菜单类型（1 / 2 / 3） */
export function menuTypeRule(): NumberSchema {
  return number()
    .required(VALIDATION_KEYS.required)
    .oneOf([MENU_TYPE.DIRECTORY, MENU_TYPE.MENU, MENU_TYPE.BUTTON], VALIDATION_KEYS.type)
}

/** 显示状态（0 / 1） */
export function visibleRule(): NumberSchema {
  return number()
    .required(VALIDATION_KEYS.required)
    .oneOf([VISIBLE.SHOW, VISIBLE.HIDE], VALIDATION_KEYS.type)
}

/** 是否缓存（0 / 1） */
export function keepAliveRule(): NumberSchema {
  return number()
    .required(VALIDATION_KEYS.required)
    .oneOf([KEEP_ALIVE.YES, KEEP_ALIVE.NO], VALIDATION_KEYS.type)
}

/** 区间数字（例如排序） */
export function rangeNumberRule(min: number, max: number): NumberSchema {
  return number()
    .required(VALIDATION_KEYS.required)
    .min(min, VALIDATION_KEYS.range)
    .max(max, VALIDATION_KEYS.range)
}

/** 非空字符串数组（例如 roleIds / ids 至少一个） */
export function requiredStringArrayRule() {
  return array().of(string().required(VALIDATION_KEYS.required)).min(1, VALIDATION_KEYS.required)
}

/** 可空字符串数组 */
export function optionalStringArrayRule() {
  return array().of(string()).default([])
}

/** 布尔值（例如「记住用户名」） */
export function booleanRule() {
  return boolean().required(VALIDATION_KEYS.type)
}

/** 空对象 schema，供无校验场景占位使用 */
export function emptySchema(): AnyObjectSchema {
  return object({})
}
