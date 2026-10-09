<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import { LOG_API, pageOperationLog } from '@/api/log'
import DataTable from '@/components/DataTable/index.vue'
import DialogForm from '@/components/DialogForm/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/composables/useTable'
import type { SysOperationLogInfo, SysOperationLogPageParams, TableColumn } from '@/types'
import { formatDateTime } from '@/utils/date'

/**
 * 操作日志（只读页面）。
 * - 数据来自 /admin/system/syslog/page，写入侧是 admin-log 的异步审计切面，本页不提供任何修改入口；
 * - 查询按钮的权限标识通过 SearchForm 的 search-perm 传入，页面不再自放一组按钮；
 * - 请求参数与异常信息可能很长，列表不铺开，只在详情弹窗里展示。
 */
defineOptions({ name: 'SystemLogPage' })

const { t } = useI18n()

/** 常用返回码候选：200 为成功，其余取码表里最常出现的失败码 */
const RESULT_CODE_OPTIONS = [200, 100004, 200003, 200007, 100008]

const {
  loading,
  rows,
  total,
  query,
  handleSearch,
  handleReset,
  handlePageChange,
  handleSizeChange,
  handleSortChange
} = useTable<SysOperationLogInfo, SysOperationLogPageParams>({
  fetch: pageOperationLog,
  defaultQuery: { username: '', module: '', action: '', resultCode: undefined },
  rowKey: 'id'
})

/** 表格列定义（只给最小宽度；操作列挂 cell-actions，按钮平铺、放不下时换行） */
const columns: TableColumn<SysOperationLogInfo>[] = [
  { prop: 'username', labelKey: 'field.username', minWidth: 110, slot: 'operator' },
  { prop: 'module', labelKey: 'field.module', minWidth: 90 },
  { prop: 'action', labelKey: 'field.action', minWidth: 90 },
  { prop: 'method', labelKey: 'field.method', width: 84, align: 'center', slot: 'method' },
  { prop: 'uri', labelKey: 'field.uri', minWidth: 160 },
  {
    prop: 'resultCode',
    labelKey: 'field.resultCode',
    width: 96,
    align: 'center',
    sortable: true,
    slot: 'resultCode'
  },
  {
    prop: 'costMs',
    labelKey: 'field.costMs',
    width: 96,
    align: 'right',
    sortable: true,
    formatter: (row) => formatCost(row.costMs)
  },
  { prop: 'ip', labelKey: 'field.ip', minWidth: 100 },
  {
    prop: 'createTime',
    labelKey: 'common.createTime',
    minWidth: 140,
    sortable: true,
    formatter: (row) => formatDateTime(row.createTime)
  },
  {
    prop: 'ops',
    labelKey: 'common.action',
    width: 76,
    align: 'right',
    fixed: 'right',
    showOverflowTooltip: false,
    cellClassName: 'cell-actions',
    slot: 'ops'
  }
]

/** 返回码下拉候选：码 + 码表文案 */
const resultCodeOptions = computed(() =>
  RESULT_CODE_OPTIONS.map((code) => ({
    value: code,
    label: `${code} · ${t(`error.${code}`)}`
  }))
)

/** 返回码是否成功（只有 200 算成功） */
function isSuccess(row: SysOperationLogInfo): boolean {
  return Number(row.resultCode) === 200
}

/** 耗时展示：Long 以字符串返回，空值显示占位而不是 0 */
function formatCost(value: string | null | undefined): string {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return `${value} ms`
}

/* ---------------------------------------------------------------- 详情 */

const detailVisible = ref(false)
const detailRow = ref<SysOperationLogInfo | null>(null)

function openDetail(row: SysOperationLogInfo): void {
  detailRow.value = row
  detailVisible.value = true
}

/** 详情里统一把空值显示为占位符 */
function orEmpty(value: string | null | undefined): string {
  return value === null || value === undefined || value === '' ? t('log.empty') : value
}
</script>

