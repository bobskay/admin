<script setup lang="ts" generic="T extends object">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import type { TableColumn, TableSortPayload } from '@/types'
import { DEFAULT_PAGE_NUM, DEFAULT_PAGE_SIZE } from '@/utils/constants'

/**
 * 数据表格：基于 el-table，内置加载态、空态、分页、工具栏插槽、列配置、行选择、排序。
 *
 * 用法：
 *   <DataTable
 *     :data="rows" :loading="loading" :columns="columns" :total="total"
 *     v-model:page="query.pageNum" v-model:size="query.pageSize"
 *     row-key="id" show-selection
 *     @selection-change="handleSelectionChange" @sort-change="handleSortChange">
 *     <template #toolbar> ... </template>
 *     <template #status="{ row }"> ... </template>
 *   </DataTable>
 *
 * 注意：
 * - total 在后端是**字符串**，本组件内部统一 Number() 后再交给分页组件；
 * - 泛型约束用 object 而非 Record<string, unknown>，否则业务侧用 interface 声明的行类型无法传入。
 */
defineOptions({ name: 'DataTable' })

const props = withDefaults(
  defineProps<{
    /** 行数据 */
    data?: T[]
    /** 列配置 */
    columns?: TableColumn<T>[]
    /** 加载态 */
    loading?: boolean
    /** 总条数（字符串或数字都接受） */
    total?: number | string
    /** 当前页 */
    page?: number
    /** 每页条数 */
    size?: number
    /** 行主键字段名 */
    rowKey?: string
    /** 是否显示选择列 */
    showSelection?: boolean
    /** 是否显示分页 */
    showPagination?: boolean
    /** 表格高度 */
    height?: string | number
    /** 每页条数候选 */
    pageSizes?: number[]
    /**
     * 树形表格的子节点字段名。传入后本组件的表格变为树形表格
     * （必须同时保证行数据里存在该字段，见菜单管理页）。
     */
    treePropsChildren?: string
    /** 树形表格默认展开的行（配合 :expand-keys 使用） */
    defaultExpandAll?: boolean
    /** 树形表格当前展开的行主键（v-model:expandKeys） */
    expandKeys?: Array<string | number>
    /** 树形表格行缩进（像素） */
    indent?: number
  }>(),
  {
    // 布尔 prop 未传时 Vue 会按 false 处理，所以「默认显示」必须显式声明
    showPagination: true
  }
)

const emit = defineEmits<{
  'update:page': [value: number]
  'update:size': [value: number]
  'update:expandKeys': [value: Array<string | number>]
  'sort-change': [payload: TableSortPayload]
  'selection-change': [rows: T[]]
  'expand-change': [row: T, expanded: boolean]
}>()

const { t } = useI18n()

const tableData = computed<T[]>(() => props.data ?? [])
const tableColumns = computed<TableColumn<T>[]>(() => props.columns ?? [])
const loadingState = computed(() => props.loading === true)
const rowKeyName = computed(() => props.rowKey ?? 'id')
const selectionVisible = computed(() => props.showSelection === true)
const paginationVisible = computed(() => props.showPagination === true)
const currentPage = computed(() => props.page ?? DEFAULT_PAGE_NUM)
const pageSize = computed(() => props.size ?? DEFAULT_PAGE_SIZE)
const pageSizesList = computed(() => props.pageSizes ?? [10, 20, 50, 100])

/** 树形表格配置：未传 treePropsChildren 时返回 undefined，保持普通表格行为 */
const tableTreeProps = computed(() =>
  props.treePropsChildren ? { children: props.treePropsChildren } : undefined
)

/** 树形表格展开行（受控） */
/**
 * 树形表格展开行（受控）。
 * el-table 的 expand-row-keys 只声明了 string[]，而业务键可能是数字，
 * 这里只做类型断言、不做运行期转换，保证与 row-key 的严格相等比较仍然成立。
 */
const expandRowKeys = computed<string[]>(() => (props.expandKeys ?? []) as string[])

/** 总条数：字符串先转数字，非法值按 0 处理 */
const numericTotal = computed(() => {
  const parsed = Number(props.total ?? 0)
  return Number.isFinite(parsed) ? parsed : 0
})

/** 列的表头文案：优先 labelKey（走 i18n），其次 label（数据文案） */
function columnLabel(column: TableColumn<T>): string {
  if (column.labelKey) {
    return t(column.labelKey)
  }
  return column.label ?? column.prop
}

/** 传给 el-table-column 的属性集合（避免在模板里写一堆条件属性） */
interface ColumnAttrs {
  prop: string
  label: string
  width?: string | number
  minWidth?: string | number
  align: 'left' | 'center' | 'right'
  sortable: 'custom' | false
  fixed?: boolean | 'left' | 'right'
  showOverflowTooltip: boolean
  className?: string
}

