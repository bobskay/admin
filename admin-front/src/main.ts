import * as ElementPlusIcons from '@element-plus/icons-vue'
import { createPinia } from 'pinia'
import { createApp, type Component } from 'vue'

// 样式：Element Plus 基线 → 官方暗色变量 → 进度条 → Tailwind → 主题与全局样式（顺序不可调换）
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'nprogress/nprogress.css'
import './styles/tailwind.css'
import './styles/index.scss'

import App from './App.vue'
import { setupDirectives } from './directives'
import i18n from './locales'
import router from './router'
import { useAppStore } from './stores/app'
import { useUserStore } from './stores/user'
import { setupRequestContext } from './utils/request'
import {
  applyLanguageToDocument,
  applyThemeToDocument,
  readPersistedLanguage,
  readPersistedTheme
} from './utils/storage'

/**
 * 入口文件。
 *
 * 关键点：**在挂载前**先从本地存储读取主题与语言并设置到根元素的 data-theme 属性与 dark 类，
 * 避免刷新时的主题闪烁（任务书 20.1）。
 */
applyThemeToDocument(readPersistedTheme())
applyLanguageToDocument(readPersistedLanguage())

const app = createApp(App)

/** 注册全部 Element Plus 图标（IcRender 与菜单图标都用得到） */
function registerElementPlusIcons(): void {
  const icons = ElementPlusIcons as unknown as Record<string, Component>
  Object.keys(icons).forEach((name) => {
    // 过滤掉模块的 default 导出，它不是图标组件
    if (name === 'default') {
      return
    }
    app.component(name, icons[name])
  })
}

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(i18n)

registerElementPlusIcons()
setupDirectives(app)

// Pinia 安装完成后才能安全取用 store：同步一次主题与语言（兜底，防止存储被外部改动）
const appStore = useAppStore()
appStore.initTheme()

/**
 * 注入路由与未登录清理回调：
 * - 200003（未登录 / 登录过期）时清 token、清用户状态与动态路由，并跳登录页（并发只处理一次）；
 * - 同时开启多标签页同步（其他标签页清掉 token 时本页同步登出）。
 */
setupRequestContext({
  router,
  onUnauthorized: () => {
    useUserStore().clear()
  }
})

app.mount('#app')

// 开发环境：拉一次校验规则清单，比对本地词条缺失情况（生产环境不请求、不输出）
if (import.meta.env.DEV) {
  void import('./locales/devCheck').then((module) => module.checkLocaleKeys())
}
