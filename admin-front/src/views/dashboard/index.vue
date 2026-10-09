<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'

import IconRender from '@/components/IconRender/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { getCount } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { SysCount, SysMeUser } from '@/types'
import { formatDateTime } from '@/utils/date'
import { readLoginTime } from '@/utils/storage'

/**
 * 工作台（任务书 21.2）。
 * 统计数据只走 /admin/system/sysauth/count（只校验登录），
 * **不要**用带权限点的列表接口凑总数（普通用户会拿到 200007 或显示 0）。
 * 计数是 Long → 字符串，展示前 Number() 转换。
 */
defineOptions({ name: 'DashboardPage' })

const { t } = useI18n()
const userStore = useUserStore()

/** 统计是否加载中 */
const counting = ref(false)
/** 统计结果；未加载完成时为 null，卡片显示占位符而不是假的 0 */
const counts = ref<SysCount | null>(null)

/** 当前用户信息 */
const userInfo = computed<SysMeUser | null>(() => userStore.userInfo)

/** 当前用户的角色编码 */
const roleCodes = computed<string[]>(() => userStore.roles ?? [])

/** 是否超级管理员 */
const isSuperAdmin = computed(() => userStore.superAdmin)

/** 显示名：优先昵称，其次用户名 */
const displayName = computed(
  () => userInfo.value?.nickname || userInfo.value?.username || ''
)

/** 前端版本（构建期注入） */
const frontendVersion = computed(() => import.meta.env.VITE_APP_VERSION || '')

/** 登录时间（登录成功时写入本地存储的毫秒时间戳） */
const loginTime = computed(() => formatDateTime(readLoginTime()))

/** 统计卡片：计数是字符串，展示时转数字 */
const statCards = computed(() => [
  {
    key: 'userCount',
    labelKey: 'dashboard.userCount',
    icon: 'User',
    value: counts.value ? Number(counts.value.userCount) : null
  },
  {
    key: 'roleCount',
    labelKey: 'dashboard.roleCount',
    icon: 'UserFilled',
    value: counts.value ? Number(counts.value.roleCount) : null
  },
  {
    key: 'menuCount',
    labelKey: 'dashboard.menuCount',
    icon: 'Menu',
    value: counts.value ? Number(counts.value.menuCount) : null
  }
])

/** 格式化统计数字：未加载完成时显示占位符，非法值按 0 处理 */
function formatCount(value: number | null): string {
  if (value === null) {
    return '—'
  }
  return Number.isFinite(value) ? String(value) : '0'
}

/** 加载统计（失败提示由请求层统一处理，这里只保持占位符） */
async function loadCounts(): Promise<void> {
  if (counting.value) {
    return
  }
  counting.value = true
  try {
    counts.value = await getCount()
  } catch {
    counts.value = null
  } finally {
    counting.value = false
  }
}

onMounted(() => {
  void loadCounts()
})
</script>

<template>
  <PageContainer>
    <!-- 欢迎区 -->
    <section class="welcome">
      <div class="welcome__main">
        <h3 class="welcome__title">{{ t('dashboard.welcome', { name: displayName }) }}</h3>
        <p class="welcome__subtitle">
          {{ userInfo?.username ?? '' }}
          <span v-if="userInfo?.email" class="welcome__sep">·</span>
          <span v-if="userInfo?.email">{{ userInfo.email }}</span>
        </p>
      </div>
      <el-tag v-if="isSuperAdmin" type="warning" effect="plain">
        {{ t('dashboard.superAdmin') }}
      </el-tag>
      <el-tag v-else type="info" effect="plain">{{ t('dashboard.normalUser') }}</el-tag>
    </section>

    <!-- 统计卡片：数据只来自 count 接口 -->
    <section class="stat-section">
      <header class="section-header">
        <h3 class="section-header__title">{{ t('dashboard.statOverview') }}</h3>
        <p class="section-header__desc">{{ t('dashboard.statHint') }}</p>
      </header>
      <div v-loading="counting" class="stat-grid">
        <div v-for="card in statCards" :key="card.key" class="stat-card">
          <span class="stat-card__icon">
            <IconRender :name="card.icon" :size="20" />
          </span>
          <div class="stat-card__body">
            <span class="stat-card__value">{{ formatCount(card.value) }}</span>
            <span class="stat-card__label">{{ t(card.labelKey) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 系统信息 -->
    <section class="info-section">
      <header class="section-header">
        <h3 class="section-header__title">{{ t('dashboard.systemInfo') }}</h3>
      </header>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('dashboard.currentUser')">
          {{ displayName }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('field.username')">
          {{ userInfo?.username ?? '' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.currentRoles')">
          <template v-if="roleCodes.length > 0">
            <el-tag
              v-for="code in roleCodes"
              :key="code"
              class="info-tag"
              size="small"
              effect="plain"
            >
              {{ code }}
            </el-tag>
          </template>
          <span v-else class="text-secondary">{{ t('dashboard.noRole') }}</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.superAdmin')">
          {{ isSuperAdmin ? t('common.yes') : t('common.no') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.email')">
          <span :class="{ 'text-secondary': !userInfo?.email }">
            {{ userInfo?.email || t('dashboard.notSet') }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.phone')">
          <span :class="{ 'text-secondary': !userInfo?.phone }">
            {{ userInfo?.phone || t('dashboard.notSet') }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.frontendVersion')">
          {{ frontendVersion }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('dashboard.loginTime')">
          {{ loginTime }}
        </el-descriptions-item>
      </el-descriptions>
    </section>
  </PageContainer>
</template>

<style scoped lang="scss">
/* PageContainer 已不再给子块留间距（查询区要与结果表格紧贴），工作台自己控制卡片间距 */
.welcome + .stat-section,
.stat-section + .info-section {
  margin-top: 12px;
}

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  background: var(--admin-bg-card);
  border: 1px solid var(--admin-border-color);
  border-radius: var(--admin-radius-card);
  box-shadow: var(--admin-shadow-card);

  &__main {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    color: var(--admin-text-primary);
  }

  &__subtitle {
    font-size: 13px;
    line-height: 20px;
    color: var(--admin-text-secondary);
  }

  &__sep {
    margin: 0 6px;
  }
}

.section-header {
  display: flex;
  flex-direction: column;
  gap: 2px;

  &__title {
    font-size: 14px;
    font-weight: 600;
    line-height: 22px;
    color: var(--admin-text-primary);
  }

  &__desc {
    font-size: 12px;
    line-height: 18px;
    color: var(--admin-text-secondary);
  }
}

.stat-section,
.info-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: var(--admin-bg-card);
  border: 1px solid var(--admin-border-color);
  border-radius: var(--admin-radius-card);
  box-shadow: var(--admin-shadow-card);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  min-height: 72px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--admin-bg-page);
  border: 1px solid var(--admin-border-color);
  border-radius: var(--admin-radius-md);

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    color: var(--admin-color-primary);
    background: var(--admin-color-primary-light-9);
    border-radius: var(--admin-radius-md);
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__value {
    font-size: 22px;
    font-weight: 600;
    line-height: 30px;
    color: var(--admin-text-primary);
    font-variant-numeric: tabular-nums;
  }

  &__label {
    font-size: 12px;
    line-height: 18px;
    color: var(--admin-text-secondary);
  }
}

.info-tag {
  margin-right: 6px;
}

@media (width <= 900px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
