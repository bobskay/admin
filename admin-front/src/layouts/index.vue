<script setup lang="ts">
import { computed } from 'vue'

import { useAppStore } from '@/stores/app'

import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar.vue'

/**
 * 整体布局：侧边栏 + 顶栏 + 主内容区，侧边栏可折叠。
 */
defineOptions({ name: 'AdminLayout' })

const appStore = useAppStore()

const collapsed = computed(() => appStore.sidebarCollapsed)
const asideWidth = computed(() => (appStore.sidebarCollapsed ? '64px' : '210px'))
</script>

<template>
  <el-container class="layout">
    <el-aside class="layout__aside" :width="asideWidth">
      <Sidebar :collapsed="collapsed" />
    </el-aside>
    <el-container class="layout__body" direction="vertical">
      <el-header class="layout__header" height="56px">
        <Navbar />
      </el-header>
      <el-main class="layout__main">
        <AppMain />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
.layout {
  height: 100vh;
  background: var(--admin-bg-page);

  &__aside {
    height: 100%;
    overflow: hidden;
    transition: width 0.2s ease;
  }

  &__body {
    height: 100%;
    min-width: 0;
  }

  &__header {
    padding: 0;
    background: var(--admin-bg-navbar);
    border-bottom: 1px solid var(--admin-border-color);
  }

  &__main {
    padding: 0;
    overflow: auto;
    background: var(--admin-bg-page);
  }
}
</style>
