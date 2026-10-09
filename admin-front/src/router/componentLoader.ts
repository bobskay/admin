import type { Component } from 'vue'

import { NOT_FOUND_VIEW, VIEWS_ROOT } from '@/utils/constants'

/**
 * 组件定位（任务书 18.2）。
 *
 * 用 import.meta.glob 建立一张全量组件映射表，**禁止手写零散的 import 语句**。
 * 定位优先级：
 *   1. 菜单的 component 字段非空 → 用它查表；
 *   2. component 为空 → 按路由 path 推导；
 *   3. 都找不到 → 404 页面（不抛异常，避免白屏）。
 */

/** 懒加载视图组件 */
type LazyView = () => Promise<{ default: Component }>

// glob 返回的是 () => Promise<unknown>，这里断言为视图模块（唯一的类型断言，附带本注释说明）
const viewModules = import.meta.glob('/src/views/**/*.vue') as unknown as Record<string, LazyView>

/** 兜底空组件：仅在 404 视图本身缺失时使用（正常情况下 404.vue 一定存在） */
const emptyView: LazyView = () => Promise.resolve({ default: { name: 'EmptyView', render: () => null } })

/** 规范化 component 字段：去掉扩展名与 src/views 前缀 */
function normalizeComponent(component: string): string {
  return component
    .trim()
    .replace(/\\/g, '/')
    .replace(/\.vue$/i, '')
    .replace(/^\/+/, '')
    .replace(/^src\//, '')
    .replace(/^views\//, '')
}

/** 规范化路由 path */
function normalizeRoutePath(path: string): string {
  return path.trim().replace(/^\/+/, '').replace(/\/+$/, '')
}

/** 拼接为 glob 使用的绝对路径 */
function toModuleKey(relative: string): string {
  return `${VIEWS_ROOT}${relative.replace(/^\/+/, '')}`
}

/** 按候选顺序查表，返回命中的键 */
function findModuleKey(candidates: string[]): string | null {
  for (const candidate of candidates) {
    if (!candidate) {
      continue
    }
    const key = toModuleKey(candidate)
    if (viewModules[key]) {
      return key
    }
  }
  return null
}

/**
 * 解析菜单对应的视图文件路径（glob 的 key）。
 * 找不到时返回 404 视图路径。
 */
export function resolveViewKey(
  component?: string | null,
  path?: string | null
): string {
  // 优先级 1：菜单的 component 字段
  if (component && component.trim()) {
    const normalized = normalizeComponent(component)
    if (normalized) {
      const matched = findModuleKey([`${normalized}.vue`, `${normalized}/index.vue`])
      if (matched) {
        return matched
      }
    }
  }

  // 优先级 2：按路由 path 推导（/system/user → /src/views/system/user/index.vue）
  if (path && path.trim()) {
    const normalized = normalizeRoutePath(path)
    if (normalized) {
      const matched = findModuleKey([`${normalized}/index.vue`, `${normalized}.vue`])
      if (matched) {
        return matched
      }
    }
  }

  // 优先级 3：404
  return NOT_FOUND_VIEW
}

/**
 * 解析菜单对应的懒加载组件；两种方式都找不到时指向 404 页面。
 *
 * 第三个参数 routeName 用于把路由 name（menu-{菜单id}）注入到视图组件上：
 * keep-alive 的 include 是按**组件名**匹配的，而 `<script setup>` 视图组件的名字来自文件名（恒为 index），
 * 注入之后 include 才能真正命中，页面缓存才生效（任务书 18.3.4）。
 */
export function resolveViewComponent(
  component?: string | null,
  path?: string | null,
  routeName?: string
): LazyView {
  const key = resolveViewKey(component, path)
  const loader = viewModules[key] ?? viewModules[NOT_FOUND_VIEW] ?? emptyView
  if (!routeName) {
    return loader
  }
  return async () => {
    const module = await loader()
    const target = module?.default as (Component & { name?: string }) | undefined
    if (target && !target.name) {
      target.name = routeName
    }
    return module
  }
}

/** 全部已注册的视图路径，供开发期排查使用 */
export function listViewKeys(): string[] {
  return Object.keys(viewModules).sort()
}

/** 布局组件的懒加载映射表（同样走 glob，不手写 import 语句） */
const layoutModules = import.meta.glob('/src/layouts/index.vue') as unknown as Record<
  string,
  LazyView
>

/** 布局组件（静态路由用） */
export function resolveLayoutComponent(): LazyView {
  return layoutModules['/src/layouts/index.vue'] ?? emptyView
}
