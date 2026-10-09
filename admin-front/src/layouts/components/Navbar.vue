<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

import Breadcrumb from '@/components/Breadcrumb/index.vue'
import IconRender from '@/components/IconRender/index.vue'
import LangSwitch from '@/components/LangSwitch/index.vue'
import ThemeSwitch from '@/components/ThemeSwitch/index.vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { LOGIN_PATH } from '@/utils/constants'

/**
 * 顶栏：折叠按钮、面包屑、主题切换、语言切换、用户下拉（昵称 + 退出登录）。
 */
defineOptions({ name: 'LayoutNavbar' })

const { t } = useI18n()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const collapsed = computed(() => appStore.sidebarCollapsed)

/** 显示名：优先昵称，其次用户名 */
const displayName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || ''
)

/** 头像地址（可空，空则显示首字） */
const avatar = computed(() => userStore.userInfo?.avatar ?? '')

/** 昵称首字（无头像时作为占位） */
const initial = computed(() => (displayName.value ? displayName.value.slice(0, 1) : ''))

function toggleSidebar(): void {
  appStore.toggleSidebar()
}

/** 退出登录：确认 → 调后端（失败也继续）→ 清状态与动态路由 → 回登录页 */
async function handleLogout(): Promise<void> {
  try {
    await ElMessageBox.confirm(t('layout.logoutConfirm'), t('common.tip'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success(t('layout.logoutSuccess'))
  await router.replace(LOGIN_PATH)
}

function handleCommand(command: string | number | object): void {
  if (String(command) === 'logout') {
    void handleLogout()
  }
}
</script>

<template>
  <div class="navbar">
    <div class="navbar__left">
      <span
        class="navbar__collapse"
        :title="collapsed ? t('layout.expandSidebar') : t('layout.collapseSidebar')"
        @click="toggleSidebar"
      >
        <IconRender :name="collapsed ? 'Expand' : 'Fold'" />
      </span>
      <Breadcrumb />
    </div>

    <div class="navbar__right">
      <ThemeSwitch />
      <LangSwitch />
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="navbar__user">
          <el-avatar v-if="avatar" :size="26" :src="avatar" />
          <span v-else class="navbar__avatar">{{ initial }}</span>
          <span class="navbar__name">{{ displayName }}</span>
          <IconRender name="ArrowDown" />
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">
              <IconRender name="SwitchButton" />
              <span class="navbar__logout-text">{{ t('layout.logout') }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 16px;
  gap: 12px;

  &__left,
  &__right {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
  }

  &__collapse {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    cursor: pointer;
    border-radius: var(--admin-radius-md);
    color: var(--admin-text-primary);

    &:hover {
      background: var(--admin-bg-page);
    }
  }

  &__user {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    height: 32px;
    padding: 0 8px;
    cursor: pointer;
    border-radius: var(--admin-radius-md);
    color: var(--admin-text-primary);

    &:hover {
      background: var(--admin-bg-page);
    }
  }

  &__avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 26px;
    height: 26px;
    border-radius: 50%;
    font-size: 12px;
    color: #ffffff;
    background: var(--admin-color-primary);
  }

  &__name {
    font-size: 13px;
    max-width: 140px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__logout-text {
    margin-left: 6px;
  }
}
</style>
