# admin-front 前端工程

通用后台管理系统的前端（Vue 3 + Vite + TypeScript + Element Plus + Tailwind CSS）。

## 环境要求

- Node.js 18+（建议 20+）
- pnpm（本工程使用 pnpm）

## 常用命令

```bash
# 安装依赖
pnpm install

# 启动开发服务（默认端口 5173）
pnpm dev

# 类型检查 + 生产构建
pnpm build

# 只做生产构建（跳过类型检查）
pnpm build:only

# 预览构建产物
pnpm preview

# 只做类型检查
pnpm type-check
```

依赖版本在 `package.json` 中全部写**精确版本号**（不带 `^` 与 `~`），请勿改成范围版本。

## 端口与代理

- 开发服务端口：**5173**
- 前端请求前缀固定为 `/api`（由环境变量 `VITE_API_BASE_URL` 提供，见 `.env*`）。
- 开发环境由 Vite 代理把 `/api` 转发到后端 `http://localhost:8080`，并**去掉 `/api` 前缀**：
  - 浏览器请求 `/api/admin/system/sysauth/login`
  - 实际转发到 `http://localhost:8080/admin/system/sysauth/login`
- 因此前端业务代码里写的接口地址**不带 `/api`**，统一放在 `src/api` 的常量里。

生产部署时需由 Nginx 等网关做同样的前缀转发（去掉 `/api`）。

## 目录结构

```
src
├── api           接口地址常量与请求函数（地址只在这里定义一次）
├── components    公共组件（PageContainer / SearchForm / DataTable / DialogForm / IconRender / ThemeSwitch / LangSwitch / Breadcrumb）
├── composables   组合式函数（useTable / useDialogForm / usePermission / useTheme / useLocale / useFormValidation）
├── directives    自定义指令（v-has 按钮权限）
├── layouts       布局与布局子组件
├── locales       国际化词条（zh-CN / en-US，按模块拆文件后汇总）
├── router        路由、守卫、动态路由、组件定位（glob 映射表）
├── stores        Pinia store（user / permission / app）
├── styles        主题变量、Element Plus 覆写、全局样式
├── types         类型声明
├── utils         axios 封装、token、常量、日期、树工具、校验规则、本地存储
├── views         页面（登录、工作台、用户、角色、菜单、错误页）
├── App.vue
└── main.ts
```

## 关键约定

### 接口地址单一来源

所有接口地址只在 `src/api/*.ts` 里定义一次并导出常量。页面里的请求调用、`v-has` 按钮权限指令
都必须引用同一常量，**禁止在页面里重复书写地址字符串**（任务书 15.5）。

> 权限标识默认等于接口地址：同一个字符串要同时出现在后端注解、数据库按钮的 `perms`、前端按钮权限指令三处。
> **接口地址一旦上线即视为对外契约，不要随意改名**，改名会静默导致权限失效（接口返回 200007 且不报错）。

### 组件定位（菜单 → 组件）

`src/router/componentLoader.ts` 用 `import.meta.glob('/src/views/**/*.vue')` 建立全量映射表，
**不允许手写零散的组件 import**。定位优先级：

1. 菜单的 `component` 字段非空 → 用它查表（例如 `system/user/index` → `/src/views/system/user/index.vue`）；
2. `component` 为空 → 按路由 path 推导（`/system/user` → `/src/views/system/user/index.vue`）；
3. 都找不到 → 指向 404 页面，不抛异常、不白屏。

新增页面时只要让目录结构与 path 一致即可零配置；目录结构对不上时在菜单里填一次 `component`。

> 注意：**新增一个此前不存在的页面文件必须重新构建部署前端**（组件是编译期产物，glob 表在构建时固定）。

### 主题

三种主题：白（light，默认）、黑（dark）、蓝（blue）。根元素同时维护 `data-theme` 属性与 `dark` 类名
（仅黑主题添加），变量定义在 `src/styles/theme.scss`，Element Plus 变量映射在 `src/styles/element-override.scss`。
主题持久化在本地存储（`admin_theme`），入口在挂载前就设置根元素标记，因此刷新不闪烁。

### 国际化

