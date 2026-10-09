<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { object } from 'yup'

import { listMenu } from '@/api/menu'
import {
  ROLE_API,
  assignRoleMenus,
  createRole,
  deleteRole,
  getRoleMenuIds,
  pageRole,
  updateRole
} from '@/api/role'
import DataTable from '@/components/DataTable/index.vue'
import DialogForm from '@/components/DialogForm/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import SearchForm from '@/components/SearchForm/index.vue'
import { useTable } from '@/composables/useTable'
import { useYupValidation } from '@/composables/useYupValidation'
import { useUserStore } from '@/stores/user'
import type { SysMenuRoute, SysRoleInfo, SysRolePageParams, TableColumn } from '@/types'
import { STATUS } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import {
  rangeNumberRule,
  remarkRule,
  requiredString,
  roleCodeRule,
  statusRule
} from '@/utils/validate'

/**
 * 角色管理（任务书 21.4）。
 * - 内置超级管理员角色（code === 'admin'）禁用编码编辑与删除，界面给出明确提示；
 * - 分配菜单权限：树为 MENU_API.list（全部启用菜单），回显走 ROLE_API.menuIds，
 *   保存走 ROLE_API.assignMenus（接口内已对 menuIds 去重）；
 * - 每个角色单独配置权限，弹窗打开时不预勾选任何权限，等待回显接口返回。
 */
defineOptions({ name: 'SystemRolePage' })

const { t } = useI18n()
const userStore = useUserStore()

/** 内置超级管理员角色编码 */
const BUILTIN_ADMIN_CODE = 'admin'

const STATUS_OPTIONS = [STATUS.ENABLED, STATUS.DISABLED]

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
} = useTable<SysRoleInfo, SysRolePageParams>({
  fetch: pageRole,
  defaultQuery: { name: '', code: '', status: undefined },
  rowKey: 'id',
  remove: deleteRole
})

/** 表格列定义（操作列挂 cell-actions：按钮平铺，放不下时换行而不是裁切） */
const columns: TableColumn<SysRoleInfo>[] = [
  { prop: 'name', labelKey: 'field.name', minWidth: 130 },
  { prop: 'code', labelKey: 'field.code', minWidth: 140, slot: 'code' },
  { prop: 'sort', labelKey: 'field.sort', width: 76, align: 'center', sortable: true },
  { prop: 'status', labelKey: 'field.status', width: 86, align: 'center', slot: 'status' },
  { prop: 'remark', labelKey: 'field.remark', minWidth: 150, slot: 'remark' },
  {
    prop: 'createTime',
    labelKey: 'common.createTime',
    minWidth: 150,
    sortable: true,
    formatter: (row) => formatDateTime(row.createTime)
  },
  {
    prop: 'action',
    labelKey: 'common.action',
    minWidth: 165,
    align: 'right',
    fixed: 'right',
    showOverflowTooltip: false,
    cellClassName: 'cell-actions',
    slot: 'action'
  }
]

/** 状态下拉候选 */
const statusFilterOptions = computed(() =>
  STATUS_OPTIONS.map((value) => ({
    value,
    label: value === STATUS.ENABLED ? t('common.enabled') : t('common.disabled')
  }))
)

/** 是否内置超级管理员角色（不允许改 code、不允许删除） */
function isBuiltinRole(row: SysRoleInfo): boolean {
  return String(row.code).toLowerCase() === BUILTIN_ADMIN_CODE
}

/* -------------------------------------------------------- 新增 / 编辑表单 */

interface RoleForm {
  id: string
  name: string
  code: string
  sort: number
  status: number
  remark: string
}

const editVisible = ref(false)
const editSubmitting = ref(false)
/** 当前编辑的行：新增时为 null */
const editingRow = ref<SysRoleInfo | null>(null)

/** 是否是内置角色的编辑（code 只读） */
const isBuiltinEditing = computed(() =>
  editingRow.value ? isBuiltinRole(editingRow.value) : false
)
/** 弹窗标题 */
const formTitle = computed(() => {
  if (!editingRow.value) {
    return t('role.createTitle')
  }
  return t('role.editTitle', { name: editingRow.value.name })
})

/** 新增 / 编辑规则：角色名与编码必填，排序 0-9999，状态取值受限 */
const roleSchema = object({
  name: requiredString().max(50, 'validation.len'),
  code: roleCodeRule(),
  sort: rangeNumberRule(0, 9999),
  status: statusRule(),
  remark: remarkRule()
})

const {
  formRef: roleFormEl,
  form: roleForm,
  errorFor: roleErrorFor,
  submit: submitRole,
  handleFieldChange: handleRoleFieldChange,
  resetForm: resetRoleForm
} = useYupValidation<RoleForm>(roleSchema)

