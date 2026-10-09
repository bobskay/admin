/**
 * 全局类型声明。
 * 字段名与后端 VO / DTO 逐字一致（见任务书 13 章），不要改名。
 *
 * 序列化约定（任务书 8.2）：
 * - 所有 Long / long 字段（含 id、total、timestamp、userCount 等）后端序列化为**字符串**；
 * - 日期字段序列化为**毫秒时间戳数字**。
 */

/* ------------------------------------------------------------------ 通用 */

/** 后端统一返回体：只有 code / message / data / timestamp 四个字段 */
export interface ApiResult<T = unknown> {
  /** 状态码，200 表示成功 */
  code: number
  /** 调试用英文短语，前端不得依赖它做展示 */
  message: string | null
  /** 业务数据；参数校验失败时是校验错误对象 */
  data: T
  /** 毫秒时间戳（Long 序列化为字符串） */
  timestamp: string
}

/** 参数校验错误对象（放在统一返回体的 data 里） */
export interface FieldErrorItem {
  /** 出错字段名，已归一化，与表单项 prop 精确对应 */
  field: string
  /** 校验原因，只取受控词表：required / len / pattern / email / range / type */
  reason: string
}

/** 分页入参公共字段 */
export interface PageQueryParams {
  pageNum?: number
  pageSize?: number
  keyword?: string | null
  sortField?: string | null
  sortOrder?: string | null
}

/** 分页出参：Long 字段一律是字符串，用前需 Number() 转换 */
export interface PageResult<T> {
  list: T[]
  /** 总条数（字符串） */
  total: string
  /** 当前页（字符串） */
  pageNum: string
  /** 每页条数（字符串） */
  pageSize: string
  /** 总页数（字符串） */
  pages: string
}

/* --------------------------------------------------------------- 认证模块 */

/** 登录入参 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录出参：tokenId 与过期秒数 */
export interface LoginUser {
  tokenId: string
  /** 有效秒数（字符串） */
  expiresIn: string
}

/** 语义别名：登录结果 */
export type LoginResult = LoginUser

/** 角色简要信息（用户列表 / me 内嵌） */
export interface SysRoleBrief {
  id: string
  name: string
  code: string
}

/** me 接口返回的用户基本信息 */
export interface SysMeUser {
  id: string
  username: string
  nickname: string
  email: string | null
  phone: string | null
  avatar: string | null
  /** 1 启用 0 禁用 */
  status: number
  roles: SysRoleBrief[]
  /** 创建时间（毫秒时间戳） */
  createTime: number
}

/** me 接口完整出参 */
export interface SysMe {
  user: SysMeUser
  /** 角色编码数组 */
  roles: string[]
  /** 是否超级管理员（唯一判据，禁止匹配 *:*:*） */
  superAdmin: boolean
  /** 权限标识数组；超管返回 ['*:*:*']，仅作展示 */
  perms: string[]
  /** 用户可见菜单路由树（只含 type 1 与 2） */
  menus: SysMenuRoute[]
}

/** 工作台统计出参（专用的只校验登录的接口） */
export interface SysCount {
  /** 用户数（字符串） */
  userCount: string
  /** 角色数（字符串） */
  roleCount: string
  /** 菜单数（字符串） */
  menuCount: string
}

/* --------------------------------------------------------------- 用户管理 */

/** 用户列表行 / 详情 */
export interface SysUserInfo {
  id: string
  username: string
  nickname: string
  email: string | null
  phone: string | null
  avatar: string | null
  /** 1 启用 0 禁用 */
  status: number
  remark: string | null
  /** 所属角色简要信息 */
  roles: SysRoleBrief[]
  /** 创建时间（毫秒时间戳） */
  createTime: number
  /** 更新时间（毫秒时间戳） */
  updateTime: number
}

/**
 * 用户分页查询入参。
 * status / roleId 用 undefined 表示「不筛选」：Element Plus 的下拉清空后给的就是 undefined，
 * 而 axios 序列化时会直接丢掉 undefined 字段，后端 DTO 收到 null，效果与传 null 一致。
 */
export interface SysUserPageParams extends PageQueryParams {
  username?: string | null
  nickname?: string | null
  status?: number
  roleId?: string
}

/** 新增用户入参 */
export interface SysUserCreateParams {
  username: string
  password: string
  nickname: string
  email?: string | null
  phone?: string | null
  status: number
  /** 至少一个 */
  roleIds: string[]
  remark?: string | null
}

/** 编辑用户入参（不含密码与角色） */
export interface SysUserUpdateParams {
  id: string
  nickname: string
  email?: string | null
  phone?: string | null
  avatar?: string | null
  status: number
  remark?: string | null
}

/** 重置密码入参 */
export interface SysUserResetPasswordParams {
  id: string
  newPassword: string
}

/** 启用 / 禁用人入参 */
export interface SysUserChangeStatusParams {
  id: string
  status: number
}

/** 分配角色入参 */
export interface SysUserAssignRolesParams {
  userId: string
  roleIds: string[]
}

/* --------------------------------------------------------------- 角色管理 */

/** 角色列表行 / 详情 */
export interface SysRoleInfo {
  id: string
  name: string
  code: string
  sort: number
  /** 1 启用 0 禁用 */
  status: number
  remark: string | null
  createTime: number
  updateTime: number
}

