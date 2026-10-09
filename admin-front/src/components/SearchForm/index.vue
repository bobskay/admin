<script setup lang="ts">
import { useI18n } from 'vue-i18n'

/**
 * 查询表单：基于 el-form 的查询区，只提供「查询」「重置」两个操作按钮。
 *
 * 用法：
 *   <SearchForm @search="handleSearch" @reset="handleReset">
 *     <el-form-item :label="t('field.username')"> ... </el-form-item>
 *   </SearchForm>
 *
 * 约定：
 * - 查询 / 重置按钮由本组件提供，页面**不要**再用 #actions 插槽重复放一组；
 * - 条件数量固定为一行自动换行，不再提供展开 / 收起；
 * - 按钮权限用 searchPerm / resetPerm 传入权限标识，留空表示不校验
 *   （v-has 对空值直接放行，超管同样放行）。
 */
defineOptions({ name: 'SearchForm' })

const props = withDefaults(
  defineProps<{
    /** 是否显示操作按钮区 */
    showActions?: boolean
    /** 重置按钮的 i18n 键 */
    resetTextKey?: string
    /** 查询按钮的 i18n 键 */
    searchTextKey?: string
    /** 查询按钮的权限标识，留空不校验 */
    searchPerm?: string
    /** 重置按钮的权限标识，留空不校验 */
    resetPerm?: string
  }>(),
  {
    showActions: true,
    resetTextKey: 'common.reset',
    searchTextKey: 'common.search',
    searchPerm: '',
    resetPerm: ''
  }
)

const emit = defineEmits<{
  /** 点击查询（或回车） */
  search: []
  /** 点击重置 */
  reset: []
}>()

const { t } = useI18n()

function handleSearch(): void {
  emit('search')
}

function handleReset(): void {
  emit('reset')
}
</script>

<template>
  <div class="search-form">
    <el-form
      class="search-form__form"
      label-width="auto"
      :inline="true"
      @submit.prevent="handleSearch"
      @keyup.enter="handleSearch"
    >
      <!-- 操作按钮与查询条件在同一个 flex 流里：条件排得下就同行，排不下自然换行 -->
      <div class="search-form__body">
        <slot />

        <div v-if="props.showActions" class="search-form__actions">
          <el-button v-has="props.searchPerm" type="primary" @click="handleSearch">
            {{ t(props.searchTextKey) }}
          </el-button>
          <el-button v-has="props.resetPerm" @click="handleReset">
            {{ t(props.resetTextKey) }}
          </el-button>
          <slot name="actions" />
        </div>
      </div>
    </el-form>
  </div>
</template>

<style scoped lang="scss">
.search-form {
  padding: 14px 16px;
  background: var(--admin-bg-card);
  border: 1px solid var(--admin-border-color);
  border-radius: var(--admin-radius-card);
  box-shadow: var(--admin-shadow-card);

  &__form {
    display: flex;
    flex-direction: column;
  }

  &__body {
    display: flex;
    flex-wrap: wrap;
    align-items: flex-start;

    :deep(.el-form-item) {
      margin-right: 12px;
      margin-bottom: 14px;
    }
  }

  /* 与 el-form-item 同高、同下边距，保证按钮和控件在同一基线上 */
  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 32px;
    margin-bottom: 14px;
  }
}
</style>
