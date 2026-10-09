import axios, {
  type AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig
} from 'axios'
import { ElMessage } from 'element-plus'
import type { Router } from 'vue-router'

import { resolveErrorMessage } from '@/locales'
import type { ApiResult, FieldErrorItem, RequestError } from '@/types'

import {
  CODE_NETWORK_ERROR,
  CODE_REQUEST_CANCELED,
  CODE_TIMEOUT_ERROR,
  CODE_UNAUTHORIZED,
  CODE_VALIDATION_FAILED,
  HEADER_NAMES,
  LOGIN_PATH,
  REQUEST_TIMEOUT,
  STORAGE_KEYS,
  SUCCESS_CODE,
  TOKEN_PREFIX
} from './constants'
import { readPersistedLanguage } from './storage'
import { getToken, removeToken } from './token'

/**
 * axios 封装（严格按任务书第十六章 9 条实现）。
 *
 * 对外只导出 post / get，业务代码不允许直接使用 axios 原始实例。
 * 调用方拿到的就是**业务数据本身**（code === 200 时直接返回 data 字段）。
 */

// 给请求配置挂一个去重键，便于在拦截器之间传递（仅此一处用到模块扩展）
declare module 'axios' {
  interface InternalAxiosRequestConfig {
    dedupKey?: string
  }
}

/* ------------------------------------------------------------ 模块级状态 */

/** 路由实例，由 setupRequestContext 注入（避免与 store / router 形成循环依赖） */
let routerRef: Router | null = null
/** 未登录时清理用户状态的回调，由 main.ts 注入 */
let onUnauthorizedCleanup: (() => void) | null = null
/** 200003 的模块级标志位：并发请求时只处理一次 */
let unauthorizedHandledAt = 0
/** 短时间窗口（毫秒） */
const UNAUTHORIZED_WINDOW = 2000
/** 在途请求：key 为 method + url + 序列化参数 */
const pendingMap = new Map<string, AbortController>()
/** storage 监听只绑定一次 */
let storageBound = false

/* ---------------------------------------------------------------- 工具 */