function columnAttrs(column: TableColumn<T>): ColumnAttrs {
  return {
    prop: column.prop,
    label: columnLabel(column),
    width: column.width,
    minWidth: column.minWidth,
    align: column.align ?? 'left',
    // 服务端排序：交给 sort-change 事件处理，不让 el-table 本地排序
    sortable: column.sortable ? 'custom' : false,
    fixed: column.fixed,
    showOverflowTooltip: column.showOverflowTooltip !== false,
    // 操作列靠这个类名拿到 cell-actions，从而允许按钮换行
    className: column.cellClassName
  }
}

/** 取单元格原始值，支持 a.b 形式的路径（入参用 unknown，避免对泛型做索引访问） */
function readProperty(source: unknown, prop: string): unknown {
  if (source === null || source === undefined) {
    return undefined
  }
  if (!prop.includes('.')) {
    return (source as Record<string, unknown>)[prop]
  }
  return prop.split('.').reduce<unknown>((acc, key) => {
    if (acc && typeof acc === 'object') {
      return (acc as Record<string, unknown>)[key]
    }
    return undefined
  }, source)
}

/** 是否需要自定义单元格渲染（插槽或格式化函数） */
function hasCustomCell(column: TableColumn<T>): boolean {
  return Boolean(column.slot) || typeof column.formatter === 'function'
}

/** 格式化单元格 */
function formatCell(column: TableColumn<T>, row: T, index: number): string {
  if (typeof column.formatter === 'function') {
    return column.formatter(row, readProperty(row, column.prop), index)
  }
  const value = readProperty(row, column.prop)
  return value === null || value === undefined ? '' : String(value)
}

function handleSortChange(payload: { prop?: string | null; order?: string | null }): void {
  const order = payload?.order === 'ascending' || payload?.order === 'descending' ? payload.order : null
  emit('sort-change', { prop: order ? (payload.prop ?? null) : null, order })
}

function handleSelectionChange(rows: T[]): void {
  emit('selection-change', rows ?? [])
}

function handlePageChange(value: number): void {
  emit('update:page', value)
}

function handleSizeChange(value: number): void {
  emit('update:size', value)
}

/** 树形表格：展开状态变化时同步给父组件 */
function handleExpandChange(row: T, expandedRows: T[]): void {
  const keys = (expandedRows ?? [])
    .map((item) => readProperty(item, rowKeyName.value))
    .filter((value): value is string | number => typeof value === 'string' || typeof value === 'number')
  emit('update:expandKeys', keys)
  emit('expand-change', row, expandedRows.some((item) => item === row))
}
</script>

<template>
  <div class="data-table">
    <div v-if="$slots.toolbar" class="data-table__toolbar">
      <slot name="toolbar" />
    </div>

    <el-table
      v-loading="loadingState"
      class="data-table__table"
      :data="tableData"
      :row-key="rowKeyName"
      :height="height"
      :tree-props="tableTreeProps"
      :default-expand-all="props.defaultExpandAll === true"
      :expand-row-keys="expandRowKeys"
      :indent="props.indent"
      @sort-change="handleSortChange"
      @selection-change="handleSelectionChange"
      @expand-change="handleExpandChange"
    >
      <el-table-column
        v-if="selectionVisible"
        type="selection"
        width="48"
        align="center"
        reserve-selection
      />

      <el-table-column
        v-for="column in tableColumns"
        :key="column.prop"
        v-bind="columnAttrs(column)"
      >
        <template v-if="hasCustomCell(column)" #default="scope">
          <slot
            v-if="column.slot"
            :name="column.slot"
            :row="scope.row"
            :index="scope.$index"
            :column="column"
          />
          <span v-else>{{ formatCell(column, scope.row, scope.$index) }}</span>
        </template>
      </el-table-column>

      <template #empty>
        <slot name="empty">
          <span class="data-table__empty">{{ t('common.noData') }}</span>
        </slot>
      </template>
    </el-table>

    <div v-if="paginationVisible" class="data-table__pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="pageSizesList"
        :total="numericTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.data-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  background: var(--admin-bg-card);
  border: 1px solid var(--admin-border-color);
  border-radius: var(--admin-radius-card);
  box-shadow: var(--admin-shadow-card);
  // 作为页面容器的 flex 子项时允许收缩，否则表格会把页面撑出横向滚动条
  min-width: 0;
  max-width: 100%;

  &__table {
    width: 100%;
  }

  &__toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__empty {
    color: var(--admin-text-secondary);
  }

  &__pagination {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    row-gap: 8px;
  }
}
</style>
