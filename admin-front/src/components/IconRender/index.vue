<script setup lang="ts">
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { computed, type Component } from 'vue'

/**
 * 图标组件：统一渲染 Element Plus 图标名。
 * 用法：<IconRender name="User" />
 * 取不到图标时渲染一个占位方块，不抛异常。
 */
defineOptions({ name: 'IconRender' })

const props = defineProps<{
  /** Element Plus 图标名（PascalCase，例如 User / Setting / HomeFilled） */
  name?: string | null
  /** 图标尺寸 */
  size?: number | string
}>()

/** 全局图标注册表 */
const iconRegistry = ElementPlusIcons as unknown as Record<string, Component>

const iconComponent = computed<Component | null>(() => {
  const name = (props.name ?? '').trim()
  if (!name) {
    return null
  }
  // 允许写成 el-icon-user 这类 kebab 形式
  const pascal = name.replace(/(^|-)([a-z])/g, (_match, _prefix: string, letter: string) =>
    letter.toUpperCase()
  )
  return iconRegistry[name] ?? iconRegistry[pascal] ?? null
})

const iconSize = computed(() => {
  const value = props.size
  if (value === undefined || value === null || value === '') {
    return undefined
  }
  return typeof value === 'number' ? `${value}px` : value
})
</script>

<template>
  <el-icon v-if="iconComponent" class="icon-render" :size="iconSize">
    <component :is="iconComponent" />
  </el-icon>
  <span v-else class="icon-render icon-render--placeholder" :style="{ width: iconSize, height: iconSize }" />
</template>

<style scoped lang="scss">
.icon-render {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: middle;

  &--placeholder {
    width: 1em;
    height: 1em;
    border: 1px dashed var(--admin-border-color);
    border-radius: var(--admin-radius-sm);
    opacity: 0.6;
  }
}
</style>
