import type { Directive, DirectiveBinding } from 'vue'

import { checkPermission, type PermissionValue } from '@/composables/usePermission'
import { useUserStore } from '@/stores/user'

/**
 * 按钮权限指令 v-has（任务书 19.1）。
 *
 * 用法：v-has="USER_API.create" 或 v-has="[USER_API.create, USER_API.update]"
 *
 * 逻辑顺序（必须按此顺序）：
 *   1. 传入值为空 → 不做任何处理，保留元素；
 *   2. superAdmin 为真 → 直接通过；
 *   3. 否则判断权限集合是否包含任一传入标识；
 *   4. 不通过则**把元素从 DOM 中移除**（不是隐藏，避免被改样式绕过）。
 *
 * 判断逻辑与 usePermission / store.hasPermission 共用同一份实现，不写第二遍。
 */

interface RemovedRecord {
  /** 占位注释节点，便于后续复核（例如权限变化后恢复渲染） */
  placeholder: Comment
  parent: Node
}

/** 被移除元素的位置记录 */
const removedRecords = new WeakMap<HTMLElement, RemovedRecord>()

/** 安全地把元素从 DOM 移除并留下占位注释 */
function removeElement(el: HTMLElement): void {
  const parent = el.parentNode
  // 元素可能尚未挂载到父节点，此时不做处理，避免报错
  if (!parent) {
    return
  }
  if (removedRecords.has(el)) {
    return
  }
  const placeholder = document.createComment('v-has')
  parent.insertBefore(placeholder, el)
  parent.removeChild(el)
  removedRecords.set(el, { placeholder, parent })
}

/** 若元素此前被移除且权限已恢复，则放回原位置 */
function restoreElement(el: HTMLElement): void {
  const record = removedRecords.get(el)
  if (!record) {
    return
  }
  removedRecords.delete(el)
  if (record.placeholder.parentNode === record.parent) {
    record.parent.insertBefore(el, record.placeholder)
    record.parent.removeChild(record.placeholder)
  }
}

/** 应用权限判断 */
function applyPermission(el: HTMLElement, binding: DirectiveBinding<PermissionValue>): void {
  const userStore = useUserStore()
  const allowed = checkPermission(userStore.superAdmin, userStore.perms, binding.value)
  if (allowed) {
    restoreElement(el)
    return
  }
  removeElement(el)
}

export const hasDirective: Directive<HTMLElement, PermissionValue> = {
  mounted(el, binding) {
    applyPermission(el, binding)
  },
  updated(el, binding) {
    // 传值或权限集合变化时重新判断
    applyPermission(el, binding)
  }
}

export default hasDirective
