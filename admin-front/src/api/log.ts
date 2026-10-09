import type { PageResult, SysOperationLogInfo, SysOperationLogPageParams } from '@/types'
import { post } from '@/utils/request'

/**
 * 操作日志接口地址（任务书 13 章的同类约定）。
 * 日志只查不改，所以这里只有一个分页接口；权限标识默认等于接口地址，
 * 页面按钮与 v-has 一律引用该常量。
 */
export const LOG_API = {
  page: '/admin/system/syslog/page'
} as const

/** 分页查询操作日志 */
export function pageOperationLog(
  data: SysOperationLogPageParams
): Promise<PageResult<SysOperationLogInfo>> {
  return post<PageResult<SysOperationLogInfo>>(LOG_API.page, data)
}