/** 打开新增弹窗 */
function openCreate(): void {
  editingRow.value = null
  resetRoleForm({
    id: '',
    name: '',
    code: '',
    sort: 0,
    status: STATUS.ENABLED,
    remark: ''
  })
  editVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(row: SysRoleInfo): void {
  editingRow.value = row
  resetRoleForm({
    id: String(row.id),
    name: row.name ?? '',
    code: row.code ?? '',
    sort: Number(row.sort ?? 0),
    status: Number(row.status),
    remark: row.remark ?? ''
  })
  editVisible.value = true
}

/** 提交新增 / 编辑：只提交 DTO 需要的字段（新增时不带 id） */
async function handleRoleSubmit(): Promise<void> {
  if (editSubmitting.value) {
    return
  }
  editSubmitting.value = true
  try {
    const executed = await submitRole(async (values) => {
      const payload = {
        name: values.name.trim(),
        // 内置角色的编码不允许修改，始终沿用原值
        code: isBuiltinEditing.value ? (editingRow.value?.code ?? values.code) : values.code.trim(),
        sort: Number(values.sort),
        status: values.status,
        remark: values.remark || null
      }
      if (values.id) {
        await updateRole({ ...payload, id: values.id })
      } else {
        await createRole(payload)
      }
    })
    if (!executed) {
      return
    }
    ElMessage.success(editingRow.value ? t('role.updateSuccess') : t('role.createSuccess'))
    editVisible.value = false
    await handleSearch()
  } finally {
    editSubmitting.value = false
  }
}

/* ---------------------------------------------------------- 删除与批量 */

async function handleDeleteRow(row: SysRoleInfo): Promise<void> {
  await handleDelete({
    ids: [String(row.id)],
    confirmText: t('role.deleteConfirm', { name: row.name }),
    successKey: 'role.deleteSuccess'
  })
}

async function handleBatchDelete(): Promise<void> {
  await handleDelete({
    confirmKey: 'common.batchDeleteConfirm',
    successKey: 'role.deleteSuccess'
  })
}

/* ------------------------------------------------------------ 分配菜单权限 */

const menuDialogVisible = ref(false)
const menuLoading = ref(false)
const menuSubmitting = ref(false)
/** 树数据（MENU_API.list：全部启用菜单，含目录/菜单/按钮三级） */
const menuTree = ref<SysMenuRoute[]>([])
/** 目标角色 */
const menuTargetRole = ref<SysRoleInfo | null>(null)
/** 当前勾选的叶子节点 */
const checkedKeys = ref<string[]>([])
/** 当前半选的父节点 */
const halfCheckedKeys = ref<string[]>([])
/** 树是否全部展开 */
const treeExpandAll = ref(true)

/** el-tree 实例（只用到这几个公开方法） */
interface MenuTreeInstance {
  setCheckedKeys: (keys: Array<string | number>) => void
  getCheckedKeys: () => Array<string | number>
  getHalfCheckedKeys: () => Array<string | number>
  store?: { _getAllNodes?: () => Array<{ expanded: boolean }> }
}
const menuTreeRef = ref<MenuTreeInstance | null>(null)

/** 抽屉标题中的角色名 */
const menuTargetName = computed(() => menuTargetRole.value?.name ?? '')
/** 目标角色是否已禁用（禁用角色的权限不生效，给出提示） */
const menuTargetDisabled = computed(
  () => Number(menuTargetRole.value?.status) === STATUS.DISABLED
)

/** 扁平化取全部节点 id（全选用） */
function flattenIds(nodes: SysMenuRoute[]): string[] {
  const result: string[] = []
  const walk = (list: SysMenuRoute[]): void => {
    list.forEach((node) => {
      result.push(String(node.id))
      if (node.children && node.children.length > 0) {
        walk(node.children)
      }
    })
  }
  walk(nodes)
  return result
}

/** 全部节点 id */
const allMenuIds = computed(() => flattenIds(menuTree.value))

/** 已勾选（含半选）数量 */
const checkedCount = computed(
  () => new Set<string>([...checkedKeys.value, ...halfCheckedKeys.value]).size
)

/** 展开 / 折叠全部：default-expand-all 只在首次渲染生效，这里直接改各节点的 expanded */
function toggleExpandAll(): void {
  const next = !treeExpandAll.value
  treeExpandAll.value = next
  const nodes = menuTreeRef.value?.store?._getAllNodes?.() ?? []
  nodes.forEach((node) => {
    node.expanded = next
  })
}

/** 全选 / 清空 */
function handleCheckAll(): void {
  checkedKeys.value = [...allMenuIds.value]
  halfCheckedKeys.value = []
}

function handleClearAll(): void {
  checkedKeys.value = []
  halfCheckedKeys.value = []
}

/** 勾选/取消勾选：以 el-tree 的勾选状态为唯一事实来源 */
function handleTreeCheck(
  _node: unknown,
  info: { checkedKeys?: Array<string | number>; halfCheckedKeys?: Array<string | number> }
): void {
  checkedKeys.value = (info?.checkedKeys ?? []).map((key) => String(key))
  halfCheckedKeys.value = (info?.halfCheckedKeys ?? []).map((key) => String(key))
}

/** 打开分配权限弹窗 */
async function openAssignMenus(row: SysRoleInfo): Promise<void> {
  menuTargetRole.value = row
  // 每个角色单独配置：先清空，避免上一条角色残留的勾选被带过来
  menuTree.value = []
  checkedKeys.value = []
  halfCheckedKeys.value = []
  treeExpandAll.value = true
  menuDialogVisible.value = true
  menuLoading.value = true
  try {
    const [tree, ids] = await Promise.all([listMenu(), getRoleMenuIds(String(row.id))])
    menuTree.value = Array.isArray(tree) ? tree : []
    // 回显接口只含叶子节点；el-tree 设叶子后会自动推导父节点的半选/全选
    checkedKeys.value = Array.isArray(ids) ? ids.map((id) => String(id)) : []
    await nextTick()
    menuTreeRef.value?.setCheckedKeys(checkedKeys.value)
  } catch {
    // 失败提示已由请求层统一处理，这里保持空树并给出说明
    menuTree.value = []
    ElMessage.error(t('role.menuLoadFailed'))
  } finally {
    menuLoading.value = false
  }
}

/** 保存菜单权限：提交勾选 + 半选的父节点 id（去重后交给接口） */
async function handleAssignMenusSubmit(): Promise<void> {
  if (menuSubmitting.value) {
    return
  }
  const roleId = menuTargetRole.value ? String(menuTargetRole.value.id) : ''
  if (!roleId) {
    return
  }
  const checked = (menuTreeRef.value?.getCheckedKeys() ?? []).map((key) => String(key))
  const halfChecked = (menuTreeRef.value?.getHalfCheckedKeys() ?? []).map((key) => String(key))
  // 半选父节点一并提交，保证父目录可见；去重后提交
  const menuIds = Array.from(new Set([...checked, ...halfChecked]))

  menuSubmitting.value = true
  try {
    await assignRoleMenus(roleId, menuIds)
    ElMessage.success(t('role.assignMenusSuccess'))
    menuDialogVisible.value = false
  } finally {
    menuSubmitting.value = false
  }
}
</script>

<template>
  <PageContainer>
    <SearchForm :search-perm="ROLE_API.page" @search="handleSearch" @reset="handleReset">
      <el-form-item :label="t('field.name')">
        <el-input
          v-model="query.name"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.name') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.code')">
        <el-input
          v-model="query.code"
          clearable
          :placeholder="t('common.inputPlaceholder', { label: t('field.code') })"
        />
      </el-form-item>
      <el-form-item :label="t('field.status')">
        <el-select
          v-model="query.status"
          clearable
          class="filter-control"
          :placeholder="t('role.statusPlaceholder')"
        >
          <el-option
            v-for="item in statusFilterOptions"
            :key="item.value"
            :value="item.value"
            :label="item.label"
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
        <el-button v-has="ROLE_API.create" type="primary" @click="openCreate">
          {{ t('role.create') }}
        </el-button>
        <el-button
          v-has="ROLE_API.remove"
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

      <template #code="{ row }">
        <div class="cell-code">
          <span>{{ row.code }}</span>
          <el-tag v-if="isBuiltinRole(row)" size="small" type="warning" effect="plain">
            {{ t('dashboard.superAdmin') }}
          </el-tag>
        </div>
      </template>

      <template #status="{ row }">
        <el-tag
          size="small"
          :type="Number(row.status) === STATUS.ENABLED ? 'success' : 'info'"
          effect="plain"
        >
          {{ Number(row.status) === STATUS.ENABLED ? t('common.enabled') : t('common.disabled') }}
        </el-tag>
      </template>

      <template #remark="{ row }">
        <span v-if="row.remark">{{ row.remark }}</span>
        <span v-else class="text-secondary">{{ t('dashboard.notSet') }}</span>
      </template>

      <template #action="{ row }">
        <el-button v-has="ROLE_API.update" link type="primary" @click="openEdit(row)">
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPermission(ROLE_API.remove)"
          link
          type="danger"
          :disabled="isBuiltinRole(row)"
          :title="isBuiltinRole(row) ? t('role.builtinProtected') : ''"
          @click="handleDeleteRow(row)"
        >
          {{ t('common.delete') }}
        </el-button>
        <el-button v-has="ROLE_API.assignMenus" link type="primary" @click="openAssignMenus(row)">
          {{ t('role.assignMenus') }}
        </el-button>
        <el-tooltip v-if="isBuiltinRole(row)" :content="t('role.builtinProtected')" placement="top">
          <el-icon class="cell-warning"><WarningFilled /></el-icon>
        </el-tooltip>
      </template>
    </DataTable>

    <!-- 新增 / 编辑角色 -->
    <DialogForm
      v-model="editVisible"
      :title="formTitle"
      width="520px"
      :loading="editSubmitting"
      :form-instance="roleFormEl"
      @submit="handleRoleSubmit"
    >
      <el-form ref="roleFormEl" label-width="88px" @submit.prevent>
        <el-form-item :label="t('field.name')" prop="name" :error="roleErrorFor('name')">
          <el-input
            :model-value="roleForm.name"
            :placeholder="t('role.namePlaceholder')"
            @update:model-value="handleRoleFieldChange('name', $event)"
          />
        </el-form-item>
        <el-form-item :label="t('field.code')" prop="code" :error="roleErrorFor('code')">
          <el-input
            :model-value="roleForm.code"
            :disabled="isBuiltinEditing"
            :placeholder="t('role.codePlaceholder')"
            @update:model-value="handleRoleFieldChange('code', $event)"
          />
          <p class="form-tip">
            {{ isBuiltinEditing ? t('role.builtinProtected') : t('role.codeTips') }}
          </p>
        </el-form-item>
        <el-form-item :label="t('field.sort')" prop="sort" :error="roleErrorFor('sort')">
          <el-input-number
            :model-value="roleForm.sort"
            :min="0"
            :max="9999"
            controls-position="right"
            @update:model-value="handleRoleFieldChange('sort', Number($event))"
          />
        </el-form-item>
        <el-form-item :label="t('field.status')" prop="status" :error="roleErrorFor('status')">
          <el-radio-group
            :model-value="roleForm.status"
            :disabled="isBuiltinEditing"
            @update:model-value="handleRoleFieldChange('status', Number($event))"
          >
            <el-radio :value="STATUS.ENABLED">{{ t('common.enabled') }}</el-radio>
            <el-radio :value="STATUS.DISABLED">{{ t('common.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="t('field.remark')" prop="remark" :error="roleErrorFor('remark')">
          <el-input
            :model-value="roleForm.remark"
            type="textarea"
            :rows="2"
            :placeholder="t('role.remarkPlaceholder')"
            @update:model-value="handleRoleFieldChange('remark', $event)"
          />
        </el-form-item>
      </el-form>
    </DialogForm>

    <!-- 分配菜单权限（弹窗） -->
    <DialogForm
      v-model="menuDialogVisible"
      width="560px"
      :title="t('role.assignMenusTitle', { name: menuTargetName })"
      :loading="menuSubmitting"
      @submit="handleAssignMenusSubmit"
    >
      <div class="menu-panel">
        <el-alert
          v-if="menuTargetDisabled"
          type="warning"
          :title="t('role.disabledTips')"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="checkedCount === 0 && !menuLoading"
          type="info"
          :title="t('role.noMenuChecked')"
          :closable="false"
          show-icon
        />

        <div class="menu-panel__toolbar">
          <el-button link type="primary" @click="toggleExpandAll">
            {{ treeExpandAll ? t('role.collapseAll') : t('role.expandAll') }}
          </el-button>
          <el-button link type="primary" @click="handleCheckAll">
            {{ t('role.selectAllMenus') }}
          </el-button>
          <el-button link type="primary" @click="handleClearAll">
            {{ t('role.clearAllMenus') }}
          </el-button>
          <span class="menu-panel__count">
            {{ t('common.selectedCount', { count: checkedCount }) }}
          </span>
        </div>

        <el-empty
          v-if="!menuLoading && menuTree.length === 0"
          :description="t('role.menuTreeEmpty')"
        />

        <el-tree
          v-else
          ref="menuTreeRef"
          v-loading="menuLoading"
          class="menu-panel__tree"
          :data="menuTree"
          node-key="id"
          show-checkbox
          :default-expand-all="treeExpandAll"
          :props="{ label: 'name', children: 'children' }"
          :default-checked-keys="checkedKeys"
          @check="handleTreeCheck"
        />
      </div>
    </DialogForm>
  </PageContainer>
</template>

<style scoped lang="scss">
.filter-control {
  /* 不写死宽度：由选中项 / placeholder 的文字长度决定 */
  width: auto;
  min-width: 110px;
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

.cell-code {
  /* 单元格内的容器一律用行内级，避免与树形表格的缩进/展开图标抢行、或撑高行高 */
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  gap: 6px;
}

.cell-warning {
  margin-left: 4px;
  color: var(--admin-color-warning);
}

.menu-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;

  &__toolbar {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--admin-divider-color);
  }

  &__count {
    margin-left: auto;
    font-size: 12px;
    color: var(--admin-text-secondary);
  }

  &__tree {
    max-height: 60vh;
    overflow: auto;
  }
}
</style>
