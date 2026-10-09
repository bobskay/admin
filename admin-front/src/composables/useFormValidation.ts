import { toTypedSchema } from '@vee-validate/yup'
import { ElMessage, type FormInstance } from 'element-plus'
import { computed, nextTick, reactive, ref } from 'vue'
import { useForm, type TypedSchema } from 'vee-validate'
import type { AnyObjectSchema } from 'yup'

import { resolveFieldError, resolveFormMessage } from '@/locales'
import type { FieldErrorItem, RequestError } from '@/types'
import { CODE_REQUEST_CANCELED, CODE_VALIDATION_FAILED } from '@/utils/constants'

/**
 * 表单校验集成（任务书 15.4）：把 VeeValidate 与 Element Plus 的表单错误展示打通。
 *
 * 用法要点（页面按此写法即可）：
 * - 规则用 yup 定义（见 utils/validate.ts），规则里的 message 写 `validation.{reason}`；
 * - 每个 el-form-item 绑 `:error="errorFor('字段名')"`，字段名必须与后端 DTO 字段名逐字一致；
 * - 字段值变化与失焦时调用 `handleFieldChange` / `handleFieldBlur` 触发校验；
 * - 提交用 `submit(async (values) => { ... })`：内部先统一校验，再执行回调；
 * - 后端返回 100004 时调用 `applyRequestError(error)`，按 data.field 定位控件并就地展示红字。
 *
 * 泛型约束用 `object` 而不是 `Record<string, unknown>`：TypeScript 的 interface 不具备
 * 隐式索引签名，用 Record 约束会导致业务页面里的 interface 无法作为 T 传入。
 */

export interface UseFormValidationOptions<T extends object> {
  /** yup 校验 schema */
  schema: AnyObjectSchema
  /** 表单初始值 */
  initialValues?: Partial<T>
}

/** vee-validate 内部使用的宽松值类型 */
type FormValues = Record<string, unknown>

export function useFormValidation<T extends object>(options: UseFormValidationOptions<T>) {
  const formRef = ref<FormInstance>()

  const formContext = useForm({
    // yup 的 AnyObjectSchema 推断不出具体字段类型，这里断言为宽松值类型（仅类型层面）
    validationSchema: toTypedSchema(options.schema) as unknown as TypedSchema<FormValues>,
    initialValues: (options.initialValues ?? {}) as unknown as FormValues
  })

  const {
    errors: validationErrors,
    meta,
    submitCount,
    validate,
    validateField,
    setFieldValue,
    resetForm,
    setValues
  } = formContext

  /** 表单值（按调用方声明的类型暴露） */
  const values = computed(() => formContext.values as unknown as T)

  /**
   * 翻译后的前端规则错误：VeeValidate 存的是 i18n 键，这里按「field.{field} + validation.{reason}」
   * 拼成完整文案，切换语言时自动跟随。
   */
  const ruleErrors = computed<Record<string, string>>(() => {
    const result: Record<string, string> = {}
    Object.entries(validationErrors.value).forEach(([field, message]) => {
      result[field] = resolveFormMessage(field, message)
    })
    return result
  })

  /**
   * 后端 100004 写入的就地错误。
   * 单独存一份的原因：VeeValidate 在下一次校验时会把内部错误改写回 i18n 键，
   * 若只依赖它，后端返回的文案会在用户再次输入时被前端规则文案顶掉。
   */
  const appliedErrors = reactive<Record<string, string>>({})

  /** 合并后的错误：后端错误优先，其次前端规则错误 */
  const displayErrors = computed<Record<string, string>>(() => ({
    ...ruleErrors.value,
    ...appliedErrors
  }))

  /** 取某字段的错误文案（绑定到 el-form-item 的 error 属性） */
  function errorFor(field: string): string {
    return displayErrors.value[field] ?? ''
  }

  /** 是否存在校验错误 */
  const hasError = computed(() => Object.keys(displayErrors.value).length > 0)

  /** 清掉某字段的后端错误（用户重新输入时调用） */
  function clearAppliedError(field: string): void {
    if (appliedErrors[field]) {
      delete appliedErrors[field]
    }
  }

  /** 取表单根 DOM */
  function formElement(): HTMLElement | null {
    const instance = formRef.value as unknown as { $el?: HTMLElement } | undefined
    return instance?.$el ?? null
  }

  /**
   * 表单里是否存在该 prop 的控件。
   * Element Plus 的 ElForm 暴露了 fields 列表，优先用它；不可用时退化为 DOM 查询。
   * （对内部结构做的是防御式读取，两种途径都不可用时按「控件不存在」处理，走兜底提示。）
   */
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
    // 降级：自行查找并滚动
    const root = formElement()
    const target = root?.querySelector<HTMLElement>(`[prop="${field}"]`)
    target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    target?.querySelector<HTMLElement>('input, textarea')?.focus()
  }

  /** 统一校验（提交前调用）；失败时滚动到第一个错误控件 */
  async function validateAll(): Promise<boolean> {
    const result = await validate()
    if (!result.valid) {
      const firstField = Object.keys(result.errors)[0]
      if (firstField) {
        await scrollToField(firstField)
      }
    }
    return result.valid
  }

  /**
   * 提交：先统一校验，通过后执行回调。
   * 返回是否真正执行了回调（校验不通过时返回 false，回调内抛出的错误会继续向上抛）。
   */
  async function submit(handler: (formValues: T) => void | Promise<void>): Promise<boolean> {
    const valid = await validateAll()
    if (!valid) {
      return false
    }
    await handler(formContext.values as unknown as T)
    return true
  }

  /** 字段值变化：同步值并立即校验该字段 */
  function handleFieldChange(field: string, value: unknown): void {
    clearAppliedError(field)
    setFieldValue(field, value)
    void validateField(field)
  }

  /** 字段失焦：校验该字段 */
  function handleFieldBlur(field: string): void {
    clearAppliedError(field)
    void validateField(field)
  }

  /**
   * 把后端返回的 100004 校验错误对象定位到具体控件。
   * 找不到对应控件时退化为一条页面级提示（任务书 7.1.5），不报错。
   * 返回是否成功定位到控件。
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
      // 存进 appliedErrors：displayErrors 优先使用它，因此不会被后续前端校验覆盖
      appliedErrors[field] = message
      await scrollToField(field)
      return true
    }

    // 兜底：控件不存在时退化为页面级提示
    ElMessage.error(message)
    return false
  }

  /** 用新值填充表单（编辑弹窗打开时使用） */
  function setFormValues(next: Partial<T>): void {
    Object.keys(appliedErrors).forEach((key) => {
      delete appliedErrors[key]
    })
    setValues(next as unknown as Partial<FormValues>)
  }

  /** 重置表单（可带新的初始值） */
  function resetFormValues(next?: Partial<T>): void {
    Object.keys(appliedErrors).forEach((key) => {
      delete appliedErrors[key]
    })
    resetForm({ values: (next ?? options.initialValues ?? {}) as unknown as Partial<FormValues> })
  }

  return {
    formRef,
    form: formContext,
    errors: displayErrors,
    appliedErrors,
    hasError,
    values,
    meta,
    submitCount,
    errorFor,
    validate,
    validateField,
    validateAll,
    submit,
    setFieldValue,
    setValues: setFormValues,
    resetForm: resetFormValues,
    clearAppliedError,
    handleFieldChange,
    handleFieldBlur,
    scrollToField,
    applyRequestError
  }
}
