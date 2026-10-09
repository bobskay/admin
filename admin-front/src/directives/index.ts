import type { App } from 'vue'

import { hasDirective } from './has'

/**
 * 注册全局自定义指令。
 * 在 main.ts 中于 app.mount 之前调用。
 */
export function setupDirectives(app: App): void {
  app.directive('has', hasDirective)
}

export { hasDirective }
