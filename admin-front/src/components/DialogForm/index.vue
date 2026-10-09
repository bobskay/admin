<script setup lang="ts">
import type { FormInstance } from 'element-plus'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

/**
 * 弹窗表单：统一标题、宽度、确定/取消、提交 loading、关闭重置、可配置为抽屉。
 *
 * 用法：
 *   <DialogForm v-model="visible" :title="t('user.create')" :loading="submitting"
 *               :form-instance="formRef" @submit="handleSubmit"> ...表单项... </DialogForm>
 *
 * 说明：关闭（取消 / 右上角 / 遮罩）时都会发出 cancel 并调用 formInstance.resetFields() 重置表单，
 * 因此页面把 useFormValidation 返回的 formRef 通过 :form-instance 传进来即可。
 *
 * 注意：布尔类型的 prop 必须用 withDefaults 显式给默认值。Vue 对「Boolean 类型且未传值」的 prop
 * 默认按 **false** 处理（不是 undefined），写成 props.showFooter !== false 这种「默认 true」的
 * 判定永远拿到 false，会让底部按钮整块消失。
 */
defineOptions({ name: 'DialogForm' })

const props = withDefaults(
  defineProps<{
    /** 可见性（v-model） */
    modelValue: boolean
    /** 标题（已翻译文案） */
    title?: string
    /** 宽度，默认 600px */
    width?: string | number
    /** 是否用抽屉代替弹窗 */
    isDrawer?: boolean
    /** 提交 loading */
    loading?: boolean
    /** 内部表单实例，用于关闭时重置表单 */
    formInstance?: FormInstance | null
    /** 确定按钮的 i18n 键 */
    confirmTextKey?: string
    /** 取消按钮的 i18n 键 */
    cancelTextKey?: string
    /** 是否显示底部按钮区 */
    showFooter?: boolean
    /** 关闭后是否销毁内容 */
    destroyOnClose?: boolean
    /** 抽屉尺寸（isDrawer 时生效） */
    drawerSize?: string | number
  }>(),
  {
    isDrawer: false,
    loading: false,
    confirmTextKey: 'common.confirm',
    cancelTextKey: 'common.cancel',
    showFooter: true,
    destroyOnClose: true
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  /** 点击确定 */
  submit: []
  /** 关闭（取消 / 右上角 / 遮罩） */
  cancel: []
}>()

const { t } = useI18n()

const visible = computed<boolean>({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

const dialogWidth = computed(() => props.width ?? '600px')
const drawerSize = computed(() => props.drawerSize ?? props.width ?? '520px')
const isDrawerMode = computed(() => props.isDrawer === true)
const submitting = computed(() => props.loading === true)
const footerVisible = computed(() => props.showFooter === true)
const confirmText = computed(() => t(props.confirmTextKey ?? 'common.confirm'))
const cancelText = computed(() => t(props.cancelTextKey ?? 'common.cancel'))

/** 重置内部表单：回到初始值 */
function resetForm(): void {
  const instance = props.formInstance
  if (instance && typeof instance.resetFields === 'function') {
    instance.resetFields()
  }
}

/** 关闭：同步可见性、通知父组件、重置表单 */
function close(): void {
  visible.value = false
  emit('cancel')
  resetForm()
}

/** 右上角关闭 / 遮罩点击：走统一关闭流程 */
function handleBeforeClose(done: () => void): void {
  close()
  done()
}

function handleSubmit(): void {
  emit('submit')
}
</script>

<template>
  <el-drawer
    v-if="isDrawerMode"
    v-model="visible"
    :title="title"
    :size="drawerSize"
    :destroy-on-close="destroyOnClose"
    :before-close="handleBeforeClose"
    class="dialog-form dialog-form--drawer"
  >
    <div class="dialog-form__body">
      <slot />
    </div>
    <!--
      注意：footer 插槽必须**无条件**提供，是否显示底部按钮由内层 v-if 控制。
      Element Plus 靠 `$slots.footer` 是否存在来决定渲不渲染 footer 容器，
      写成 `<template v-if="footerVisible" #footer>` 会让 footer 整块消失。
    -->
    <template #footer>
      <div v-if="footerVisible" class="dialog-form__footer">
        <slot name="footer">
          <el-button @click="close">{{ cancelText }}</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ confirmText }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-drawer>

  <el-dialog
    v-else
    v-model="visible"
    :title="title"
    :width="dialogWidth"
    :destroy-on-close="destroyOnClose"
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
    class="dialog-form"
  >
    <div class="dialog-form__body">
      <slot />
    </div>
    <template #footer>
      <div v-if="footerVisible" class="dialog-form__footer">
        <slot name="footer">
          <el-button @click="close">{{ cancelText }}</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ confirmText }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dialog-form {
  &__body {
    padding: 4px 4px 0;
    // 表单很长时让内容自己滚动，保证底部「取消 / 确定」始终留在弹窗里可见
    max-height: 62vh;
    overflow-y: auto;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
}
</style>
