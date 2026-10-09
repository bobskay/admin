import { useDebounceFn } from '@vueuse/core'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reactive, ref, type Ref } from 'vue'

import { translate } from '@/locales'
import type { PageQueryParams, PageResult, RequestError, TableSortPayload } from '@/types'
import {
  CODE_REQUEST_CANCELED,
  DEFAULT_PAGE_NUM,
  DEFAULT_PAGE_SIZE,
  SEARCH_DEBOUNCE
} from '@/utils/constants'

/**
 * 表格数据管理组合式函数（任务书 15.4）。
 * 覆盖：分页、查询、排序、刷新、重置、删除后回退页码、并发请求取消。
 *
 * 泛型约束用 `object` 而不是 `Record<string, unknown>`：TypeScript 的 interface 不具备
 * 隐式索引签名，用 Record 约束会导致业务页面里的 interface 无法作为 T 传入。
 */

/** 查询条件 = 业务条件 + 分页公共字段 */
export type TableQuery<Q> = Q & PageQueryParams

/** 表格排序变化载荷（对应 DataTable 的 sort-change 事件） */
export type SortChangePayload = TableSortPayload

/** 删除操作的可选项 */
export interface DeleteOptions {
  /** 指定要删除的 id；不传时使用表格选中的行 */
  ids?: string[]
  /** 删除接口；不传时使用 useTable 的 remove 选项 */
  remove?: (ids: string[]) => Promise<unknown>
  /** 自定义确认文案（已翻译的整句，例如带名称的确认） */
  confirmText?: string
  /** 确认文案的 i18n 键 */
  confirmKey?: string
  /** 成功提示的 i18n 键 */
  successKey?: string
}

export interface UseTableOptions<T, Q extends object> {
  /** 列表接口 */
  fetch: (params: TableQuery<Q>) => Promise<PageResult<T>>
  /** 初始查询条件 */
  defaultQuery?: Partial<Q>
  /** 初始每页条数 */
  pageSize?: number
  /** 行主键字段名，默认 id */
  rowKey?: string
  /** 是否创建后立即加载，默认 true */
  immediate?: boolean
  /** 删除接口（供 handleDelete 使用） */
  remove?: (ids: string[]) => Promise<unknown>
  /** 数据加载完成回调 */
  onLoaded?: (rows: T[]) => void
}

/** 读取对象属性（入参为 unknown，避免对泛型做索引访问） */
function readProperty(source: unknown, key: string): unknown {
  if (source === null || source === undefined) {
    return undefined
  }
  return (source as Record<string, unknown>)[key]
}

