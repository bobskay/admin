<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

import IconRender from '@/components/IconRender/index.vue'
import { usePermissionStore } from '@/stores/permission'
import { HOME_PATH } from '@/utils/constants'
import { buildSidebarTree } from '@/utils/tree'

import SidebarItem from './SidebarItem.vue'

/**
 * 侧边栏：用**后端原始菜单树**渲染（不复用扁平化后的路由对象，否则目录层级会丢）。
 * 规则：工作台固定第一项；空目录不渲染；visible=0 不展示；孤儿节点按顶级处理。
 */
defineOptions({ name: 'LayoutSidebar' })

const props = defineProps<{
  collapsed: boolean
}>()

const { t } = useI18n()
const route = useRoute()
const permissionStore = usePermissionStore()

/** 应用名：优先取构建期注入的环境变量 */
const appName = computed(() => import.meta.env.VITE_APP_TITLE || t('common.appName'))

/** 侧边栏菜单树（已过滤 visible=0、空目录，并补全孤儿节点） */
const menus = computed(() => buildSidebarTree(permissionStore.menuTree))

/** 当前激活项：用路由 path 匹配菜单 index */
const activePath = computed(() => route.path)

/** 折叠时只显示首字 */
const brandInitial = computed(() => appName.value.slice(0, 1))
</script>

<template>
  <div class="sidebar">
    <div class="sidebar__brand">
      <span class="sidebar__logo">{{ brandInitial }}</span>
      <span v-show="!props.collapsed" class="sidebar__title">{{ appName }}</span>
    </div>
    <el-scrollbar class="sidebar__scroll">
      <el-menu
        :default-active="activePath"
        :collapse="props.collapsed"
        :collapse-transition="false"
        unique-opened
        router
        class="sidebar__menu"
      >
        <el-menu-item :index="HOME_PATH">
          <IconRender name="HomeFilled" />
          <template #title>{{ t('layout.dashboard') }}</template>
        </el-menu-item>
        <SidebarItem v-for="menu in menus" :key="menu.id" :item="menu" :base-path="''" />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<style scoped lang="scss">
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--admin-bg-sidebar);
  border-right: 1px solid var(--admin-border-color);

  &__brand {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 56px;
    flex: 0 0 56px;
    padding: 0 14px;
    overflow: hidden;
    border-bottom: 1px solid var(--admin-divider-color);
  }

  &__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    flex: 0 0 26px;
    border-radius: var(--admin-radius-md);
    font-size: 14px;
    font-weight: 600;
    color: #ffffff;
    background: var(--admin-color-primary);
  }

  &__title {
    font-size: 14px;
    font-weight: 600;
    white-space: nowrap;
    color: var(--admin-text-sidebar);
  }

  &__scroll {
    flex: 1;
    min-height: 0;
  }

  &__menu {
    border-right: none;
    background: transparent;

    :deep(.el-menu-item),
    :deep(.el-sub-menu__title) {
      color: var(--admin-text-sidebar);
    }

    :deep(.el-menu-item:hover),
    :deep(.el-sub-menu__title:hover) {
      background: var(--admin-bg-sidebar-active);
    }

    :deep(.el-menu-item.is-active) {
      color: var(--admin-text-sidebar-active);
      background: var(--admin-bg-sidebar-active);
    }
  }
}
</style>
