<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { object, string } from 'yup'

import { listRole } from '@/api/role'
import {
  USER_API,
  assignUserRoles,
  changeUserStatus,
  createUser,
  deleteUser,
  pageUser,
  resetUserPassword,
  updateUser
} from '@/api/user'
import DataTable from '@/components/DataTable/index.vue'
import DialogForm from '@/components/DialogForm/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/composables/useTable'
import { useYupValidation } from '@/composables/useYupValidation'
import { useUserStore } from '@/stores/user'
import type { SysRoleBrief, SysUserInfo, SysUserPageParams, TableColumn } from '@/types'
import { STATUS } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import {
  emailRule,
  nicknameRule,
  passwordRule,
  phoneRule,
  remarkRule,
  requiredStringArrayRule,
  statusRule,
  usernameRule
} from '@/utils/validate'

/**
 * 用户管理（任务书 21.3）。
 * - 查询、新增、编辑、删除、重置密码、启用/禁用、分配角色 全部用 v-has 包裹，标识取 USER_API.*；
 * - 入参字段名与后端 DTO 逐字一致（否则 100004 的 data.field 无法定位控件）。
 */
defineOptions({ name: 'SystemUserPage' })

const { t } = useI18n()
const userStore = useUserStore()

/* ------------------------------------------------------------ 查询条件 */

const STATUS_OPTIONS = [STATUS.ENABLED, STATUS.DISABLED]

/** 角色下拉数据（ROLE_API.list，只校验登录） */
const roleOptions = ref<SysRoleBrief[]>([])

const {
  loading,
  rows,
  total,
  query,
  selectedRows,
  handleSearch,
  handleReset,
  handleDelete,
  handlePageChange,
  handleSizeChange,
  handleSortChange,
  handleSelectionChange
} = useTable<SysUserInfo, SysUserPageParams>({
  fetch: pageUser,
  defaultQuery: { username: '', nickname: '', status: undefined, roleId: undefined },
  rowKey: 'id',
  remove: deleteUser
})

/**
 * 表格列定义。
 * 只给最小宽度，让表格按屏幕宽度自适应分配；操作列挂 cell-actions，
 * 按钮平铺排列、放不下时换行，因此不需要为了塞下按钮把列撑宽（撑宽会让整张表出现横向滚动条）。
 */
const columns: TableColumn<SysUserInfo>[] = [
  { prop: 'username', labelKey: 'field.username', minWidth: 110, slot: 'username' },
  { prop: 'nickname', labelKey: 'field.nickname', minWidth: 90 },
  { prop: 'email', labelKey: 'field.email', minWidth: 140, slot: 'email' },
  { prop: 'phone', labelKey: 'field.phone', minWidth: 110, slot: 'phone' },
  { prop: 'roles', labelKey: 'field.roles', minWidth: 110, slot: 'roles' },
  { prop: 'status', labelKey: 'field.status', width: 72, align: 'center', slot: 'status' },
  {
    prop: 'createTime',
    labelKey: 'common.createTime',
    minWidth: 145,
    sortable: true,
    formatter: (row) => formatDateTime(row.createTime)
  },
  {
    prop: 'action',
    labelKey: 'common.action',
    minWidth: 210,
    align: 'right',
    fixed: 'right',
    // 按钮不参与省略号截断，否则 nowrap 会把放不下的按钮直接裁掉
    showOverflowTooltip: false,
    cellClassName: 'cell-actions',
    slot: 'action'
  }
]

/** 状态下拉候选（值是数字） */
const statusFilterOptions = computed(() =>
  STATUS_OPTIONS.map((value) => ({
    value,
    label: value === STATUS.ENABLED ? t('common.enabled') : t('common.disabled')
  }))
)

/** 当前登录用户 id（Long 一律按字符串比较） */
const currentUserId = computed(() => String(userStore.userInfo?.id ?? ''))

/** 是否为当前登录用户 */
function isCurrentUser(row: SysUserInfo): boolean {
  return currentUserId.value !== '' && String(row.id) === currentUserId.value
}

/** 是否为超级管理员账号（不允许删除与禁用，与后端 200010 对齐） */
function isSuperAdminUser(row: SysUserInfo): boolean {
  return (row.roles ?? []).some((role) => role.code === 'admin')
}

