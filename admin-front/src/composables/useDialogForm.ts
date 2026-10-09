import { ElMessage } from 'element-plus'
import { reactive, ref } from 'vue'

import { translate } from '@/locales'

/**
 * 弹窗表单管理组合式函数（任务书 15.4）。
 * 覆盖：打开（新增 / 编辑）、关闭、提交、提交 loading、加载详情。
 *
 * 泛型约束用 `object` 而不是 `Record<string, unknown>`：TypeScript 的 interface 不具备
 * 隐式索引签名，用 Record 约束会导致业务页面里的 interface 无法作为 T 传入。
 */

export type DialogMode = 'create' | 'update'

export interface DialogFormOptions<T extends object> {
  /** 表单默认值工厂（每次调用返回一份全新的默认值），字段名与后端 DTO 保持一致 */
  defaultForm: () => T
  /** 加载详情（编辑时调用） */
  loadDetail?: (id: string) => Promise<Partial<T> | null | undefined>
  /** 新增接口 */
  create?: (data: T) => Promise<unknown>
  /** 编辑接口 */
  update?: (data: T) => Promise<unknown>
  /** 主键字段名，默认 id */
  idKey?: string
  /** 列表行里的主键字段名，默认 id */
  rowIdKey?: string
  /** 新增成功提示的 i18n 键 */
  createSuccessKey?: string
  /** 编辑成功提示的 i18n 键 */
  updateSuccessKey?: string
  /** 提交成功后的回调（通常是刷新列表） */
  onSuccess?: (mode: DialogMode) => void | Promise<void>
  /** 提交失败回调；提供后 submit 不再向上抛错 */
  onError?: (error: unknown) => void
}

/** 读取对象属性（入参为 unknown，避免对泛型做索引访问） */
function readProperty(source: unknown, key: string): unknown {
  if (source === null || source === undefined) {
    return undefined
  }
  return (source as Record<string, unknown>)[key]
}

export function useDialogForm<T extends object>(options: DialogFormOptions<T>) {
  const idKey = options.idKey ?? 'id'
  const rowIdKey = options.rowIdKey ?? 'id'

  const visible = ref(false)
  const isEdit = ref(false)
  const submitting = ref(false)
  const detailLoading = ref(false)
  const form = reactive(options.defaultForm()) as unknown as T

  /** 表单的可写视图（用于按字段名赋值 / 删除） */
  function formRecord(): Record<string, unknown> {
    return form as unknown as Record<string, unknown>
  }

  /** 重置表单为默认值（同时清掉上一次编辑残留的键） */
  function resetForm(): void {
    const defaults = options.defaultForm() as unknown as Record<string, unknown>
    const target = formRecord()
    Object.keys(target).forEach((key) => {
      if (!(key in defaults)) {
        delete target[key]
      }
    })
    Object.assign(target, defaults)
  }

  /**
   * 只回填表单认识的字段，避免把列表行里的额外字段（createTime 等）一起提交给后端。
   * 主键例外：编辑时必须提交主键。
   */
  function assignKnown(source: Partial<T> | Record<string, unknown>): void {
    const defaults = options.defaultForm() as unknown as Record<string, unknown>
    const target = formRecord()
    Object.keys(defaults).forEach((key) => {
      const value = readProperty(source, key)
      if (value !== undefined) {
        target[key] = value
      }
    })
    if (!(idKey in defaults)) {
      const idValue = readProperty(source, idKey)
      if (idValue !== undefined) {
        target[idKey] = idValue
      }
    }
  }

  /** 打开新增弹窗 */
  function openCreate(overrides?: Partial<T>): void {
    resetForm()
    isEdit.value = false
    visible.value = true
    if (overrides) {
      Object.assign(formRecord(), overrides)
    }
  }

  /** 打开编辑弹窗（可选加载详情） */
  async function openEdit(row: Partial<T> | Record<string, unknown>, overrides?: Partial<T>): Promise<void> {
    resetForm()
    isEdit.value = true
    visible.value = true
    assignKnown(row)

    const idValue = readProperty(row, rowIdKey)
    const id = idValue === null || idValue === undefined ? '' : String(idValue)
    if (id && options.loadDetail) {
      detailLoading.value = true
      try {
        const detail = await options.loadDetail(id)
        if (detail) {
          assignKnown(detail)
        }
      } catch {
        // 详情加载失败时保留列表行数据，错误提示已由请求层处理
      } finally {
        detailLoading.value = false
      }
    }

    if (overrides) {
      Object.assign(formRecord(), overrides)
    }
  }

  /** 关闭弹窗并重置表单 */
  function close(): void {
    visible.value = false
    resetForm()
  }

  /**
   * 提交。
   * 成功：提示 → 关闭 → 重置 → 触发 onSuccess（通常刷新列表）→ 返回 true；
   * 失败：提供了 onError 时交给它处理并返回 false（例如把后端 100004 定位到表单项），
   *       未提供 onError 时向上抛错，由调用方处理。
   */
  async function submit(): Promise<boolean> {
    if (submitting.value) {
      return false
    }
    const mode: DialogMode = isEdit.value ? 'update' : 'create'
    const handler = mode === 'update' ? options.update : options.create
    if (!handler) {
      console.warn(`[useDialogForm] ${mode} handler is not configured`)
      return false
    }

    submitting.value = true
    try {
      const payload = { ...formRecord() } as unknown as T
      await handler(payload)
      const successKey =
        mode === 'update'
          ? (options.updateSuccessKey ?? 'common.saveSuccess')
          : (options.createSuccessKey ?? 'common.saveSuccess')
      ElMessage.success(translate(successKey))
      visible.value = false
      resetForm()
      await options.onSuccess?.(mode)
      return true
    } catch (error) {
      if (options.onError) {
        options.onError(error)
        return false
      }
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    visible,
    isEdit,
    submitting,
    detailLoading,
    form,
    openCreate,
    openEdit,
    close,
    submit,
    resetForm
  }
}
