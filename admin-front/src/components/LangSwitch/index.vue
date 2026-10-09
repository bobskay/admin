<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

import { useLocale } from '@/composables/useLocale'
import type { LanguageCode } from '@/utils/constants'

/**
 * 中英语言切换。
 * 切换后同步：本地存储、vue-i18n、html lang、以及后续请求的 accept-language 请求头。
 */
defineOptions({ name: 'LangSwitch' })

const { t } = useI18n()
const { language, languages, languageLabelKeys, setLanguage } = useLocale()

/** 触发按钮上显示的当前语言名 */
const currentLabel = computed(() => t(languageLabelKeys[language.value]))

function handleCommand(command: string | number | object): void {
  setLanguage(String(command) as LanguageCode)
}
</script>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="lang-switch" :title="t('layout.language')">
      <span class="lang-switch__text">{{ currentLabel }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in languages"
          :key="item"
          :command="item"
          :disabled="item === language"
        >
          {{ t(languageLabelKeys[item]) }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.lang-switch {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 8px;
  cursor: pointer;
  color: var(--admin-text-primary);
  border-radius: var(--admin-radius-md);
  font-size: 13px;
  transition: background-color var(--admin-transition);

  &:hover {
    background: var(--admin-bg-hover);
  }

  &__text {
    white-space: nowrap;
  }
}
</style>