/** 编辑 / 删除 / 启停用是否被保护 */
function isProtectedUser(row: SysUserInfo): boolean {
  return isCurrentUser(row) || isSuperAdminUser(row)
}

/** 保护原因提示：供按钮 title 使用 */
function protectedTip(row: SysUserInfo): string {
  if (isSuperAdminUser(row)) {
    return t('user.superAdminTips')
  }
  return isCurrentUser(row) ? t('user.currentUserTip') : ''
}

/** 加载角色下拉（失败时保持空列表，提示由请求层统一处理） */
async function loadRoleOptions(): Promise<void> {
  try {
    const list = await listRole()
    roleOptions.value = Array.isArray(list) ? list : []
  } catch {
    roleOptions.value = []
  }
}

/* -------------------------------------------------------------- 新增表单 */

interface UserCreateForm {
  username: string
  password: string
  nickname: string
  email: string
  phone: string
  status: number
  roleIds: string[]
  remark: string
}

const createVisible = ref(false)
const createSubmitting = ref(false)
/** 是否已尝试提交（避免弹窗刚打开就显示「角色不能为空」的红字） */
const createTouched = ref(false)

/** 新增规则：用户名 / 密码 / 昵称必填，角色至少一个 */
const createSchema = object({
  username: usernameRule(),
  password: passwordRule(),
  nickname: nicknameRule(),
  email: emailRule(),
  phone: phoneRule(),
  status: statusRule(),
  roleIds: requiredStringArrayRule(),
  remark: remarkRule()
})

const {
  formRef: createFormEl,
  form: createForm,
  errorFor: createErrorFor,
  submit: submitCreate,
  handleFieldChange: handleCreateFieldChange,
  resetForm: resetCreateForm
} = useYupValidation<UserCreateForm>(createSchema)

/** 角色字段的错误文案：yup 错误优先，其次首次提交后的「至少一个」提示 */
const roleIdsError = computed(() => {
  const error = createErrorFor('roleIds')
  if (error) {
    return error
  }
  return createTouched.value && createForm.roleIds.length === 0 ? t('validation.required') : ''
})

/** 打开新增弹窗 */
function openCreate(): void {
  createTouched.value = false
  resetCreateForm({
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    status: STATUS.ENABLED,
    roleIds: [],
    remark: ''
  })
  createVisible.value = true
}

/** 新增提交：只提交 DTO 需要的字段 */
async function handleCreateSubmit(): Promise<void> {
  if (createSubmitting.value) {
    return
  }
  createTouched.value = true
  createSubmitting.value = true
  try {
    const executed = await submitCreate(async (values) => {
      await createUser({
        username: values.username.trim(),
        password: values.password,
        nickname: values.nickname.trim(),
        email: values.email || null,
        phone: values.phone || null,
        status: values.status,
        roleIds: values.roleIds,
        remark: values.remark || null
      })
    })
    if (!executed) {
      return
    }
    ElMessage.success(t('user.createSuccess'))
    createVisible.value = false
    await handleSearch()
  } finally {
    createSubmitting.value = false
  }
}

/* -------------------------------------------------------------- 编辑表单 */

interface UserEditForm {
  id: string
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  remark: string
}

const editVisible = ref(false)
const editSubmitting = ref(false)
/** 正在编辑的用户名（弹窗标题用） */
const editingUsername = ref('')

/** 编辑规则：不含密码与角色（角色走独立的分配角色入口） */
const editSchema = object({
  nickname: nicknameRule(),
  email: emailRule(),
  phone: phoneRule(),
  status: statusRule(),
  remark: remarkRule()
})

const {
  formRef: editFormEl,
  form: editForm,
  errorFor: editErrorFor,
  submit: submitEdit,
  handleFieldChange: handleEditFieldChange,
  resetForm: resetEditForm
} = useYupValidation<UserEditForm>(editSchema)