/** 生成 traceId：去掉横线的 uuid */
function createTraceId(): string {
  const uuid =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}`
  return uuid.replace(/-/g, '')
}

/** 序列化参数用于去重键 */
function serializeParams(data: unknown): string {
  if (data === null || data === undefined) {
    return ''
  }
  try {
    return JSON.stringify(data)
  } catch {
    return String(data)
  }
}

/** 并发去重键：method + url + 序列化参数 */
function buildDedupKey(config: InternalAxiosRequestConfig): string {
  const method = (config.method ?? 'post').toLowerCase()
  return `${method}|${config.url ?? ''}|${serializeParams(config.data)}`
}

/** 释放去重记录（只清理属于自己的那一条） */
function releasePending(config?: InternalAxiosRequestConfig): void {
  const key = config?.dedupKey
  if (!key) {
    return
  }
  const controller = pendingMap.get(key)
  if (controller && controller.signal === config?.signal) {
    pendingMap.delete(key)
  }
}

/** 从响应头读取 traceId */
function readTraceId(response?: AxiosResponse): string | undefined {
  const value = response?.headers?.[HEADER_NAMES.TRACE_ID]
  return value === undefined || value === null ? undefined : String(value)
}

/** 判断是否为统一返回体 */
function isApiResult(value: unknown): value is ApiResult<unknown> {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ApiResult<unknown>).code === 'number'
  )
}

/**
 * 运行时返回的是业务数据本身，这里包一层仅为满足 axios 拦截器的类型签名。
 */
function passThrough(value: unknown): AxiosResponse {
  return value as unknown as AxiosResponse
}

/* ------------------------------------------------------ 未登录统一处理 */

/** 跳转登录页并带上重定向地址 */
function redirectToLogin(): void {
  const router = routerRef
  if (!router) {
    return
  }
  const current = router.currentRoute.value
  if (current.path === LOGIN_PATH) {
    return
  }
  void router.replace({ path: LOGIN_PATH, query: { redirect: current.fullPath } })
}

/**
 * 200003（未登录或登录已过期）与多标签页同步的统一入口。
 * 用模块级标志 + 短时间窗口保证并发请求下只弹一次提示、只跳一次。
 */
function handleUnauthorized(traceId?: string): void {
  const now = Date.now()
  if (now - unauthorizedHandledAt < UNAUTHORIZED_WINDOW) {
    return
  }
  unauthorizedHandledAt = now

  removeToken()
  onUnauthorizedCleanup?.()
  ElMessage.error(resolveErrorMessage(CODE_UNAUTHORIZED))
  console.warn('[request] unauthorized, redirecting to login', { traceId })
  redirectToLogin()
}

/** 多标签页同步：其他标签页清掉 token 时本页同步清空状态并跳登录页 */
function bindStorageSync(): void {
  if (storageBound) {
    return
  }
  storageBound = true
  window.addEventListener('storage', (event: StorageEvent) => {
    if (event.key !== STORAGE_KEYS.TOKEN) {
      return
    }
    // 只有「被清除」才同步登出；同一标签页内写入不会触发 storage 事件
    if (event.newValue) {
      return
    }
    handleUnauthorized()
  })
}

/* --------------------------------------------------------- 错误对象构造 */

/** 业务错误：1200003 与 100004 特殊处理，其余按 error.{code} 提示 */
function buildBusinessError(body: ApiResult<unknown>, traceId?: string): RequestError {
  if (body.code === CODE_VALIDATION_FAILED) {
    // 参数校验失败：把校验错误对象透传给调用方，由表单定位控件，这一层不弹任何提示
    const detail = (body.data ?? null) as FieldErrorItem | null
    console.warn('[request] validation failed', {
      traceId,
      field: detail?.field ?? null,
      reason: detail?.reason ?? null,
      message: body.message
    })
    return { code: body.code, data: body.data ?? null, traceId }
  }

  if (body.code === CODE_UNAUTHORIZED) {
    handleUnauthorized(traceId)
    return { code: body.code, data: null, traceId }
  }

  const text = resolveErrorMessage(body.code)
  ElMessage.error(text)
  // message 只进 console，不做展示（任务书 6.1）
  console.error(`[request] business error code=${body.code} message=${body.message ?? ''}`, {
    traceId
  })
  return { code: body.code, data: body.data ?? null, traceId }
}

/** HTTP 层错误：网络错误、超时、非 200 状态码 */
function buildTransportError(error: AxiosError, traceId?: string): RequestError {
  const code =
    error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'
      ? CODE_TIMEOUT_ERROR
      : CODE_NETWORK_ERROR
  const text =
    code === CODE_TIMEOUT_ERROR
      ? resolveErrorMessage('timeout')
      : error.response
        ? resolveErrorMessage('http')
        : resolveErrorMessage('network')
  ElMessage.error(text)
  console.error(`[request] transport error ${error.code ?? ''} ${error.message}`, { traceId })
  return { code, data: null, traceId, message: text }
}

/* -------------------------------------------------------------- 实例 */

const instance: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken()
  if (token) {
    // 认证头：Bearer + tokenId
    config.headers.set(HEADER_NAMES.AUTHORIZATION, `${TOKEN_PREFIX}${token}`)
  }
  // 每次请求生成新的 traceId，便于与后端日志对账
  config.headers.set(HEADER_NAMES.TRACE_ID, createTraceId())
  // 语言头跟随当前语言
  config.headers.set(HEADER_NAMES.ACCEPT_LANGUAGE, readPersistedLanguage())

  // 并发去重：同 key 在途时取消前一个请求
  const key = buildDedupKey(config)
  config.dedupKey = key
  const previous = pendingMap.get(key)
  if (previous) {
    previous.abort()
  }
  const controller = new AbortController()
  config.signal = controller.signal
  pendingMap.set(key, controller)

  return config
})

instance.interceptors.response.use(
  (response: AxiosResponse) => {
    releasePending(response.config)
    const traceId = readTraceId(response)
    const body: unknown = response.data

    // 非统一返回体（例如静态资源）原样返回
    if (!isApiResult(body)) {
      return passThrough(body)
    }

    if (body.code === SUCCESS_CODE) {
      // 成功：直接把 data 字段返回给调用方
      return passThrough(body.data)
    }

    return Promise.reject(buildBusinessError(body, traceId))
  },
  (error: AxiosError) => {
    releasePending(error.config)

    // 去重导致的取消：静默 reject，不弹提示
    if (isCanceledRequest(error)) {
      const canceled: RequestError = {
        code: CODE_REQUEST_CANCELED,
        data: null,
        canceled: true
      }
      return Promise.reject(canceled)
    }

    const traceId = readTraceId(error.response)

    // HTTP 状态码非 200 但响应体仍是统一返回体时，按业务错误处理
    const responseBody: unknown = error.response?.data
    if (isApiResult(responseBody)) {
      return Promise.reject(buildBusinessError(responseBody, traceId))
    }

    return Promise.reject(buildTransportError(error, traceId))
  }
)

/* ---------------------------------------------------------- 对外接口 */

/**
 * POST 请求。调用方拿到的就是业务数据本身（泛型声明返回类型）。
 * 注意：参数校验失败（100004）时 reject 的错误对象上带 data（校验错误对象）。
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  return instance.post(url, data ?? {}, config) as unknown as Promise<T>
}

/** GET 请求（业务接口一律 POST，此方法仅用于兼容非业务场景） */
export function get<T = unknown>(
  url: string,
  params?: Record<string, unknown>,
  config?: AxiosRequestConfig
): Promise<T> {
  return instance.get(url, { ...config, params }) as unknown as Promise<T>
}

/**
 * 注入路由与未登录清理回调，并开启多标签页同步。
 * 在 main.ts 中于 Pinia 与 Router 就绪后调用一次。
 */
export function setupRequestContext(options: {
  router: Router
  /** 清理用户状态（token 之外的 store / 动态路由） */
  onUnauthorized?: () => void
}): void {
  routerRef = options.router
  onUnauthorizedCleanup = options.onUnauthorized ?? null
  bindStorageSync()
}

/**
 * 判断是否为「请求被取消」。
 * 说明：这里刻意不用 `axios.isCancel(error)` 直接做条件——它是类型谓词，
 * 会把后续代码里的 AxiosError 收窄成 never，导致读 error.response 报 TS2339。
 * 因此包一层普通布尔函数（传入的是表达式而非 error 变量本身，不会触发收窄）。
 */
function isCanceledRequest(error: AxiosError): boolean {
  if (error.code === 'ERR_CANCELED') {
    return true
  }
  return axios.isCancel(error as unknown as object)
}
