<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { usePermissionStore } from '@/stores/permission'

/**
 * 主内容区 + keep-alive。
 * include 用**路由 name**（动态路由为 menu-{菜单id}，静态路由为 Dashboard 等）；
 * 视图组件在 componentLoader 里被注入了同名 name，因此 include 能真正命中。
 */
defineOptions({ name: 'LayoutAppMain' })

const router = useRouter()
const permissionStore = usePermissionStore()

/** 需要缓存的组件名集合 */
const cachedNames = computed<string[]>(() => {
  const names: string[] = []
  // 动态路由（响应式，登录后立即生效）
  permissionStore.routes.forEach((record) => {
    if (record.meta?.keepAlive && record.name) {
      names.push(String(record.name))
    }
  })
  // 静态路由（工作台等）
  router.getRoutes().forEach((record) => {
    if (record.meta?.keepAlive && typeof record.name === 'string') {
      names.push(record.name)
    }
  })
  return Array.from(new Set(names))
})
</script>

<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route: currentRoute }">
      <keep-alive :include="cachedNames">
        <component :is="Component" :key="currentRoute.path" />
      </keep-alive>
    </router-view>
  </section>
</template>

<style scoped lang="scss">
.app-main {
  min-height: 100%;
}
</style>