/** 打开编辑弹窗：回填列表行数据（null 归一化为空字符串，保证输入框受控） */
function openEdit(row: SysUserInfo): void {
  editingUsername.value = row.username
  resetEditForm({
    id: String(row.id),
    username: row.username ?? '',
    nickname: row.nickname ?? '',
    email: row.email ?? '',
    phone: row.phone ?? '',
    status: Number(row.status),
    remark: row.remark ?? ''
  })
  editVisible.value = true
}

/** 编辑提交：不含密码与角色 */
async function handleEditSubmit(): Promise<void> {
  if (editSubmitting.value) {
    return
  }
  editSubmitting.value = true
  try {
    const executed = await submitEdit(async (values) => {
      await updateUser({
        id: values.id,
        nickname: values.nickname.trim(),
        email: values.email || null,
        phone: values.phone || null,
        status: values.status,
        remark: values.remark || null
      })
    })
    if (!executed) {
      return
    }
    ElMessage.success(t('user.updateSuccess'))
    editVisible.value = false
    await handleSearch()
  } finally {
    editSubmitting.value = false
  }
}

/* ------------------------------------------------------------ 分配角色 */

const roleVisible = ref(false)
const roleSubmitting = ref(false)
const roleTargetId = ref('')
const roleTargetName = ref('')
const assignRoleIds = ref<string[]>([])

/** 分配角色时角色可清空（与新增用户的「至少一个」规则不同，见任务书 13.2） */
function openAssignRoles(row: SysUserInfo): void {
  roleTargetId.value = String(row.id)
  roleTargetName.value = row.nickname || row.username
  assignRoleIds.value = (row.roles ?? []).map((role) => String(role.id))
  roleVisible.value = true
}

async function handleAssignRolesSubmit(): Promise<void> {
  if (roleSubmitting.value) {
    return
  }
  roleSubmitting.value = true
  try {
    await assignUserRoles(roleTargetId.value, assignRoleIds.value)
    ElMessage.success(t('user.assignRolesSuccess'))
    roleVisible.value = false
    await handleSearch()
  } finally {
    roleSubmitting.value = false
  }
}

/* ------------------------------------------------------------ 重置密码 */

interface ResetPasswordForm {
  id: string
  newPassword: string
  confirmPassword: string
}

const resetVisible = ref(false)
const resetSubmitting = ref(false)
const resetTargetName = ref('')

/** 重置密码规则：新密码 8-32 位含字母数字，两次输入必须一致 */
const resetSchema = object({
  newPassword: passwordRule(),
  confirmPassword: string()
    .required('validation.required')
    .test('sameAsNewPassword', 'user.passwordConfirmMismatch', function (value) {
      return value === this.parent.newPassword
    })
})

const {
  formRef: resetFormEl,
  form: resetForm,
  errorFor: resetErrorFor,
  submit: submitReset,
  handleFieldChange: handleResetFieldChange,
  resetForm: resetPasswordForm
} = useYupValidation<ResetPasswordForm>(resetSchema)

function openResetPassword(row: SysUserInfo): void {
  resetTargetName.value = row.nickname || row.username
  resetPasswordForm({ id: String(row.id), newPassword: '', confirmPassword: '' })
  resetVisible.value = true
}

async function handleResetPasswordSubmit(): Promise<void> {
  if (resetSubmitting.value) {
    return
  }
  resetSubmitting.value = true
  try {
    const executed = await submitReset(async (values) => {
      // confirmPassword 只用于前端比对，不提交给后端
      await resetUserPassword(values.id, values.newPassword)
    })
    if (!executed) {
      return
    }
    ElMessage.success(t('user.resetPasswordSuccess'))
    resetVisible.value = false
  } finally {
    resetSubmitting.value = false
  }
}

/* -------------------------------------------------------- 启停用与删除 */

/** 启用 / 禁用：失败时提示并重新加载（状态以后端为准） */
async function handleStatusChange(row: SysUserInfo, next: number): Promise<void> {
  try {
    await changeUserStatus(String(row.id), next)
    ElMessage.success(t('user.changeStatusSuccess'))
    await handleSearch()
  } catch {
    ElMessage.warning(t('user.statusChangeFailed'))
  }
}

/** 删除单条：走同一个批量入口 deleteUser(ids) */
async function handleDeleteRow(row: SysUserInfo): Promise<void> {
  await handleDelete({
    ids: [String(row.id)],
    confirmText: t('user.deleteConfirm', { name: row.nickname || row.username }),
    successKey: 'user.deleteSuccess'
  })
}

