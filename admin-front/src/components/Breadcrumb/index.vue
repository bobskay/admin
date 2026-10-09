<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

import { usePermissionStore } from '@/stores/permission'
import { HOME_PATH, MENU_TYPE } from '@/utils/constants'
import { findMenuPath } from '@/utils/tree'

/**
 * 面包屑：依据当前路由与菜单树反查祖先链（utils/tree.ts 的 findMenuPath）。
 * 工作台固定作为第一项。
 */
defineOptions({ name: 'Breadcrumb' })

interface Crumb {
  title: string
  /** 可跳转路径；目录没有页面时为空串 */
  path: string
}

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const permissionStore = usePermissionStore()

const crumbs = computed<Crumb[]>(() => {
  const home: Crumb = { title: t('layout.dashboard'), path: HOME_PATH }
  const menuId = route.meta?.menuId

  const chain = menuId ? findMenuPath(permissionStore.menuTree, String(menuId)) : []
  if (chain.length === 0) {
    // 静态路由（工作台、错误页）：直接用 meta.title
    if (route.path === HOME_PATH) {
      return [home]
    }
    const metaTitle = route.meta?.title
    const title = metaTitle
      ? route.meta?.rawTitle
        ? String(metaTitle)
        : t(String(metaTitle))
      : ''
    return title ? [home, { title, path: '' }] : [home]
  }

  const items: Crumb[] = chain
    .filter((node) => node.type === MENU_TYPE.DIRECTORY || node.type === MENU_TYPE.MENU)
    .map((node) => ({
      // 菜单名是数据，直接展示
      title: node.name,
      path: node.type === MENU_TYPE.DIRECTORY ? '' : (node.path ?? '')
    }))

  return [home, ...items]
})

function handleClick(crumb: Crumb, index: number): void {
  // 最后一项是当前页，不跳转
  if (index === crumbs.value.length - 1 || !crumb.path || crumb.path === route.path) {
    return
  }
  void router.push(crumb.path)
}
</script>

<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item v-for="(crumb, index) in crumbs" :key="`${crumb.title}-${index}`">
      <span v-if="!crumb.path || index === crumbs.length - 1" class="breadcrumb__current">
        {{ crumb.title }}
      </span>
      <a v-else class="breadcrumb__link" @click.prevent="handleClick(crumb, index)">
        {{ crumb.title }}
      </a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<style scoped lang="scss">
.breadcrumb {
  display: inline-flex;
  align-items: center;
  font-size: 14px;

  &__current {
    color: var(--admin-text-secondary);
  }

  &__link {
    color: var(--admin-text-primary);
    cursor: pointer;

    &:hover {
      color: var(--admin-color-primary);
    }
  }
}
</style>
