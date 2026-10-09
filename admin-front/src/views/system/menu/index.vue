<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { object } from 'yup'

import {
  MENU_API,
  createMenu,
  deleteMenu,
  detailMenu,
  permsMenu,
  treeMenu,
  updateMenu
} from '@/api/menu'
import DataTable from '@/components/DataTable/index.vue'
import DialogForm from '@/components/DialogForm/index.vue'
import IconRender from '@/components/IconRender/index.vue'
import PageContainer from '@/components/PageContainer/index.vue'
import { listViewKeys } from '@/router/componentLoader'
import { useYupValidation } from '@/composables/useYupValidation'
import { useUserStore } from '@/stores/user'
import type { PermissionItem, SysMenuRoute, TableColumn } from '@/types'
import { KEEP_ALIVE, MENU_TYPE, ROOT_PARENT_ID, STATUS, VIEWS_ROOT, VISIBLE } from '@/utils/constants'
import {
  keepAliveRule,
  menuTypeRule,
  rangeNumberRule,
  requiredString,
  statusRule,
  visibleRule
} from '@/utils/validate'

/**
 * 菜单管理（任务书 21.5）。
 * - 树数据来自 MENU_API.tree（全量、含按钮、含禁用），**不要**用 MENU_API.list（只含启用项）；
 * - 权限标识下拉的选项来自 MENU_API.perms，清单为空或请求失败时降级为普通输入框；
 * - 父节点类型约束：目录挂顶级或目录、菜单挂目录、按钮挂菜单。
 */
defineOptions({ name: 'SystemMenuPage' })

const { t } = useI18n()
const userStore = useUserStore()

/** 父节点类型约束表：子类型 → 允许的父类型（'' 代表顶级） */
const ALLOWED_PARENT_TYPES: Record<number, string[]> = {
  [MENU_TYPE.DIRECTORY]: ['', String(MENU_TYPE.DIRECTORY)],
  [MENU_TYPE.MENU]: [String(MENU_TYPE.DIRECTORY)],
  [MENU_TYPE.BUTTON]: [String(MENU_TYPE.MENU)]
}

/* ---------------------------------------------------------------- 树表格 */

const loading = ref(false)
const menuTree = ref<SysMenuRoute[]>([])
/** 树形表格展开行 */
const expandKeys = ref<Array<string | number>>([])
/** 权限标识清单：接口失败或为空时降级为输入框 */
const permsList = ref<PermissionItem[]>([])

/**
 * 表格列定义（树形表格：children 字段 + 行缩进）。
 * 树形行的操作列还会额外吃 indent（每层 16px）的缩进，所以列宽必须把这个算进去，
 * 否则按钮会被迫换行、整行变成两行高。
 */
const columns: TableColumn<SysMenuRoute>[] = [
  { prop: 'name', labelKey: 'field.name', minWidth: 180, slot: 'menuName' },
  { prop: 'type', labelKey: 'field.type', width: 68, align: 'center', slot: 'menuType' },
  { prop: 'path', labelKey: 'field.path', minWidth: 150, slot: 'menuPath' },
  { prop: 'perms', labelKey: 'field.perms', minWidth: 170, slot: 'menuPerms' },
  { prop: 'sort', labelKey: 'field.sort', width: 60, align: 'center' },
  { prop: 'visible', labelKey: 'field.visible', width: 72, align: 'center', slot: 'menuVisible' },
  { prop: 'status', labelKey: 'field.status', width: 72, align: 'center', slot: 'menuStatus' },
  {
    prop: 'action',
    labelKey: 'common.action',
    minWidth: 190,
    align: 'right',
    fixed: 'right',
    showOverflowTooltip: false,
    cellClassName: 'cell-actions',
    slot: 'menuAction'
  }
]

/** 菜单类型标签文案 */
function typeLabel(type: number): string {
  if (type === MENU_TYPE.DIRECTORY) {
    return t('menu.typeDirectory')
  }
  if (type === MENU_TYPE.MENU) {
    return t('menu.typeMenu')
  }
  return t('menu.typeButton')
}

