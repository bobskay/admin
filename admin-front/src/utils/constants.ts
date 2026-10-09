/**
 * 本地存储键名、请求头名、枚举取值等全局常量。
 * 禁止在页面里散落魔法字符串（任务书 16.9）。
 */

/* ------------------------------------------------------------ 本地存储键名 */

export const STORAGE_KEYS = {
  /** 登录 token */
  TOKEN: 'admin_token',
  /** 当前语言 */
  LANGUAGE: 'admin_language',
  /** 当前主题 */
  THEME: 'admin_theme',
  /** 记住的用户名 */
  REMEMBER_USERNAME: 'admin_remember_username',
  /** 本次登录成功的时间（毫秒时间戳字符串，工作台「登录时间」用） */
  LOGIN_TIME: 'admin_login_time',
  /** 侧边栏折叠状态（任务书 17 章要求折叠状态持久化） */
  SIDEBAR_COLLAPSED: 'admin_sidebar_collapsed'
} as const

/* ---------------------------------------------------------------- 请求头名 */

export const HEADER_NAMES = {
  TRACE_ID: 'x-trace-id',
  AUTHORIZATION: 'authorization',
  ACCEPT_LANGUAGE: 'accept-language'
} as const

/** token 前缀 */
export const TOKEN_PREFIX = 'Bearer '

/* ------------------------------------------------------------------ 语言 */

export const LANGUAGE_CODES = ['zh-CN', 'en-US'] as const
export type LanguageCode = (typeof LANGUAGE_CODES)[number]

/** 默认语言：简体中文 */
export const DEFAULT_LANGUAGE: LanguageCode = 'zh-CN'

/* ------------------------------------------------------------------ 主题 */

export const THEME_NAMES = ['light', 'dark', 'blue'] as const
export type ThemeName = (typeof THEME_NAMES)[number]

/** 默认主题：白 */
export const DEFAULT_THEME: ThemeName = 'light'

/** 黑主题附加在根元素上的类名 */
export const DARK_CLASS = 'dark'

/* -------------------------------------------------------------- 状态与枚举 */

/** 启用 / 禁用（用户、角色、菜单通用） */
export const STATUS = {
  ENABLED: 1,
  DISABLED: 0
} as const

/** 菜单类型 */
export const MENU_TYPE = {
  DIRECTORY: 1,
  MENU: 2,
  BUTTON: 3
} as const

/** 显示状态 */
export const VISIBLE = {
  SHOW: 1,
  HIDE: 0
} as const

/** 页面缓存 */
export const KEEP_ALIVE = {
  YES: 1,
  NO: 0
} as const

/** 顶级菜单的 parentId */
export const ROOT_PARENT_ID = '0'

/**
 * 超级管理员的权限数组展示值。
 * 仅用于展示与兼容，**禁止**在任何判断逻辑里匹配它（判据是 superAdmin 布尔值）。
 */
export const SUPER_ADMIN_PERM = '*:*:*'

/* -------------------------------------------------------------- 分页与请求 */

export const DEFAULT_PAGE_NUM = 1
export const DEFAULT_PAGE_SIZE = 10
export const MAX_PAGE_SIZE = 100

/** 请求超时（毫秒） */
export const REQUEST_TIMEOUT = 15000

/** 查询条件输入型字段的防抖时长 */
export const SEARCH_DEBOUNCE = 300

/* ---------------------------------------------------------------- 状态码 */

/** 唯一成功码 */
export const SUCCESS_CODE = 200
/** 参数校验失败 */
export const CODE_VALIDATION_FAILED = 100004
/** 未登录或登录已过期 */
export const CODE_UNAUTHORIZED = 200003

/** 请求被并发去重取消时使用的内部码 */
export const CODE_REQUEST_CANCELED = -1
/** 传输层网络错误（无响应）时使用的内部码 */
export const CODE_NETWORK_ERROR = -2
/** 传输层超时错误时使用的内部码 */
export const CODE_TIMEOUT_ERROR = -3

/* ------------------------------------------------------------ 路由与白名单 */

/** 登录页路径 */
export const LOGIN_PATH = '/login'

/** 403 路径 */
export const FORBIDDEN_PATH = '/403'

/** 404 路径 */
export const NOT_FOUND_PATH = '/404'

/** 登录后的默认首页 */
export const HOME_PATH = '/dashboard'

/** 无需登录即可访问的路由（守卫白名单） */
export const WHITE_LIST_PATHS: string[] = [LOGIN_PATH, FORBIDDEN_PATH, NOT_FOUND_PATH]

/* ------------------------------------------------------------ 动态路由名 */

/** 布局路由名（动态路由统一挂到它下面） */
export const ROOT_ROUTE_NAME = 'Root'

/** 动态路由名前缀：menu-{菜单id} */
export const MENU_ROUTE_PREFIX = 'menu-'

/** 404 视图的 glob 路径 */
export const NOT_FOUND_VIEW = '/src/views/error/404.vue'

/** 视图文件根目录 */
export const VIEWS_ROOT = '/src/views/'