- 语言：简体中文（默认）与英文，词条键名结构完全对齐，按模块拆文件：`common` / `login` / `layout` /
  `dashboard` / `user` / `role` / `menu` / `field` / `validation` / `error`。
- 字段标签键名为 `field.{字段名}`，**字段名必须与后端 DTO 字段名逐字一致**，因此表单项的 `prop` 也要用同样的字段名。
- 校验原因只允许六个值：`required` / `len` / `pattern` / `email` / `range` / `type`，键名为 `validation.{reason}`。
- 错误码词条以数字码为键（`error.200003` 等），覆盖第 6.2 章完整码表。
- 后端返回的 `message` 是调试短语，**前端不得用它做展示**，只允许打印到控制台。
- 开发环境启动时会拉一次 `/admin/system/sysmenu/rules`，与本地 `field.*` / `validation.*` 词条比对，
  缺哪条就在控制台打印 `missing i18n key: ...`；生产环境不请求、不输出。

### 参数校验错误（100004）的处理

后端返回 `code=100004` + `data.field` + `data.reason` 时，前端按以下顺序取文案（取到即停）：

1. `validation.{field}.{reason}`（整句特例）
2. `field.{field}` + `validation.{reason}`（常规路径，英文用空格连接）
3. `error.100004`（兜底）
4. `error.unknown`

请求层**不会**为 100004 弹全局提示，由表单按 `field` 定位到具体控件并就地展示红字；
定位不到控件时退化为一条页面级提示。

### 请求层（`src/utils/request.ts`）

- 单个 axios 实例，`baseURL` 取 `VITE_API_BASE_URL`，超时 15 秒；对外只导出 `post` / `get`。
- 请求头：`authorization: Bearer {token}`、每次生成 `x-trace-id`、按当前语言设置 `accept-language`。
- 响应：`code === 200` 时直接把 `data` 返回给调用方；其余按 `error.{code}` 提示并 reject `{ code, data }`。
- `200003`：清 token 与用户状态、跳登录页，并发时只处理一次（模块级标志 + 短时间窗口）。
- 同接口 + 相同参数在途时用 `AbortController` 取消前一个请求；被取消的请求静默 reject（`canceled: true`）。
- 多标签页同步：监听 `storage` 事件，其他标签页清掉 token 时本页同步登出。

## 公共组件与组合式函数契约

| 用法 | 说明 |
|---|---|
| `<PageContainer title-key="menu.user" description-key="user.description">` | 页面容器 |
| `<SearchForm @search="..." @reset="..."><template #default="{ collapsed }">` | 查询表单 |
| `<DataTable :data :loading :columns :total v-model:page v-model:size row-key show-selection @sort-change @selection-change>` | 数据表格（插槽 `toolbar` 与按列 `slot` 命名的插槽，参数 `{ row, index, column }`） |
| `<DialogForm v-model :title :loading :form-instance @submit @cancel>` | 弹窗表单（关闭时调用 `formInstance.resetFields()`） |
| `<IconRender name="User" />` | 图标 |
| `useTable({ fetch, defaultQuery, remove })` | 返回 `{ loading, rows, total, query, selectedIds, loadData, handleSearch, handleReset, handleSortChange, handleSelectionChange, handleDelete, ... }` |
| `useDialogForm({ defaultForm, loadDetail, create, update })` | 返回 `{ visible, isEdit, submitting, form, openCreate, openEdit, close, submit }` |
| `useFormValidation({ schema })` | 返回 `{ formRef, errorFor, validateAll, handleSubmit, handleFieldChange, handleFieldBlur, applyRequestError, ... }` |
| `usePermission()` | 返回 `{ hasPermission, isSuperAdmin }` |
| `v-has="USER_API.create"` | 按钮权限指令：传值为空 → 保留；超管 → 通过；否则匹配权限集合；不通过则**从 DOM 移除元素** |

> `total` 在后端是字符串（Long 序列化规则），`DataTable` 与 `useTable` 内部会先 `Number()` 再交给分页组件。

## 开发期提示

- 不要为了显示统计数据去调用带权限点的列表接口（工作台统计走 `/admin/system/sysauth/count`）。
- 排序字段由后端白名单控制，前端只传字段名与 `asc` / `desc`。
- 所有 `id`、`total`、`count` 等 Long 字段前端一律按字符串处理。