<template>
  <PageContainer>
    <SearchForm :search-perm="LOG_API.page" @search="handleSearch" @reset="handleReset">
      <el-form-item :label="t('field.username')">
        <el-input
          v-model="query.username"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.username') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.module')">
        <el-input
          v-model="query.module"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.module') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.action')">
        <el-input
          v-model="query.action"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.action') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.resultCode')">
        <el-select
          v-model="query.resultCode"
          clearable
          class="filter-control"
          :placeholder="t('log.resultCodePlaceholder')"
        >
          <el-option
            v-for="item in resultCodeOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </el-form-item>
    </SearchForm>

    <DataTable
      :page="query.pageNum"
      :size="query.pageSize"
      :data="rows"
      :loading="loading"
      :columns="columns"
      :total="total"
      row-key="id"
      @update:page="handlePageChange"
      @update:size="handleSizeChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar>
        <el-button v-has="LOG_API.page" @click="handleSearch">
          {{ t('common.refresh') }}
        </el-button>
        <span class="toolbar-tip">{{ t('common.totalCount', { total }) }}</span>
      </template>

      <template #operator="{ row }">
        <span v-if="row.username">{{ row.username }}</span>
        <span v-else class="text-secondary">{{ t('log.anonymous') }}</span>
      </template>

      <template #method="{ row }">
        <el-tag size="small" effect="plain">{{ row.method || '-' }}</el-tag>
      </template>

      <template #resultCode="{ row }">
        <el-tag size="small" :type="isSuccess(row) ? 'success' : 'danger'" effect="plain">
          {{ row.resultCode ?? '-' }}
        </el-tag>
      </template>

      <template #ops="{ row }">
        <el-button link type="primary" @click="openDetail(row)">
          {{ t('log.detail') }}
        </el-button>
      </template>
    </DataTable>

    <!-- 详情：只读展示，底部按钮文案用「关闭」 -->
    <DialogForm
      v-model="detailVisible"
      :title="t('log.detailTitle')"
      width="720px"
      confirm-text-key="common.close"
      @submit="detailVisible = false"
    >
      <el-descriptions v-if="detailRow" :column="2" border>
        <el-descriptions-item :label="t('field.username')">
          {{ orEmpty(detailRow.username) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.module')">
          {{ orEmpty(detailRow.module) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.action')">
          {{ orEmpty(detailRow.action) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.method')">
          {{ orEmpty(detailRow.method) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.uri')" :span="2">
          {{ orEmpty(detailRow.uri) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.resultCode')">
          {{ detailRow.resultCode ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.costMs')">
          {{ formatCost(detailRow.costMs) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.ip')">
          {{ orEmpty(detailRow.ip) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.createTime')">
          {{ formatDateTime(detailRow.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('log.params')" :span="2">
          <pre class="detail-text">{{ orEmpty(detailRow.params) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item
          v-if="detailRow.errorMsg"
          :label="t('log.errorMsg')"
          :span="2"
        >
          <pre class="detail-text detail-text--error">{{ detailRow.errorMsg }}</pre>
        </el-descriptions-item>
        <el-descriptions-item :label="t('log.userAgent')" :span="2">
          <span class="detail-agent">{{ orEmpty(detailRow.userAgent) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </DialogForm>
  </PageContainer>
</template>

<style scoped lang="scss">
.filter-control {
  /* 不写死宽度：由选中项 / placeholder 的文字长度决定 */
  width: auto;
  min-width: 130px;
}

.toolbar-tip {
  font-size: 12px;
  color: var(--admin-text-secondary);
}

/* 长文本（请求参数、异常栈）限定高度内滚动，不把弹窗撑破 */
.detail-text {
  max-height: 180px;
  margin: 0;
  overflow: auto;
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 18px;
  white-space: pre-wrap;
  word-break: break-all;

  &--error {
    color: var(--admin-color-danger);
  }
}

.detail-agent {
  font-size: 12px;
  color: var(--admin-text-secondary);
  word-break: break-all;
}
</style>
