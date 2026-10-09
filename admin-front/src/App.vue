<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { computed } from 'vue'

import { useAppStore } from '@/stores/app'

/**
 * 应用根组件：router-view + Element Plus 的 ElConfigProvider。
 * Element Plus 自带的组件文案（分页、日期选择、空表格等）通过语言包跟随当前语言。
 */
defineOptions({ name: 'App' })

const appStore = useAppStore()

const elementLocale = computed(() => (appStore.language === 'en-US' ? en : zhCn))
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>