/** 菜单类型标签颜色 */
function typeTagType(type: number): 'primary' | 'success' | 'info' {
  if (type === MENU_TYPE.DIRECTORY) {
    return 'primary'
  }
  return type === MENU_TYPE.MENU ? 'success' : 'info'
}

/** 是否有子节点 */
function hasChildren(row: SysMenuRoute): boolean {
  return Array.isArray(row.children) && row.children.length > 0
}

/** 扁平化取全部节点 id */
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

/** 只取有子节点的 id（展开全部用） */
function collectExpandableIds(nodes: SysMenuRoute[]): string[] {
  const result: string[] = []
  const walk = (list: SysMenuRoute[]): void => {
    list.forEach((node) => {
      if (node.children && node.children.length > 0) {
        result.push(String(node.id))
        walk(node.children)
      }
    })
  }
  walk(nodes)
  return result
}

/** 全部可展开节点 id */
const expandableIds = computed(() => collectExpandableIds(menuTree.value))
/** 树的全部节点 id */
const allMenuIds = computed(() => flattenIds(menuTree.value))

const expandedAll = ref(true)

/** 加载菜单树（查询即刷新树） */
async function loadMenuTree(): Promise<void> {
  loading.value = true
  try {
    const tree = await treeMenu()
    menuTree.value = Array.isArray(tree) ? tree : []
    // 首次加载默认展开全部层级
    expandKeys.value = expandedAll.value ? [...expandableIds.value] : []
  } catch {
    menuTree.value = []
    expandKeys.value = []
  } finally {
    loading.value = false
  }
}

/** 展开 / 折叠全部 */
function toggleExpandAll(): void {
  const next = !expandedAll.value
  expandedAll.value = next
  expandKeys.value = next ? [...expandableIds.value] : []
}

/** 加载权限标识清单：失败或为空时保留空列表（表单自动降级为输入框） */
async function loadPermsList(): Promise<void> {
  try {
    const list = await permsMenu()
    permsList.value = Array.isArray(list) ? list : []
  } catch {
    permsList.value = []
  }
}

/** 权限标识清单是否可用于下拉 */
const permsSelectable = computed(() => permsList.value.length > 0)

/** 动作名对应的 i18n 文案（清单里有则用词条，没有则显示「未识别动作」） */
function actionLabel(action: string): string {
  const key = `menu.action${action.charAt(0).toUpperCase()}${action.slice(1)}`
  return t(key) === key ? t('menu.permsActionUnknown') : t(key)
}

/** 单个权限选项的展示文案（模块 + 动作 + 接口地址，避免同名动作分辨不清） */
function permLabel(item: PermissionItem): string {
  return `${item.perm} · ${t('menu.permSource', {
    module: item.module,
    action: actionLabel(item.action)
  })}`
}

/* ------------------------------------------------------------ 新增 / 编辑 */

interface MenuForm {
  id: string
  parentId: string
  type: number
  name: string
  path: string
  icon: string
  perms: string
  sort: number
  visible: number
  keepAlive: number
  redirect: string
  status: number
}

const formVisible = ref(false)
const formSubmitting = ref(false)
/** 当前编辑的菜单（新增时为 null） */
const editingMenu = ref<SysMenuRoute | null>(null)
/** 表单模式：新增根菜单 / 新增子级 / 编辑 */
const formMode = ref<'root' | 'child' | 'edit'>('root')
/** 是否已尝试提交（用于避免弹窗刚打开就显示必填红字） */
const formTouched = ref(false)

/** 当前菜单自身与全部子孙 id（上级菜单不能选它们） */
const selfAndDescendantIds = computed<string[]>(() => {
  const row = editingMenu.value
  if (!row) {
    return []
  }
  return [String(row.id), ...flattenIds(row.children ?? [])]
})

/** 上级菜单树：编辑时把自身与子孙排除，并在最前面补一个「顶级菜单」根节点 */
const parentTree = computed<SysMenuRoute[]>(() => {
  const excluded = new Set(selfAndDescendantIds.value)
  const strip = (nodes: SysMenuRoute[]): SysMenuRoute[] =>
    nodes
      .filter((node) => !excluded.has(String(node.id)))
      .map((node) => ({
        ...node,
        // 只有目录可以承载子节点；菜单与按钮不参与上级选择
        children: node.type === MENU_TYPE.DIRECTORY ? strip(node.children ?? []) : []
      }))
  return [
    {
      id: ROOT_PARENT_ID,
      parentId: ROOT_PARENT_ID,
      name: t('menu.topLevel'),
      type: MENU_TYPE.DIRECTORY,
      path: null,
      component: null,
      icon: null,
      sort: 0,
      visible: VISIBLE.SHOW,
      keepAlive: KEEP_ALIVE.NO,
      redirect: null,
      children: []
    },
    ...strip(menuTree.value)
  ]
})

