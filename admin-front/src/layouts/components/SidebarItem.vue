<script setup lang="ts">
import { computed } from 'vue'

import IconRender from '@/components/IconRender/index.vue'
import type { SysMenuRoute } from '@/types'
import { MENU_TYPE } from '@/utils/constants'
import { resolveMenuPath } from '@/utils/tree'

/**
 * 递归菜单项：目录渲染为 el-sub-menu，菜单渲染为 el-menu-item，按钮不渲染。
 * 空目录（没有任何可展示子菜单）直接不渲染。
 */
defineOptions({ name: 'SidebarItem' })

const props = defineProps<{
  /** 菜单节点 */
  item: SysMenuRoute
  /** 父级完整路径（用于相对路径拼接） */
  basePath: string
}>()

/** 完整路径 */
const fullPath = computed(() => resolveMenuPath(props.basePath, props.item.path))

/** 可展示的子菜单 */
const children = computed(() => props.item.children ?? [])

/** 是否目录 */
const isDirectory = computed(() => props.item.type === MENU_TYPE.DIRECTORY)

/** 是否菜单（页面） */
const isMenu = computed(() => props.item.type === MENU_TYPE.MENU)
</script>

<template>
  <el-sub-menu v-if="isDirectory && children.length > 0" :index="fullPath">
    <template #title>
      <IconRender v-if="item.icon" :name="item.icon" />
      <span>{{ item.name }}</span>
    </template>
    <SidebarItem
      v-for="child in children"
      :key="child.id"
      :item="child"
      :base-path="fullPath"
    />
  </el-sub-menu>

  <el-menu-item v-else-if="isMenu && item.path" :index="fullPath">
    <IconRender v-if="item.icon" :name="item.icon" />
    <template #title>{{ item.name }}</template>
  </el-menu-item>
</template>