export function useTable<T extends object, Q extends object = Record<string, unknown>>(
  options: UseTableOptions<T, Q>
) {
  const rowKey = options.rowKey ?? 'id'
  const loading = ref(false)
  const rows = ref<T[]>([]) as Ref<T[]>
  /** 总条数：已由字符串转为数字 */
  const total = ref(0)
  const selectedRows = ref<T[]>([]) as Ref<T[]>
  const selectedIds = ref<string[]>([])

  function createQuery(): TableQuery<Q> {
    return {
      pageNum: DEFAULT_PAGE_NUM,
      pageSize: options.pageSize ?? DEFAULT_PAGE_SIZE,
      sortField: null,
      sortOrder: null,
      ...(options.defaultQuery ?? {})
    } as TableQuery<Q>
  }

  const query = reactive(createQuery()) as TableQuery<Q>

  /** 查询条件的可写视图（用于重置） */
  function queryRecord(): Record<string, unknown> {
    return query as unknown as Record<string, unknown>
  }

  /**
   * 请求序号：只应用最后一次请求的响应。
   * 与请求层的 AbortController 去重配合，双重保证「旧响应不覆盖新结果」。
   */
  let requestSeq = 0

  /** 取行主键（后端 Long 一律按字符串处理） */
  function pickId(row: unknown): string {
    const value = readProperty(row, rowKey)
    return value === null || value === undefined ? '' : String(value)
  }

  /** 加载数据 */
  async function loadData(): Promise<void> {
    const seq = ++requestSeq
    loading.value = true
    try {
      const result = await options.fetch({ ...query } as TableQuery<Q>)
      if (seq !== requestSeq) {
        return
      }
      rows.value = result?.list ?? []
      // 总条数是字符串（后端 Long 序列化规则），必须先转数字再交给分页组件
      const parsed = Number(result?.total ?? 0)
      total.value = Number.isFinite(parsed) ? parsed : 0
      options.onLoaded?.(rows.value)
    } catch (error) {
      if (seq !== requestSeq) {
        return
      }
      const requestError = error as RequestError | null
      // 被并发去重取消的请求静默处理
      if (requestError?.canceled || requestError?.code === CODE_REQUEST_CANCELED) {
        return
      }
      rows.value = []
      total.value = 0
    } finally {
      if (seq === requestSeq) {
        loading.value = false
      }
    }
  }

  /** 刷新（保持当前页码） */
  async function refresh(): Promise<void> {
    await loadData()
  }

  /** 查询：回到第一页 */
  async function handleSearch(): Promise<void> {
    query.pageNum = DEFAULT_PAGE_NUM
    await loadData()
  }

  /**
   * 防抖查询（任务书 16.7：输入型查询条件变化时做 300ms 防抖）。
   * 输入框联动查询时用它，不要自己再实现一遍防抖（VueUse 已提供）。
   */
  const debouncedSearch = useDebounceFn(handleSearch, SEARCH_DEBOUNCE)

  /** 重置查询条件：恢复初始条件并回到第一页 */
  async function handleReset(): Promise<void> {
    const defaults = createQuery() as unknown as Record<string, unknown>
    const target = queryRecord()
    Object.keys(defaults).forEach((key) => {
      target[key] = defaults[key]
    })
    await loadData()
  }

  /** 排序变化 */
  async function handleSortChange(payload: SortChangePayload): Promise<void> {
    if (payload?.order && payload.prop) {
      query.sortField = payload.prop
      query.sortOrder = payload.order === 'ascending' ? 'asc' : 'desc'
    } else {
      query.sortField = null
      query.sortOrder = null
    }
    query.pageNum = DEFAULT_PAGE_NUM
    await loadData()
  }

  /** 页码变化 */
  async function handlePageChange(page: number): Promise<void> {
    query.pageNum = page
    await loadData()
  }

  /** 每页条数变化：回到第一页 */
  async function handleSizeChange(size: number): Promise<void> {
    query.pageSize = size
    query.pageNum = DEFAULT_PAGE_NUM
    await loadData()
  }

  /** 行选择变化 */
  function handleSelectionChange(selection: T[]): void {
    selectedRows.value = selection ?? []
    selectedIds.value = (selection ?? []).map((row) => pickId(row)).filter((id) => id !== '')
  }

  /** 清空选择 */
  function clearSelection(): void {
    selectedRows.value = []
    selectedIds.value = []
  }

  /**
   * 删除：二次确认 → 调接口 → 删除后回退页码 → 重新加载。
   * 用法：`await handleDelete(deleteUser)` 或 `await handleDelete({ remove: deleteUser, confirmText })`
   */
  async function handleDelete(
    removeOrOptions?: ((ids: string[]) => Promise<unknown>) | DeleteOptions
  ): Promise<boolean> {
    const opts: DeleteOptions =
      typeof removeOrOptions === 'function' ? { remove: removeOrOptions } : (removeOrOptions ?? {})
    const removeFn = opts.remove ?? options.remove
    if (!removeFn) {
      console.warn('[useTable] handleDelete: no remove handler configured')
      return false
    }

    const ids = opts.ids && opts.ids.length > 0 ? opts.ids : selectedIds.value
    if (ids.length === 0) {
      ElMessage.warning(translate('common.selectRequired'))
      return false
    }

    const confirmText =
      opts.confirmText ??
      (opts.confirmKey
        ? translate(opts.confirmKey)
        : ids.length > 1
          ? translate('common.batchDeleteConfirm', { count: ids.length })
          : translate('common.deleteConfirm'))

    try {
      await ElMessageBox.confirm(confirmText, translate('common.tip'), {
        confirmButtonText: translate('common.confirm'),
        cancelButtonText: translate('common.cancel'),
        type: 'warning'
      })
    } catch {
      // 用户取消
      return false
    }

    try {
      await removeFn(ids)
      ElMessage.success(
        opts.successKey ? translate(opts.successKey) : translate('common.deleteSuccess')
      )
      // 删除最后一页最后一条时自动回退页码
      const currentPage = Number(query.pageNum ?? DEFAULT_PAGE_NUM)
      if (rows.value.length <= ids.length && currentPage > DEFAULT_PAGE_NUM) {
        query.pageNum = currentPage - 1
      }
      clearSelection()
      await loadData()
      return true
    } catch {
      // 失败提示已由请求层统一处理
      return false
    }
  }

  if (options.immediate !== false) {
    void loadData()
  }

  return {
    loading,
    rows,
    total,
    query,
    selectedRows,
    selectedIds,
    loadData,
    refresh,
    handleSearch,
    debouncedSearch,
    handleReset,
    handleSortChange,
    handlePageChange,
    handleSizeChange,
    handleSelectionChange,
    clearSelection,
    handleDelete
  }
}