/** 上级菜单节点的 id → type 映射（校验父节点类型用） */
const parentTypeMap = computed<Record<string, number>>(() => {
  const map: Record<string, number> = { [ROOT_PARENT_ID]: MENU_TYPE.DIRECTORY }
  const walk = (nodes: SysMenuRoute[]): void => {
    nodes.forEach((node) => {
      map[String(node.id)] = Number(node.type)
      if (node.children && node.children.length > 0) {
        walk(node.children)
      }
    })
  }
  walk(menuTree.value)
  return map
})

/** 上级菜单选择器的 props */
const parentTreeProps = {
  label: 'name',
  children: 'children',
  value: 'id'
}

/** 菜单表单规则：类型决定 path / perms 的必填性 */
const menuFormSchema = object({
  parentId: requiredString().max(50, 'validation.len'),
  type: menuTypeRule(),
  name: requiredString().max(50, 'validation.len'),
  // 目录与菜单必填 path（以斜杠开头），按钮不需要
  path: requiredString()
    .max(200, 'validation.len')
    .matches(/^\//, 'validation.pattern')
    .when('type', {
      is: (value: number) => value === MENU_TYPE.BUTTON,
      then: (schema) => schema.notRequired().defined(),
      otherwise: (schema) => schema
    }),
  // 按钮必填 perms（等于接口地址，以斜杠开头），目录与菜单不需要
  perms: requiredString()
    .max(200, 'validation.len')
    .matches(/^\//, 'validation.pattern')
    .when('type', {
      is: (value: number) => value !== MENU_TYPE.BUTTON,
      then: (schema) => schema.notRequired().defined(),
      otherwise: (schema) => schema
    }),
  icon: requiredString().notRequired().defined(),
  redirect: requiredString().notRequired().defined(),
  sort: rangeNumberRule(0, 9999),
  visible: visibleRule(),
  keepAlive: keepAliveRule(),
  status: statusRule()
})

const {
  formRef: menuFormEl,
  form,
  errorFor,
  submit: submitMenu,
  handleFieldChange,
  resetForm: resetMenuForm
} = useYupValidation<MenuForm>(menuFormSchema)

/** 弹窗标题 */
const formTitle = computed(() => {
  if (formMode.value === 'edit') {
    return t('menu.editTitle', { name: editingMenu.value?.name ?? '' })
  }
  return formMode.value === 'root' ? t('menu.createRootTitle') : t('menu.createChildTitle')
})

/** 目录与菜单显示 path 等字段；按钮不显示 */
const showPathFields = computed(() => form.type !== MENU_TYPE.BUTTON)
/** 只有目录显示重定向 */
const showRedirectField = computed(() => form.type === MENU_TYPE.DIRECTORY)
/** 按钮显示权限标识 */
const showPermsField = computed(() => form.type === MENU_TYPE.BUTTON)
/** 目录与菜单显示图标、显示状态、缓存 */
const showDisplayFields = computed(() => form.type !== MENU_TYPE.BUTTON)

/**
 * 「上级菜单」的就地错误文案：yup 只校验必填，两条自定义规则在这里判定。
 * - menu.parentInvalid：上级菜单不能是自己或自己的子孙
 * - menu.parentTypeInvalid：父节点类型不合法（目录挂顶级或目录、菜单挂目录、按钮挂菜单）
 */
const parentIdError = computed(() => {
  const required = errorFor('parentId')
  if (required) {
    return required
  }
  const parentId = form.parentId
  if (!parentId) {
    return t('menu.parentRequired')
  }
  if (selfAndDescendantIds.value.includes(parentId)) {
    return t('menu.parentInvalid')
  }
  const allowed = ALLOWED_PARENT_TYPES[form.type] ?? []
  const parentKey = parentId === ROOT_PARENT_ID ? '' : String(parentTypeMap.value[parentId] ?? '')
  return allowed.includes(parentKey) ? '' : t('menu.parentTypeInvalid')
})

/** 打开新增：root = 根菜单，child = 指定父节点的子级 */
function openCreate(mode: 'root' | 'child', parent?: SysMenuRoute): void {
  formMode.value = mode
  editingMenu.value = null
  formTouched.value = false
  const childOfMenu = mode === 'child' && parent ? String(parent.id) : ROOT_PARENT_ID
  resetMenuForm({
    id: '',
    parentId: childOfMenu,
    // 挂在菜单下默认给按钮，挂目录或顶级默认给目录
    type: childOfMenu === ROOT_PARENT_ID ? MENU_TYPE.MENU : MENU_TYPE.BUTTON,
    name: '',
    path: '',
    icon: '',
    perms: '',
    sort: 0,
    visible: VISIBLE.SHOW,
    keepAlive: KEEP_ALIVE.NO,
    redirect: '',
    status: STATUS.ENABLED
  })
  formVisible.value = true
}

/** 表单值 → MenuForm（null 统一归一化为空串/默认值） */
function toMenuForm(detail: SysMenuRoute): MenuForm {
  return {
    id: String(detail.id),
    parentId:
      detail.parentId === null || detail.parentId === undefined
        ? ROOT_PARENT_ID
        : String(detail.parentId),
    type: Number(detail.type),
    name: detail.name ?? '',
    path: detail.path ?? '',
    icon: detail.icon ?? '',
    perms: detail.perms ?? '',
    sort: Number(detail.sort ?? 0),
    visible: Number(detail.visible ?? VISIBLE.SHOW),
    keepAlive: Number(detail.keepAlive ?? KEEP_ALIVE.NO),
    redirect: detail.redirect ?? '',
    status: Number(detail.status ?? STATUS.ENABLED)
  }
}

/** 打开编辑：先回填列表行，再用详情接口覆盖（详情失败则保留行数据） */
async function openEdit(row: SysMenuRoute): Promise<void> {
  formMode.value = 'edit'
  editingMenu.value = row
  formTouched.value = false
  resetMenuForm(toMenuForm(row))
  formVisible.value = true
  try {
    const detail = await detailMenu(String(row.id))
    if (detail) {
      resetMenuForm(toMenuForm(detail))
    }
  } catch {
    // 详情失败时保留列表行数据，错误提示已由请求层处理
  }
}

/** 类型切换：清掉上一个类型专属字段的旧值和错误，避免提交残留脏数据 */
function handleTypeChange(value: number): void {
  handleFieldChange('type', Number(value))
  if (Number(value) === MENU_TYPE.BUTTON) {
    handleFieldChange('path', '')
    handleFieldChange('redirect', '')
  } else {
    handleFieldChange('perms', '')
  }
}

/** 选择权限标识：按动作名自动带出按钮名称（用户仍可手改） */
function handlePermsChange(value: string): void {
  handleFieldChange('perms', value)
  const matched = permsList.value.find((item) => item.perm === value)
  if (!matched) {
    return
  }
  // 按钮名按动作名的中英文词条带出；已填过名称时不覆盖用户输入
  if (!form.name.trim()) {
    handleFieldChange('name', actionLabel(matched.action))
    ElMessage.info(t('menu.autoNameByAction'))
  }
}

/** 提交新增 / 编辑 */
async function handleMenuSubmit(): Promise<void> {
  if (formSubmitting.value) {
    return
  }
  formTouched.value = true
  formSubmitting.value = true
  try {
    const executed = await submitMenu(async (values) => {
      const isButton = Number(values.type) === MENU_TYPE.BUTTON
      const payload = {
        parentId: values.parentId || ROOT_PARENT_ID,
        name: values.name.trim(),
        type: Number(values.type),
        path: isButton ? null : values.path.trim() || null,
        // 不再让用户填组件路径：视图组件一律按路由 path 对应 src/views 目录结构自动定位
        component: null,
        icon: isButton ? null : values.icon.trim() || null,
        perms: isButton ? values.perms.trim() : null,
        sort: Number(values.sort),
        visible: Number(values.visible),
        keepAlive: Number(values.keepAlive),
        redirect: Number(values.type) === MENU_TYPE.DIRECTORY ? values.redirect.trim() || null : null,
        status: Number(values.status)
      }
      if (values.id) {
        await updateMenu({ ...payload, id: values.id })
      } else {
        await createMenu(payload)
      }
    })
    if (!executed) {
      return
    }
    ElMessage.success(editingMenu.value ? t('menu.updateSuccess') : t('menu.createSuccess'))
    formVisible.value = false
    await loadMenuTree()
  } finally {
    formSubmitting.value = false
  }
}

/* ---------------------------------------------------------------- 删除 */

/** 删除单条：有子节点时按后端约定提示不能删除（error.200018） */
async function handleDeleteRow(row: SysMenuRoute): Promise<void> {
  if (hasChildren(row)) {
    ElMessage.error(t('error.200018'))
    return
  }
  try {
    await deleteMenu([String(row.id)])
    ElMessage.success(t('menu.deleteSuccess'))
    await loadMenuTree()
  } catch {
    // 失败提示已由请求层统一处理
  }
}

/* ---------------------------------------------------------------- 其它 */

/** 表单里的图标预览 */
const iconPreview = computed(() => form.icon.trim())

/** 按钮类型必须填 perms：schema 在非按钮分支放开了必填，这里补一次就地提示 */
const permsError = computed(() => {
  const error = errorFor('perms')
  if (error) {
    return error
  }
  return formTouched.value && form.type === MENU_TYPE.BUTTON && !form.perms.trim()
    ? t('menu.permsRequired')
    : ''
})

/** 类型单选的可选项 */
const menuTypes = [MENU_TYPE.DIRECTORY, MENU_TYPE.MENU, MENU_TYPE.BUTTON]

/** 全部已注册的视图文件（import.meta.glob 结果），用于校验路由路径与 src/views 目录结构是否一致 */
const viewKeys = listViewKeys()

/**
 * 当前填写的路由路径是否**没有**对应的页面文件。
 * 组件定位规则是 path → views/{path}/index.vue（或 views/{path}.vue），
 * 路径与目录结构不一致时该菜单会落到 404 页面，所以这里就地提示（不阻断保存）。
 */
const pathViewMissing = computed<boolean>(() => {
  if (Number(form.type) === MENU_TYPE.BUTTON) {
    return false
  }
  const normalized = form.path.trim().replace(/^\/+/, '').replace(/\/+$/, '')
  if (!normalized) {
    return false
  }
  return (
    !viewKeys.includes(`${VIEWS_ROOT}${normalized}/index.vue`) &&
    !viewKeys.includes(`${VIEWS_ROOT}${normalized}.vue`)
  )
})

onMounted(async () => {
  await Promise.all([loadMenuTree(), loadPermsList()])
})
</script>

<template>
  <PageContainer>
    <DataTable
      v-model:expand-keys="expandKeys"
      :data="menuTree"
      :loading="loading"
      :columns="columns"
      :total="menuTree.length"
      :show-pagination="false"
      tree-props-children="children"
      :indent="16"
      row-key="id"
    >
      <template #toolbar>
        <el-button v-has="MENU_API.create" type="primary" @click="openCreate('root')">
          {{ t('menu.createRoot') }}
        </el-button>
        <el-button @click="toggleExpandAll">
          {{ expandedAll ? t('menu.collapseAll') : t('menu.expandAll') }}
        </el-button>
        <el-button v-has="MENU_API.tree" @click="loadMenuTree">
          {{ t('common.refresh') }}
        </el-button>
        <span class="toolbar-tip">{{ t('common.totalCount', { total: allMenuIds.length }) }}</span>
      </template>

      <template #menuName="{ row }">
        <div class="cell-name">
          <IconRender :name="row.icon" :size="15" />
          <span>{{ row.name }}</span>
        </div>
      </template>

      <template #menuType="{ row }">
        <el-tag size="small" :type="typeTagType(Number(row.type))" effect="plain">
          {{ typeLabel(Number(row.type)) }}
        </el-tag>
      </template>

      <template #menuPath="{ row }">
        <span v-if="row.path">{{ row.path }}</span>
        <span v-else class="text-secondary">-</span>
      </template>

      <template #menuPerms="{ row }">
        <span v-if="row.perms" class="cell-perms">{{ row.perms }}</span>
        <span v-else class="text-secondary">-</span>
      </template>

      <template #menuVisible="{ row }">
        <el-tag
          size="small"
          :type="Number(row.visible) === VISIBLE.SHOW ? 'success' : 'info'"
          effect="plain"
        >
          {{ Number(row.visible) === VISIBLE.SHOW ? t('menu.visibleShow') : t('menu.visibleHide') }}
        </el-tag>
      </template>

      <template #menuStatus="{ row }">
        <el-tag
          size="small"
          :type="Number(row.status) === STATUS.ENABLED ? 'success' : 'danger'"
          effect="plain"
        >
          {{ Number(row.status) === STATUS.ENABLED ? t('common.enabled') : t('common.disabled') }}
        </el-tag>
      </template>

      <template #menuAction="{ row }">
        <el-button
          v-has="MENU_API.create"
          link
          type="primary"
          :disabled="Number(row.type) === MENU_TYPE.BUTTON"
          @click="openCreate('child', row)"
        >
          {{ t('menu.createChild') }}
        </el-button>
        <el-button v-has="MENU_API.update" link type="primary" @click="openEdit(row)">
          {{ t('common.edit') }}
        </el-button>
        <el-button
          v-if="userStore.hasPermission(MENU_API.remove)"
          link
          type="danger"
          :disabled="hasChildren(row)"
          :title="hasChildren(row) ? t('menu.hasChildrenTip') : ''"
          @click="handleDeleteRow(row)"
        >
          {{ t('common.delete') }}
        </el-button>
      </template>
    </DataTable>

    <!-- 新增 / 编辑菜单 -->
    <DialogForm
      v-model="formVisible"
      :title="formTitle"
      width="620px"
      :loading="formSubmitting"
      :form-instance="menuFormEl"
      @submit="handleMenuSubmit"
    >
      <el-form ref="menuFormEl" label-width="104px" @submit.prevent>
        <el-form-item :label="t('field.type')" prop="type" :error="errorFor('type')">
          <el-radio-group
            :model-value="form.type"
            @update:model-value="handleTypeChange(Number($event))"
          >
            <el-radio v-for="type in menuTypes" :key="type" :value="type">
              {{ typeLabel(type) }}
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('field.parentId')" prop="parentId" :error="parentIdError">
          <el-tree-select
            :model-value="form.parentId"
            class="form-control"
            check-strictly
            node-key="id"
            :data="parentTree"
            :props="parentTreeProps"
            :placeholder="t('menu.selectParent')"
            @update:model-value="handleFieldChange('parentId', $event)"
          />
        </el-form-item>

        <el-form-item :label="t('field.name')" prop="name" :error="errorFor('name')">
          <el-input
            :model-value="form.name"
            :placeholder="t('menu.namePlaceholder')"
            @update:model-value="handleFieldChange('name', $event)"
          />
        </el-form-item>

        <el-form-item
          v-if="showPathFields"
          :label="t('field.path')"
          prop="path"
          :error="errorFor('path')"
        >
          <el-input
            :model-value="form.path"
            :placeholder="t('menu.pathPlaceholder')"
            @update:model-value="handleFieldChange('path', $event)"
          />
          <p v-if="pathViewMissing" class="form-tip form-tip--warning">
            {{ t('menu.pathNoView', { path: form.path.trim().replace(/^\/+/, '') }) }}
          </p>
        </el-form-item>

        <el-form-item
          v-if="showDisplayFields"
          :label="t('field.icon')"
          prop="icon"
          :error="errorFor('icon')"
        >
          <div class="icon-field">
            <el-input
              :model-value="form.icon"
              :placeholder="t('menu.iconPlaceholder')"
              @update:model-value="handleFieldChange('icon', $event)"
            />
            <span class="icon-field__preview">
              <span class="icon-field__label">{{ t('menu.iconPreview') }}</span>
              <IconRender :name="iconPreview" :size="18" />
            </span>
          </div>
        </el-form-item>

        <el-form-item v-if="showPermsField" :label="t('field.perms')" prop="perms" :error="permsError">
          <!-- 清单可用：可下拉可手输；清单为空或接口失败：降级为普通输入框 -->
          <el-select
            v-if="permsSelectable"
            :model-value="form.perms"
            class="form-control"
            filterable
            allow-create
            default-first-option
            :placeholder="t('menu.permsPlaceholder')"
            @update:model-value="handlePermsChange"
          >
            <el-option
              v-for="item in permsList"
              :key="item.perm"
              :value="item.perm"
              :label="permLabel(item)"
            />
          </el-select>
          <el-input
            v-else
            :model-value="form.perms"
            :placeholder="t('menu.permsPlaceholder')"
            @update:model-value="handleFieldChange('perms', $event)"
          />
          <p class="form-tip">
            {{ permsSelectable ? t('menu.permsTips') : t('menu.permsEmpty') }}
          </p>
        </el-form-item>

        <el-form-item :label="t('field.sort')" prop="sort" :error="errorFor('sort')">
          <el-input-number
            :model-value="form.sort"
            :min="0"
            :max="9999"
            controls-position="right"
            @update:model-value="handleFieldChange('sort', Number($event))"
          />
          <span class="form-inline-tip">{{ t('menu.sortTips') }}</span>
        </el-form-item>

        <el-form-item
          v-if="showDisplayFields"
          :label="t('field.visible')"
          prop="visible"
          :error="errorFor('visible')"
        >
          <el-radio-group
            :model-value="form.visible"
            @update:model-value="handleFieldChange('visible', Number($event))"
          >
            <el-radio :value="VISIBLE.SHOW">{{ t('menu.visibleShow') }}</el-radio>
            <el-radio :value="VISIBLE.HIDE">{{ t('menu.visibleHide') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="showDisplayFields"
          :label="t('field.keepAlive')"
          prop="keepAlive"
          :error="errorFor('keepAlive')"
        >
          <el-radio-group
            :model-value="form.keepAlive"
            @update:model-value="handleFieldChange('keepAlive', Number($event))"
          >
            <el-radio :value="KEEP_ALIVE.YES">{{ t('common.yes') }}</el-radio>
            <el-radio :value="KEEP_ALIVE.NO">{{ t('common.no') }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="showRedirectField"
          :label="t('field.redirect')"
          prop="redirect"
          :error="errorFor('redirect')"
        >
          <el-input
            :model-value="form.redirect"
            :placeholder="t('menu.redirectPlaceholder')"
            @update:model-value="handleFieldChange('redirect', $event)"
          />
          <p class="form-tip">{{ t('menu.redirectTips') }}</p>
        </el-form-item>

        <el-form-item :label="t('field.status')" prop="status" :error="errorFor('status')">
          <el-radio-group
            :model-value="form.status"
            @update:model-value="handleFieldChange('status', Number($event))"
          >
            <el-radio :value="STATUS.ENABLED">{{ t('common.enabled') }}</el-radio>
            <el-radio :value="STATUS.DISABLED">{{ t('common.disabled') }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </DialogForm>
  </PageContainer>
</template>

<style scoped lang="scss">
.form-control {
  width: 100%;
}

.form-tip {
  margin-top: 2px;
  font-size: 12px;
  line-height: 18px;
  color: var(--admin-text-secondary);
}

.form-tip--warning {
  color: var(--admin-color-warning);
}

.form-inline-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--admin-text-secondary);
}

.toolbar-tip {
  font-size: 12px;
  color: var(--admin-text-secondary);
}

.cell-name {
  /*
   * 必须是行内级容器：树形表格会在第一列的 .cell 里额外插入「缩进 + 展开图标」两个行内元素，
   * 用块级的 flex 会让它们和菜单名各占一行，行高直接翻倍（内容看着只有一行，高度却是两行）。
   */
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
  gap: 6px;
}

.cell-perms {
  font-family: Menlo, Consolas, monospace;
  font-size: 12px;
}

.icon-field {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;

  &__preview {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
  }

  &__label {
    font-size: 12px;
    color: var(--admin-text-secondary);
  }
}
</style>
