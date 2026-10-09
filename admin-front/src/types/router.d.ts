import 'vue-router'

// 路由 meta 类型扩展：动态路由生成时写入，守卫与布局读取
declare module 'vue-router' {
  interface RouteMeta {
    /** 页面标题的 i18n 键或菜单名（菜单名是数据，直接展示） */
    title?: string
    /** 标题是否已经是最终文案（菜单名来自后端数据，不再走 i18n） */
    rawTitle?: boolean
    /** Element Plus 图标名 */
    icon?: string | null
    /** 是否缓存页面 */
    keepAlive?: boolean
    /** 是否在侧边栏展示 */
    visible?: boolean
    /** 菜单 id */
    menuId?: string
  }
}
