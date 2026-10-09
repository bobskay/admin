<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import { HOME_PATH } from '@/utils/constants'

/** 403：无访问权限 */
defineOptions({ name: 'ErrorForbidden' })

const { t } = useI18n()
const router = useRouter()

function backHome(): void {
  void router.replace(HOME_PATH)
}
</script>

<template>
  <div class="error-page">
    <div class="error-page__code">403</div>
    <p class="error-page__desc">{{ t('common.forbiddenDesc') }}</p>
    <el-button type="primary" @click="backHome">
      {{ t('common.backHome') }}
    </el-button>
  </div>
</template>

<style scoped lang="scss">
.error-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 100vh;
  background: var(--admin-bg-page);
  color: var(--admin-text-primary);

  &__code {
    font-size: 64px;
    font-weight: 600;
    line-height: 1;
    color: var(--admin-color-primary);
  }

  &__desc {
    margin: 0;
    font-size: 14px;
    color: var(--admin-text-secondary);
  }
}
</style>
