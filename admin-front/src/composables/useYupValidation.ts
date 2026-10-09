import { ElMessage, type FormInstance } from 'element-plus'
import { nextTick, reactive, ref } from 'vue'
import type { AnyObjectSchema, ValidationError } from 'yup'

import { resolveFieldError, resolveFormMessage } from '@/locales'
import type { FieldErrorItem, RequestError } from '@/types'
import { CODE_REQUEST_CANCELED, CODE_VALIDATION_FAILED } from '@/utils/constants'

/**
 * yup 表单校验（补齐 useFormValidation 不方便覆盖的两类场景）：
 *
 * 1. **弹窗表单**的 schema 随模式变化（新增要密码与角色、编辑不要），
 *    写成函数即可按当前模式动态取用；
 * 2. 提交入参与表单值的字段名偶尔不一致（例如确认密码），需要「按字段收集值」再提交，
 *    不能整份表单对象直接发出去。
 *
 * 与 useFormValidation 的约定完全一致：
 * - yup 的 message 写 `validation.{reason}`（经 resolveFormMessage 按 field.{字段} 拼装）；
 * - 错误通过 el-form-item 的 :error 就地展示红字；
 * - 字段名与后端 DTO 字段名逐字一致（否则后端 100004 的 field 无法定位）；
 * - 后端返回 100004 时用 applyRequestError 按 data.field 定位控件，不额外弹全局提示。
 */

/** 字段错误映射 */
export type FieldErrorMap = Record<string, string>

/** 取第一个错误文案（yup 的 path 可能是数组） */
function firstPath(error: ValidationError): { field: string; message: string } {
  const rawPath = error.path
  const top = Array.isArray(rawPath) ? rawPath[0] : rawPath
  const field = top === undefined || top === null ? '' : String(top)
  const rawMessage = error.errors[0] ?? ''
  return { field, message: resolveFormMessage(field, rawMessage) }
}

/**
 * 把 yup 的 ValidationError 列表转成「字段 → 可展示文案」的映射。
 * 所有错误都过一遍，因此一次就能把全部红字挂上（便于 scrollToFirstError 定位）。
 */
export function buildFieldErrors(error: unknown): FieldErrorMap {
  const result: FieldErrorMap = {}
  const validationError = error as ValidationError | null | undefined
  const inner = validationError?.inner
  const list: unknown[] = Array.isArray(inner) && inner.length > 0 ? inner : [error]

  list.forEach((item) => {
    const typed = item as ValidationError | null | undefined
    if (!typed || !Array.isArray(typed.errors) || typed.errors.length === 0) {
      return
    }
    const { field, message } = firstPath(typed)
    if (field && !result[field]) {
      result[field] = message
    }
  })

  return result
}

