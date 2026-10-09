import dayjs from 'dayjs'

/**
 * 日期格式化。
 * 后端日期一律返回**毫秒时间戳数字**（任务书 8.2），因此这里按数字处理；
 * 同时兼容字符串数字与 Date，便于本地测试数据。
 */

/** 默认展示格式 */
export const DATE_TIME_FORMAT = 'YYYY-MM-DD HH:mm:ss'
export const DATE_FORMAT = 'YYYY-MM-DD'

/** 可接受的日期入参 */
export type DateLike = number | string | Date | null | undefined

/** 解析为 dayjs 对象；空值或非法值返回 null */
function parse(value: DateLike): dayjs.Dayjs | null {
  if (value === null || value === undefined || value === '') {
    return null
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) ? dayjs(value) : null
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if (!trimmed) {
      return null
    }
    // 纯数字字符串按毫秒时间戳解析
    if (/^\d+$/.test(trimmed)) {
      return dayjs(Number(trimmed))
    }
    const parsed = dayjs(trimmed)
    return parsed.isValid() ? parsed : null
  }
  const parsed = dayjs(value)
  return parsed.isValid() ? parsed : null
}

/** 格式化为 yyyy-MM-dd HH:mm:ss；无效值返回占位符 */
export function formatDateTime(value: DateLike, format: string = DATE_TIME_FORMAT): string {
  const parsed = parse(value)
  return parsed ? parsed.format(format) : ''
}

/** 格式化为 yyyy-MM-dd */
export function formatDate(value: DateLike): string {
  return formatDateTime(value, DATE_FORMAT)
}

/** 时间戳（毫秒）转 ISO 字符串，供调试使用 */
export function toIsoString(value: DateLike): string {
  const parsed = parse(value)
  return parsed ? parsed.toISOString() : ''
}