/** 批量删除选中行 */
async function handleBatchDelete(): Promise<void> {
  await handleDelete({
    confirmKey: 'common.batchDeleteConfirm',
    successKey: 'user.deleteSuccess'
  })
}

onMounted(() => {
  void loadRoleOptions()
})
</script>

<template>
  <PageContainer>
    <SearchForm :search-perm="USER_API.page" @search="handleSearch" @reset="handleReset">
      <el-form-item :label="t('field.username')">
        <el-input
          v-model="query.username"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.username') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.nickname')">
        <el-input
          v-model="query.nickname"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.nickname') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.status')">
        <el-select
          v-model="query.status"
          clearable
          class="filter-control"
          :placeholder="t('user.statusPlaceholder')"
        >
          <el-option
            v-for="item in statusFilterOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('field.roleId')">
        <el-select
          v-model="query.roleId"
          clearable
          filterable
          class="filter-control"
          :placeholder="t('common.selectPlaceholder', { label: t('field.roleId') })"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role.id"
            :value="role.id"
            :label="role.name"
          />
        </el-select>
      </el-form-item>
    </SearchForm>

    <DataTable
      :page="query.pageNum"
      :size="query.pageSize"
      :data="rows"
      :loading="loading"
      :columns="columns"
      :total="total"
      row-key="id"
      show-selection
      @update:page="handlePageChange"
      @update:size="handleSizeChange"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
    >
      <template #toolbar>
        <el-button v-has="USER_API.create" type="primary" @click="openCreate">
          {{ t('user.create') }}
        </el-button>
        <el-button
          v-has="USER_API.remove"
          type="danger"
          plain
          :disabled="selectedRows.length === 0"
          @click="handleBatchDelete"
        >
          {{ t('common.batchDelete') }}
        </el-button>
        <span v-if="selectedRows.length > 0" class="toolbar-tip">
          {{ t('common.selectedCount', { count: selectedRows.length }) }}
        </span>
      </template>

      <template #username="{ row }">
        <div class="cell-user">
          <span class="cell-user__name">{{ row.username }}</span>
          <el-tag v-if="isSuperAdminUser(row)" size="small" type="warning" effect="plain">
            {{ t('dashboard.superAdmin') }}
          </el-tag>
        </div>
      </template>

      <template #email="{ row }">
        <span v-if="row.email">{{ row.email }}</span>
        <span v-else class="text-secondary">{{ t('user.noEmail') }}</span>
      </template>

      <template #phone="{ row }">
        <span v-if="row.phone">{{ row.phone }}</span>
        <span v-else class="text-secondary">{{ t('user.noPhone') }}</span>
      </template>

      <template #roles="{ row }">
        <template v-if="(row.roles ?? []).length > 0">
          <el-tag
            v-for="role in row.roles"
            :key="role.id"
            class="cell-tag"
            size="small"
            effect="plain"
          >
            {{ role.name }}
          </el-tag>
        </template>
        <span v-else class="text-secondary">{{ t('user.noRole') }}</span>
      </template>

      <template #status="{ row }">
        <!-- 有启停用权限用开关；无权限时 v-has 会整体移除开关，退化为只读标签 -->
        <el-switch
          v-has="USER_API.changeStatus"
          :model-value="Number(row.status) === STATUS.ENABLED"
          :disabled="isProtectedUser(row)"
          :title="protectedTip(row)"
          @update:model-value="
            (value) => handleStatusChange(row, value ? STATUS.ENABLED : STATUS.DISABLED)
          "
        />
        <el-tag
          v-if="!userStore.hasPermission(USER_API.changeStatus)"
          size="small"
          :type="Number(row.status) === STATUS.ENABLED ? 'success' : 'info'"
          effect="plain"
        >
          {{ Number(row.status) === STATUS.ENABLED ? t('common.enabled') : t('common.disabled') }}
        </el-tag>
      </template>

      <template #action="{ row }">
        <el-button
          v-has="USER_API.update"
          link
          type="primary"
          :disabled="isProtectedUser(row)"
          :title="protectedTip(row)"
          @click="openEdit(row)"
        >
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPermission(USER_API.remove)"
          link
          type="danger"
          :disabled="isProtectedUser(row)"
          :title="protectedTip(row)"
          @click="handleDeleteRow(row)"
        >
          {{ t('common.delete') }}
        </el-button>
        <el-button
          v-if="userStore.hasPermission(USER_API.resetPassword)"
          link
          type="primary"
          @click="openResetPassword(row)"
        >
          {{ t('user.resetPassword') }}
        </el-button>
        <el-button
          v-if="userStore.hasPermission(USER_API.assignRoles)"
          link
          type="primary"
          @click="openAssignRoles(row)"
        >
          {{ t('user.assignRoles') }}
        </el-button>
      </template>
    </DataTable>

    <!-- 新增用户 -->
    <DialogForm
      v-model="createVisible"
      :title="t('user.create')"
      width="560px"
      :loading="createSubmitting"
      :form-instance="createFormEl"
      @submit="handleCreateSubmit"
    >
      <el-form ref="createFormEl" label-width="88px" @submit.prevent>
        <el-form-item
          :label="t('field.username')"
          prop="username"
          :error="createErrorFor('username')"
        >
          <el-input
            :model-value="createForm.username"
            :placeholder="t('user.usernamePlaceholder')"
            @update:model-value="handleCreateFieldChange('username', $event)"
          />
          <p class="form-tip">{{ t('user.usernameHint') }}</p>
        </el-form-item>
        <el-form-item
          :label="t('field.password')"
          prop="password"
          :error="createErrorFor('password')"
        >
          <el-input
            :model-value="createForm.password"
            type="password"
            show-password
            :placeholder="t('login.passwordPlaceholder')"
            @update:model-value="handleCreateFieldChange('password', $event)"
          />
          <p class="form-tip">{{ t('user.passwordRule') }}</p>
        </el-form-item>
        <el-form-item
          :label="t('field.nickname')"
          prop="nickname"
          :error="createErrorFor('nickname')"
        >
          <el-input
            :model-value="createForm.nickname"
            :placeholder="t('user.nicknamePlaceholder')"
            @update:model-value="handleCreateFieldChange('nickname', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.email')" prop="email" :error="createErrorFor('email')">
          <el-input
            :model-value="createForm.email"
            :placeholder="t('user.emailPlaceholder')"
            @update:model-value="handleCreateFieldChange('email', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.phone')" prop="phone" :error="createErrorFor('phone')">
          <el-input
            :model-value="createForm.phone"
            :placeholder="t('user.phonePlaceholder')"
            @update:model-value="handleCreateFieldChange('phone', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.status')" prop="status" :error="createErrorFor('status')">
          <el-radio-group
            :model-value="createForm.status"
            @update:model-value="handleCreateFieldChange('status', Number($event))"
          >
            <el-radio :value="STATUS.ENABLED">{{ t('common.enabled') }}</el-radio>
            <el-radio :value="STATUS.DISABLED">{{ t('common.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('field.roleIds')" prop="roleIds" :error="roleIdsError">
          <el-select
            :model-value="createForm.roleIds"
            multiple
            filterable
            class="form-control"
            :placeholder="t('user.selectRolesPlaceholder')"
            @update:model-value="handleCreateFieldChange('roleIds', $event)"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :value="role.id"
              :label="role.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('field.remark')" prop="remark" :error="createErrorFor('remark')">
          <el-input
            :model-value="createForm.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('user.remarkPlaceholder')"
            @update:model-value="handleCreateFieldChange('remark', $event)"
          />
        </el-form-item>
      </el-form>
    </DialogForm>

    <!-- 编辑用户：不含密码与角色 -->
    <DialogForm
      v-model="editVisible"
      :title="t('user.editUserTitle', { name: editingUsername })"
      width="560px"
      :loading="editSubmitting"
      :form-instance="editFormEl"
      @submit="handleEditSubmit"
    >
      <el-form ref="editFormEl" label-width="88px" @submit.prevent>
        <el-form-item :label="t('field.username')" prop="username">
          <el-input :model-value="editForm.username" disabled />
        </el-form-item>
        <el-form-item
          :label="t('field.nickname')"
          prop="nickname"
          :error="editErrorFor('nickname')"
        >
          <el-input
            :model-value="editForm.nickname"
            :placeholder="t('user.nicknamePlaceholder')"
            @update:model-value="handleEditFieldChange('nickname', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.email')" prop="email" :error="editErrorFor('email')">
          <el-input
            :model-value="editForm.email"
            :placeholder="t('user.emailPlaceholder')"
            @update:model-value="handleEditFieldChange('email', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.phone')" prop="phone" :error="editErrorFor('phone')">
          <el-input
            :model-value="editForm.phone"
            :placeholder="t('user.phonePlaceholder')"
            @update:model-value="handleEditFieldChange('phone', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.status')" prop="status" :error="editErrorFor('status')">
          <el-radio-group
            :model-value="editForm.status"
            @update:model-value="handleEditFieldChange('status', Number($event))"
          >
            <el-radio :value="STATUS.ENABLED">{{ t('common.enabled') }}</el-radio>
            <el-radio :value="STATUS.DISABLED">{{ t('common.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('field.remark')" prop="remark" :error="editErrorFor('remark')">
          <el-input
            :model-value="editForm.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('user.remarkPlaceholder')"
            @update:model-value="handleEditFieldChange('remark', $event)"
          />
        </el-form-item>
      </el-form>
    </DialogForm>

    <!-- 分配角色：roleIds 可为空数组（清空角色） -->
    <DialogForm
      v-model="roleVisible"
      :title="t('user.assignRolesTitle', { name: roleTargetName })"
      width="460px"
      :loading="roleSubmitting"
      @submit="handleAssignRolesSubmit"
    >
      <el-form label-width="72px" @submit.prevent>
        <el-form-item :label="t('field.roleIds')">
          <el-select
            v-model="assignRoleIds"
            multiple
            filterable
            class="form-control"
            :placeholder="t('common.selectPlaceholder', { label: t('field.roleIds') })"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :value="role.id"
              :label="role.name"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </DialogForm>

    <!-- 重置密码 -->
    <DialogForm
      v-model="resetVisible"
      :title="t('user.resetPasswordTitle', { name: resetTargetName })"
      width="460px"
      :loading="resetSubmitting"
      :form-instance="resetFormEl"
      @submit="handleResetPasswordSubmit"
    >
      <el-form ref="resetFormEl" label-width="88px" @submit.prevent>
        <el-form-item
          :label="t('field.newPassword')"
          prop="newPassword"
          :error="resetErrorFor('newPassword')"
        >
          <el-input
            :model-value="resetForm.newPassword"
            type="password"
            show-password
            :placeholder="t('login.passwordPlaceholder')"
            @update:model-value="handleResetFieldChange('newPassword', $event)"
          />
          <p class="form-tip">{{ t('user.resetPasswordRule') }}</p>
        </el-form-item>
        <el-form-item
          :label="t('field.password')"
          prop="confirmPassword"
          :error="resetErrorFor('confirmPassword')"
        >
          <el-input
            :model-value="resetForm.confirmPassword"
            type="password"
            show-password
            :placeholder="t('user.passwordConfirmPlaceholder')"
            @update:model-value="handleResetFieldChange('confirmPassword', $event)"
          />
        </el-form-item>
      </el-form>
    </DialogForm>
  </PageContainer>
</template>

<style scoped lang="scss">
.filter-control {
  /* 不写死宽度：由选中项 / placeholder 的文字长度决定，选中长角色名时自动变宽 */
  width: auto;
  min-width: 110px;
}

.form-control {
  width: 100%;
}

.form-tip {
  margin-top: 2px;
  font-size: 12px;
  line-height: 18px;
  color: var(--admin-text-secondary);
}

.toolbar-tip {
  font-size: 12px;
  color: var(--admin-text-secondary);
}

.cell-user {
  /* 单元格内的容器一律用行内级，避免与树形表格的缩进/展开图标抢行、或撑高行高 */
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  gap: 6px;

  &__name {
    font-weight: 500;
  }
}

.cell-tag {
  margin-right: 6px;
}
</style>