/** 角色分页查询入参（status 用 undefined 表示不筛选，理由同 SysUserPageParams） */
export interface SysRolePageParams extends PageQueryParams {
  name?: string | null
  code?: string | null
  status?: number
}

/** 操作日志分页查询入参 */
export interface SysOperationLogPageParams extends PageQueryParams {
  /** 操作人用户名，模糊匹配 */
  username?: string | null
  /** 模块名，模糊匹配 */
  module?: string | null
  /** 动作名，模糊匹配 */
  action?: string | null
  /** 返回码，精确匹配 */
  resultCode?: number
}

/** 操作日志行数据（id / userId / costMs 是 Long，一律按字符串处理） */
export interface SysOperationLogInfo {
  id: string
  userId?: string | null
  username?: string | null
  module?: string | null
  action?: string | null
  method?: string | null
  uri?: string | null
  ip?: string | null
  userAgent?: string | null
  params?: string | null
  resultCode?: number | null
  errorMsg?: string | null
  costMs?: string | null
  createTime?: number | null
}

/** 角色新增 / 编辑入参 */
export interface SysRoleSaveParams {
  id?: string
  name: string
  code: string
  sort: number
  status: number
  remark?: string | null
}

/* --------------------------------------------------------------- 菜单管理 */

/** 菜单节点（既是 me 的菜单路由树节点，也是菜单管理页的树节点） */
export interface SysMenuRoute {
  id: string
  parentId: string
  /** 菜单名，用户填什么显示什么（数据，不走 i18n） */
  name: string
  /** 1 目录 2 菜单 3 按钮 */
  type: number
  /** 前端路由路径；目录与菜单必填，按钮为空 */
  path: string | null
  /** 前端组件路径，允许为空（为空时按 path 推导） */
  component: string | null
  icon: string | null
  /**
   * 权限标识。me 返回的菜单树不含该字段，菜单管理页的树才含。
   * 目录与菜单留空，按钮必填且等于接口地址。
   */
  perms?: string | null
  sort: number
  /** 1 显示 0 隐藏 */
  visible: number
  /** 1 缓存 0 不缓存 */
  keepAlive: number
  redirect: string | null
  /** 1 启用 0 禁用；me 返回的菜单树不含该字段 */
  status?: number
  createTime?: number
  updateTime?: number
  children?: SysMenuRoute[]
}

/** 语义别名：菜单树节点 */
export type SysMenuNode = SysMenuRoute

/** 菜单新增 / 编辑入参 */
export interface SysMenuSaveParams {
  id?: string
  parentId: string
  name: string
  /** 1 目录 2 菜单 3 按钮 */
  type: number
  path?: string | null
  component?: string | null
  icon?: string | null
  perms?: string | null
  sort: number
  visible: number
  keepAlive: number
  redirect?: string | null
  status: number
}

/* ------------------------------------------------------------------ 清单 */

/** 权限标识清单条目 */
export interface PermissionItem {
  /** 权限标识，即小写的接口地址 */
  perm: string
  /** 所属模块，类路径末端，例如 sysuser */
  module: string
  /** 动作名，方法名转小写，例如 resetpassword */
  action: string
}

/** 校验规则清单条目（开发期比对词条用） */
export interface ValidationRuleItem {
  /** DTO 字段名 */
  field: string
  /** 校验原因，取自受控词表 */
  reason: string
  /** 建议的字段标签键名，即 field.{field} */
  labelKey: string
}

/** 请求层 reject 的错误对象 */
export interface RequestError {
  /** 业务状态码；-1 表示请求被取消（并发去重），-2 / -3 表示传输层错误 */
  code: number
  /** 校验失败时是 FieldErrorItem，其余为 null */
  data: unknown
  /** 是否为取消请求导致的中断 */
  canceled?: boolean
  /** 便于与后端日志对账 */
  traceId?: string
  /** 传输层错误已解析好的展示文案 */
  message?: string
}

/* -------------------------------------------------------------- 表格组件 */

/** 表格排序变化载荷 */
export interface TableSortPayload {
  prop: string | null
  order: 'ascending' | 'descending' | null
}

/**
 * 数据表格的列配置。
 * 行类型由调用方决定，泛型 Row 让 formatter 获得精确的行类型。
 */
export interface TableColumn<Row = Record<string, unknown>> {
  /** 字段名（与行对象的键一致） */
  prop: string
  /** 表头文案的 i18n 键 */
  labelKey?: string
  /** 表头文案（菜单名之类的数据可直接给文案；一般用 labelKey） */
  label?: string
  width?: string | number
  minWidth?: string | number
  align?: 'left' | 'center' | 'right'
  /** 是否可排序；true 会按服务端排序（custom）处理 */
  sortable?: boolean
  fixed?: boolean | 'left' | 'right'
  showOverflowTooltip?: boolean
  /**
   * 附加到该列单元格（td）上的类名。
   * 操作列用它挂 cell-actions，让按钮平铺排列、放不下时换行，而不是被 nowrap 裁掉。
   */
  cellClassName?: string
  /** 具名插槽名；使用插槽时本列内容由插槽渲染，插槽参数为 { row, index, column } */
  slot?: string
  /** 单元格格式化函数（未指定 slot 时生效） */
  formatter?: (row: Row, value: unknown, index: number) => string
}
