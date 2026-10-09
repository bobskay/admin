<script setup lang="ts">
import { useI18n } from 'vue-i18n'

import IconRender from '@/components/IconRender/index.vue'
import { useTheme } from '@/composables/useTheme'
import type { ThemeName } from '@/utils/constants'

/**
 * 三主题切换（白 / 黑 / 蓝）。
 * 文案全部走 i18n（layout.themeLight / themeDark / themeBlue）。
 */
defineOptions({ name: 'ThemeSwitch' })

const { t } = useI18n()
const { theme, themes, themeLabelKeys, isDark, setTheme } = useTheme()

/** 每个主题对应的图标名 */
const themeIcons: Record<ThemeName, string> = {
  light: 'Sunny',
  dark: 'Moon',
  blue: 'Cloudy'
}

function handleCommand(command: string | number | object): void {
  setTheme(String(command) as ThemeName)
}
</script>

<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="theme-switch" :title="t('layout.theme')">
      <IconRender :name="isDark ? themeIcons.dark : themeIcons[theme]" />
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="item in themes"
          :key="item"
          :command="item"
          :disabled="item === theme"
        >
          <IconRender :name="themeIcons[item]" />
          <span class="theme-switch__label">{{ t(themeLabelKeys[item]) }}</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<style scoped lang="scss">
.theme-switch {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  cursor: pointer;
  color: var(--admin-text-primary);
  border-radius: var(--admin-radius-md);
  transition: background-color var(--admin-transition);

  &:hover {
    background: var(--admin-bg-hover);
  }

  &__label {
    margin-left: 6px;
  }
}
</style>