export function useYupValidation<T extends object>(
  schema: AnyObjectSchema | (() => AnyObjectSchema)
) {
  const formRef = ref<FormInstance>()
  /** 字段错误：键为后端 DTO 字段名，值为已翻译文案 */
  const errors = reactive<FieldErrorMap>({})
  /** 表单值（各弹窗自己的数据结构，与提交 DTO 不一定逐字一致） */
  const form = reactive({}) as T

  /** 取某字段的错误文案（绑定到 el-form-item 的 error 属性） */
  function errorFor(field: string): string {
    return errors[field] ?? ''
  }

  /** 清空全部错误 */
  function clearErrors(): void {
    Object.keys(errors).forEach((key) => {
      delete errors[key]
    })
  }

  /** 写入错误映射（先清空，保证不残留上一次的提示） */
  function applyErrors(map: FieldErrorMap): void {
    clearErrors()
    Object.assign(errors, map)
  }

  /** 取表单实例的根 DOM（兜底定位用） */
  function formElement(): HTMLElement | null {
    const instance = formRef.value as unknown as { $el?: HTMLElement } | undefined
    return instance?.$el ?? null
  }

  /** 表单里是否存在该 prop 的控件（优先用 ElForm 暴露的 fields，失败退化为 DOM 查询） */
  function hasFormField(field: string): boolean {
    const instance = formRef.value as unknown as
      | { fields?: Array<{ prop?: string } | undefined> }
      | undefined
    const fields = instance?.fields
    if (Array.isArray(fields)) {
      return fields.some((item) => item?.prop === field)
    }
    const root = formElement()
    return !!root?.querySelector(`[prop="${field}"]`)
  }

  /** 滚动并聚焦到指定字段 */
  async function scrollToField(field: string): Promise<void> {
    await nextTick()
    const instance = formRef.value
    if (instance && typeof instance.scrollToField === 'function') {
      instance.scrollToField(field)
      return
    }
    const root = formElement()
    const target = root?.querySelector<HTMLElement>(`[prop="${field}"]`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target?.querySelector<HTMLElement>('input, textarea')?.focus()
  }

  /** 滚动到第一个有错误的字段 */
  async function scrollToFirstError(): Promise<void> {
    const first = Object.keys(errors)[0]
    if (first) {
      await scrollToField(first)
    }
  }

  /**
   * 校验一份值。
   * schema 允许传函数，便于按当前模式 / 当前表单值动态取用不同的 schema。
   */
  async function validateValues(values: unknown): Promise<{ valid: boolean; values: T }> {
    try {
      const target = typeof schema === 'function' ? schema() : schema
      const result = (await target.validate(values, {
        abortEarly: false,
        stripUnknown: false
      })) as T
      clearErrors()
      return { valid: true, values: result }
    } catch (error) {
      applyErrors(buildFieldErrors(error))
      await scrollToFirstError()
      return { valid: false, values: values as T }
    }
  }

  /** 校验当前表单值，返回是否通过（提交前可单独调用） */
  async function validateAll(): Promise<boolean> {
    const result = await validateValues({ ...(form as Record<string, unknown>) })
    return result.valid
  }

  /**
   * 提交：读当前表单值 → 校验 → 通过后执行回调。
   * 回调入参是 yup 校验后的值（已按 schema 过滤/转换）。
   */
  async function submit(handler: (values: T) => void | Promise<void>): Promise<boolean> {
    const result = await validateValues({ ...(form as Record<string, unknown>) })
    if (!result.valid) {
      return false
    }
    await handler(result.values)
    return true
  }

  /**
   * 字段值变化：写入表单值并清掉该字段已有的错误。
   * 第二个参数可选——只清错误（不写值）时可以只传字段名。
   */
  function handleFieldChange(field: string, value?: unknown): void {
    if (errors[field]) {
      delete errors[field]
    }
    if (arguments.length > 1) {
      ;(form as Record<string, unknown>)[field] = value
    }
  }

  /** 字段失焦：目前只清错误，保持与 handleFieldChange 一致的行为 */
  function handleFieldBlur(field: string): void {
    if (errors[field]) {
      delete errors[field]
    }
  }

  /** 用新值重置表单（编辑弹窗打开时也用它填充） */
  function resetForm(values?: Partial<T>): void {
    clearErrors()
    const target = form as Record<string, unknown>
    Object.keys(target).forEach((key) => {
      delete target[key]
    })
    if (values) {
      Object.assign(target, values)
    }
  }

  /**
   * 把后端返回的 100004 校验错误对象定位到具体控件。
   * 找不到对应控件时退化为一条页面级提示（任务书 7.1.5），不报错。
   */
  async function applyRequestError(error: unknown): Promise<boolean> {
    const requestError = error as RequestError | null | undefined
    if (
      !requestError ||
      requestError.canceled ||
      requestError.code === CODE_REQUEST_CANCELED ||
      requestError.code !== CODE_VALIDATION_FAILED
    ) {
      return false
    }

    const detail = (requestError.data ?? null) as FieldErrorItem | null
    const message = resolveFieldError(detail)
    const field = (detail?.field ?? '').trim()

    await nextTick()
    if (field && hasFormField(field)) {
      errors[field] = message
      await scrollToField(field)
      return true
    }

    ElMessage.error(message)
    return false
  }

  return {
    formRef,
    form,
    errors,
    errorFor,
    clearErrors,
    applyErrors,
    validateValues,
    validateAll,
    submit,
    handleFieldChange,
    handleFieldBlur,
    resetForm,
    setValues: resetForm,
    scrollToField,
    applyRequestError
  }
}

export default useYupValidation
