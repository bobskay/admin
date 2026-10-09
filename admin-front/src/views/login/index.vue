<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { object } from 'yup'

import LangSwitch from '@/components/LangSwitch/index.vue'
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import { useFormValidation } from '@/composables/useFormValidation'
import { resolveErrorMessage } from '@/locales'
import { useUserStore } from '@/stores/user'
import type { LoginParams, RequestError } from '@/types'
import { HOME_PATH } from '@/utils/constants'
import { readRememberedUsername, removeRememberedUsername, writeRememberedUsername } from '@/utils/storage'
import { passwordRule, usernameRule } from '@/utils/validate'

/**
 * 登录页（任务书 21.1）。
 * 校验：VeeValidate + yup（useFormValidation），规则里的 message 是 i18n 键。
 * 失败提示按状态码走 error.{code} 词条，不暴露「用户是否存在」的细节。
 */
defineOptions({ name: 'LoginPage' })

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 登录入参的 yup 规则：用户名必填且 2-50 位，密码必填且 8-32 位并含字母与数字 */
const loginSchema = object({
  username: usernameRule(),
  password: passwordRule()
})

const { formRef, values, errorFor, submit, setValues, handleFieldChange, applyRequestError } =
  useFormValidation<LoginParams>({
    schema: loginSchema,
    initialValues: { username: '', password: '' }
  })

/** 是否记住用户名 */
const rememberMe = ref(false)
/** 提交中（按钮 loading） */
const submitting = ref(false)

/** 登录成功后的跳转地址：优先 query.redirect，否则首页 */
const redirectPath = computed(() => {
  const redirect = route.query.redirect
  const value = Array.isArray(redirect) ? redirect[0] : redirect
  return typeof value === 'string' && value.trim() !== '' ? value : HOME_PATH
})

onMounted(() => {
  const remembered = readRememberedUsername()
  if (remembered) {
    rememberMe.value = true
    setValues({ username: remembered, password: '' })
  }
})

/** 记住用户名 / 取消记住 */
function applyRememberedUsername(username: string): void {
  if (rememberMe.value) {
    writeRememberedUsername(username)
  } else {
    removeRememberedUsername()
  }
}

/** 登录失败：按业务码提示；200004 之类的重名码登录场景不会出现，这里统一走 error.{code} */
async function handleLoginError(error: unknown): Promise<void> {
  const requestError = error as RequestError | null
  // 参数校验失败（100004）：定位到具体控件，不再额外弹全局提示
  const located = await applyRequestError(requestError)
  if (located || requestError?.code === 100004) {
    return
  }
  const code = requestError?.code
  if (typeof code === 'number' && !requestError?.canceled) {
    ElMessage.error(resolveErrorMessage(code))
  }
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) {
    return
  }
  submitting.value = true
  try {
    const executed = await submit(async (formValues) => {
      const username = formValues.username.trim()
      await userStore.login(username, formValues.password)
      applyRememberedUsername(username)
    })
    if (!executed) {
      return
    }
    ElMessage.success(t('login.success'))
    await router.replace(redirectPath.value)
  } catch (error) {
    await handleLoginError(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login">
    <div class="login__switchers">
      <ThemeSwitch />
      <LangSwitch />
    </div>

    <div class="login__card">
      <aside class="login__brand">
        <h1 class="login__brand-title">{{ t('login.brandTitle') }}</h1>
        <p class="login__brand-desc">{{ t('login.brandDesc') }}</p>
      </aside>

      <section class="login__form-area">
        <header class="login__header">
          <h2 class="login__title">{{ t('login.title') }}</h2>
          <p class="login__subtitle">{{ t('login.subtitle') }}</p>
        </header>

        <el-form
          ref="formRef"
          class="login__form"
          label-position="top"
          @submit.prevent="handleSubmit"
        >
          <el-form-item :label="t('field.username')" prop="username" :error="errorFor('username')">
            <el-input
              :model-value="values.username"
              :placeholder="t('login.usernamePlaceholder')"
              clearable
              autocomplete="username"
              @update:model-value="handleFieldChange('username', $event)"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item :label="t('field.password')" prop="password" :error="errorFor('password')">
            <el-input
              :model-value="values.password"
              type="password"
              show-password
              :placeholder="t('login.passwordPlaceholder')"
              autocomplete="current-password"
              @update:model-value="handleFieldChange('password', $event)"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login__options">
            <el-checkbox v-model="rememberMe">{{ t('login.rememberMe') }}</el-checkbox>
          </div>

          <el-button
            class="login__submit"
            type="primary"
            native-type="submit"
            :loading="submitting"
          >
            {{ t('login.submit') }}
          </el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--admin-bg-page);

  &__switchers {
    position: absolute;
    top: 16px;
    right: 20px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__card {
    display: flex;
    width: 100%;
    max-width: 760px;
    min-height: 400px;
    overflow: hidden;
    background: var(--admin-bg-card);
    border: 1px solid var(--admin-border-color);
    border-radius: 6px;
    box-shadow: 0 6px 24px rgb(0 0 0 / 6%);
  }

  &__brand {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 12px;
    width: 300px;
    padding: 32px 28px;
    color: #ffffff;
    background: linear-gradient(160deg, var(--admin-color-primary) 0%, var(--admin-color-primary-dark) 100%);
  }

  &__brand-title {
    font-size: 20px;
    font-weight: 600;
    line-height: 30px;
  }

  &__brand-desc {
    font-size: 13px;
    line-height: 22px;
    opacity: 0.85;
  }

  &__form-area {
    flex: 1;
    min-width: 0;
    padding: 40px 36px;
  }

  &__header {
    display: flex;
    flex-direction: column;
    gap: 6px;
    margin-bottom: 22px;
  }

  &__title {
    font-size: 20px;
    font-weight: 600;
    line-height: 28px;
    color: var(--admin-text-primary);
  }

  &__subtitle {
    font-size: 13px;
    line-height: 20px;
    color: var(--admin-text-secondary);
  }

  &__form {
    display: flex;
    flex-direction: column;
  }

  &__options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 2px 0 18px;
  }

  &__submit {
    width: 100%;
  }
}

@media (width <= 720px) {
  .login__brand {
    display: none;
  }

  .login__form-area {
    padding: 32px 24px;
  }
}
</style>
