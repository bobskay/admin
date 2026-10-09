/// <reference types="vite/client" />

// 环境变量类型声明
interface ImportMetaEnv {
  /** 应用标题 */
  readonly VITE_APP_TITLE: string
  /** 接口基础地址（前端请求前缀，固定 /api） */
  readonly VITE_API_BASE_URL: string
  /** 应用版本号 */
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

// 单文件组件模块声明
declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

// nprogress 无自带类型，使用官方 @types/nprogress
