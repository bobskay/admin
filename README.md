# 通用后台管理系统（admin）

前后端分离的通用后台管理系统，功能域只有四个：**登录、用户管理、角色管理、菜单管理**。

- 后端：Spring Boot 3.3.5 + JDK 17 + MyBatis-Plus 3.5.7 + Redisson + MySQL 8 + Redis，Maven 多模块。
- 前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Tailwind CSS + vue-i18n。

---

## 一、目录结构

```
admin
├── README.md                  本文档
├── doc/sql/admin.sql          建库 + 建表 + 索引 + 种子数据（一个文件，可直接执行）
├── admin-backend              后端（Maven 多模块）
│   ├── pom.xml                聚合器
│   ├── admin-dependence       版本集中管理（唯一出现版本号的地方）
│   ├── admin-common           常量、工具门面（字符串/集合/对象/JSON/日期/ID/树/Argon2）
│   ├── admin-doc              knife4j / OpenAPI 接口文档配置
│   ├── admin-log              logback、请求日志切面、操作审计日志
│   ├── admin-permission       统一返回、异常、分页、traceId、认证、鉴权、缓存、全局配置
│   ├── admin-system           业务：认证、用户、角色、菜单
│   └── admin-boot             启动类、配置文件、启动扫描校验组件
└── admin-front                前端（Vue3 + Vite）
```

后端依赖方向严格单向：`admin-common ← admin-log ← admin-permission ← admin-system ← admin-boot`，`admin-doc` 被 `admin-permission` 依赖。
`admin-common` 是纯工具模块，不依赖任何业务模块；`admin-log` 不依赖 `admin-permission`（否则循环依赖）。

---

## 二、环境要求

| 组件 | 版本要求 | 本机示例 |
|---|---|---|
| JDK | **17**（编译 release 固定 17，JDK 8 会直接失败） | `C:\java\jdk\jdk17` |
| Maven | 3.8+ | `C:\java\maven\apache-maven-3.8.6` |
| Node.js | 18+（建议 20+） | v24 |
| pnpm | 8+ | 12.x |
| MySQL | 8.x（库名 `admin`，utf8mb4 / utf8mb4_general_ci / InnoDB） | 127.0.0.1:3306 |
| Redis | 6+ | 127.0.0.1:6379 |

### JDK 切换（重要）

本机系统 `JAVA_HOME` 默认指向 JDK 8，**构建前必须切到 17**，否则 Maven 会以 1.8 编译并报错。

PowerShell（当前会话临时切换）：

```powershell
$env:JAVA_HOME = 'C:\java\jdk\jdk17'
& 'C:\java\maven\apache-maven-3.8.6\bin\mvn.cmd' -f admin-backend\pom.xml clean install -DskipTests
```

cmd：

```bat
set JAVA_HOME=C:\java\jdk\jdk17
mvn -f admin-backend\pom.xml clean install -DskipTests
```

---

## 三、启动顺序

1. 启动 **MySQL** 与 **Redis**。
2. 执行数据库脚本：
   ```bash
   mysql -h127.0.0.1 -P3306 -uroot -p < doc/sql/admin.sql
   ```
   脚本可重复执行（每次重建表与种子数据）。
3. 启动后端（端口 8080）：
   ```bash
   # 方式一：IDEA 运行 com.ming.admin.AdminApplication
   # 方式二：命令行
   java -jar admin-backend/admin-boot/target/admin-boot-1.0.0.jar
   ```
   默认激活 `dev`；生产用 `--spring.profiles.active=prod`。
4. 启动前端（端口 5173）：
   ```bash
   cd admin-front
   pnpm install
   pnpm dev
   ```
5. 浏览器访问 <http://localhost:5173>，用下面的默认账号登录。

### 默认账号

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `Admin@123` | 超级管理员，拥有全部权限（`superAdmin = true`，权限数组展示为 `*:*:*`） |
| `common` | 无内置账号 | 种子数据里只有 `admin` 用户；`common` 是**角色**（普通角色），可自行新建用户并分配该角色来验证按钮权限效果 |

种子数据中超级管理员的密码字段是预先用 Spring Security 的 Argon2id 编码器（salt 16、hash 32、并行度 1、内存 19456 KiB、迭代 2）生成的哈希，**不要改成明文或自行重新生成**。

---

## 四、后端模块说明

| 模块 | 职责 |
|---|---|
| admin-dependence | 所有第三方依赖版本号的唯一来源；继承 `spring-boot-starter-parent`；集中声明注解处理器顺序（Lombok → lombok-mapstruct-binding → MapStruct） |
| admin-common | 全局常量（请求头名、请求属性名、Redis 键前缀、状态值、菜单类型值、超管角色编码、MASK、白名单、校验原因词表）与工具门面（`StrUtils`/`CollUtils`/`ObjUtils`/`JsonUtils`/`DateUtils`/`IdUtils`/`TreeUtils`/`PasswordUtils`/`ServletUtils`） |
| admin-doc | knife4j / springdoc 配置，按 auth / user / role / menu 四个分组，文档地址 `/doc.html` |
| admin-log | logback 配置、请求日志切面（不写库）、操作审计日志（注解 + 切面 + 实体 + Mapper + Service + 独立有界线程池） |
| admin-permission | 统一返回体 `R`、状态码 `ResultCode`、校验错误对象、全局异常处理、分页对象、traceId 过滤器、认证过滤器、`@Menu` 权限注解、权限拦截器、线程上下文、token/权限/缓存服务、Jackson 与 MyBatis-Plus 与 Redisson 与 Web 配置、启动扫描清单注册表 |
| admin-system | 认证、用户、角色、菜单的 controller / service / mapper / entity / dto / vo / convert |
| admin-boot | 启动类、`application.yml`（+ dev / prod）、启动扫描校验组件 |

### 关键机制

- **认证**：登录成功后生成 32 位无横线小写 tokenId，登录用户 JSON 存 Redis `admin:token:{tokenId}`（默认 2 小时过期），滑动续期只在剩余时间小于一半时重置。
- **鉴权**：接口上标 `@Menu`（不传值），权限标识 = 接口地址；拦截器比对用户权限集合；超级管理员（`superAdmin = true`）直接放行。
- **权限缓存**：`admin:perm:{userId}:{permVersion}`，全局版本号 `admin:perm:version`；任何影响权限的写操作只做版本号自增，旧键 30 分钟后自然过期（不使用 `KEYS` 扫描）。
- **逻辑删除**：字段 `deleted`（0 未删 / 1 已删）在 `MybatisPlusConfig` 里集中声明，所有删除走 MyBatis-Plus 的统一删除方法。
- **用户名唯一**：`username` 单列唯一索引，**逻辑删除后用户名永久不可复用**，不提供改名与回收站。
- **统一返回**：HTTP 永远 200，业务状态放 body 的 `code`；body 只有 `code / message / data / timestamp`；`message` 是调试用英文短语，**前端只依赖 `code` 与 `data`**，中文/英文文案全部由前端 vue-i18n 翻译。
- **traceId**：请求头与响应头 `x-trace-id`，同时写入日志 MDC 与请求属性。
- **日期与 Long**：日期出参为毫秒时间戳数字；所有 `Long` 出参为字符串（防前端精度丢失），入参同时接受字符串与数字。

---

## 五、接口地址即契约（**重要**）

- 本项目的**权限标识默认等于接口地址**。同一个地址字符串同时出现在三处：后端映射注解、数据库按钮行的 `perms`、前端按钮权限指令。
- 因此**接口地址一旦上线就当作对外契约，不要随意改名**。改类路径或方法路径会导致数据库里的 `perms` 静默失效（接口全部返回 200007 且不报错）。
- 启动时的扫描组件会导出「权限标识清单」与「校验规则清单」，并在开发环境与数据库 `perms` 比对，发现"数据库里有、代码里没有"的标识会打 WARN 日志，便于对账。
- 接口路径、`perms`、前端指令字符串三处一律**全小写**；自定义请求头（`x-trace-id`、`authorization`、`accept-language`）也一律小写书写。
- 后端接口地址**不带 `/api` 前缀**；`/api` 只由前端 axios 实例的 `baseURL` 提供，开发环境由 Vite 代理转发到 `http://localhost:8080` 并去掉 `/api`。

### 接口清单（全部 POST，参数一律从请求体取）

| 模块 | 接口 |
|---|---|
| 认证 | `/admin/system/sysauth/login`、`/logout`、`/me`、`/count` |
| 用户 | `/admin/system/sysuser/page`、`/detail`、`/create`、`/update`、`/delete`、`/resetpassword`、`/changestatus`、`/assignroles` |
| 角色 | `/admin/system/sysrole/page`、`/list`、`/detail`、`/create`、`/update`、`/delete`、`/menuids`、`/assignmenus` |
| 菜单 | `/admin/system/sysmenu/tree`、`/list`、`/perms`、`/rules`、`/detail`、`/create`、`/update`、`/delete` |

接口文档：<http://localhost:8080/doc.html>（生产环境通过 `admin.doc-enabled` 关闭）。

---

## 六、前端说明

- 请求统一走 `src/utils/request.ts` 导出的 `post`，业务代码里不出现 `/api` 前缀、不直接使用 axios 实例。
- 接口地址只在 `src/api/*.ts` 里定义一次并导出常量；页面与指令一律引用常量，禁止重复书写地址字符串。
- 路由组件按 `import.meta.glob('/src/views/**/*.vue')` 映射表定位：菜单 `component` 非空优先用它，为空则按 `path` 推导（`/system/user` → `src/views/system/user/index.vue`），都找不到指向 404。
- 三套主题：白（light，默认）、黑（dark）、蓝（blue），通过根元素 `data-theme` 属性与 `dark` 类名切换，持久化到本地存储，挂载前先设置以避免闪烁。
- 国际化：简体中文（默认）与英文，词条按模块拆分；错误文案按 `error.{code}` 翻译，参数校验失败按 `field.{field}` + `validation.{reason}` 拼装。
- 开发环境启动时会拉一次 `/admin/system/sysmenu/rules` 清单，与本地词条比对，缺失的键会在浏览器控制台 WARN 输出。
- 依赖安装：`pnpm install`。工程根的 `pnpm-workspace.yaml` 只用于承载 pnpm 的安装设置（本工程是单包工程，不是 monorepo）——pnpm 10 起把依赖的构建脚本默认视为不可信，pnpm 12 又不再读取 `package.json` 的 `pnpm` 字段，因此 `esbuild` / `@parcel/watcher` / `vue-demi` 的放行清单写在该文件里；缺少它时 `pnpm install` 会以 `ERR_PNPM_IGNORED_BUILDS` 非零退出（依赖其实已装好，可用 `pnpm approve-builds --all` 补放行）。

---

## 七、环境与部署注意

- **时区一致**：数据库连接串带 `serverTimezone=Asia/Shanghai`，JVM 与应用保持同一时区，避免 `datetime` 与毫秒时间戳互转出现偏差。
- **字符集**：库、表、连接三处都是 utf8mb4。
- **Redis 库隔离**：dev 用 database 0，prod 用 database 1，避免开发清缓存误删线上 token。
- **密码与密钥**：数据库密码、Redis 密码一律从环境变量读取并给默认值（`DB_USERNAME`、`DB_PASSWORD`、`REDIS_PASSWORD` 等），不把生产密码硬编码在仓库里。
- **端口占用**：8080 / 5173 被占用时先确认占用进程再处理，不要随意改端口导致前端代理失效。
- **CORS**：开发环境走 Vite 代理属于同源请求，应用不配置 CORS，也没有 `@CrossOrigin`；将来接入网关后由网关剥离 `/api` 并逐条列出允许的来源（禁止使用 `*`），前端代码不需要改动。

### 常见启动失败排查清单

| 现象 | 排查方向 |
|---|---|
| 编译报 1.8 不支持的语法 | `JAVA_HOME` 未切到 JDK 17 |
| 父 POM 解析失败 | 各模块 `<parent>` 的 `relativePath` 是否指向 `admin-dependence`（聚合器是 `./admin-dependence/pom.xml`，其余是 `../admin-dependence/pom.xml`） |
| MapStruct 转换器未生成实现类 | 注解处理器顺序必须是 Lombok → lombok-mapstruct-binding → MapStruct |
| 分页不生效 | MyBatis-Plus 版本必须是 3.5.7（3.5.9+ 把分页插件拆成独立依赖，极易漏配） |
| 启动报 Redis 连接失败 | Redis 未启动，或 `spring.data.redis` 配置与实例不一致 |
| 逻辑删除行为不一致 | 是否在 `application.yml` 里重复声明了逻辑删除配置（只允许在 `MybatisPlusConfig` 里声明） |
| 接口全部返回 200007 | 数据库 `perms` 与代码里的接口地址不一致，看启动日志的权限标识比对 WARN |
| 启动报 `NoSuchMethodError`（ControllerAdviceBean）或 `/doc.html` 打开 500 | knife4j 4.5.0 自带的 springdoc 版本与 Spring Boot 不匹配；本项目已在 `admin-dependence` 中把 springdoc 固定为 **2.3.0**（knife4j 4.5.0 就是针对它编译的），自行改动依赖版本时需保持该固定，否则打开文档会抛 `SpringDocConfigProperties.getGroupConfigs()` 的 `NoSuchMethodError` |
| 后端启动报 `Unable to connect to Redis server`，且日志里有 `ERR AUTH <password> called without any password configured` | `spring.data.redis.password` 被解析成空字符串（`${REDIS_PASSWORD:}` 的默认值）时，Redisson 会误以为需要认证；项目已在 `RedissonConfig` 里把空密码清成 null，若自行改动该配置类需保留这段兜底 |
| `pnpm install` 报 `ERR_PNPM_IGNORED_BUILDS` | pnpm 10+ 的构建脚本安全策略；执行 `pnpm approve-builds --all` 或保留 `admin-front/pnpm-workspace.yaml` 里的 `allowBuilds` 清单 |

---

## 八、实现说明与取舍

- **审计字段自动填充**：`create_time` / `update_time` / `create_by` / `update_by` 由 `AuditMetaObjectHandler` 统一填充，当前用户 id 取自线程上下文。MyBatis-Plus 的自动填充需要在实体字段上声明填充时机（`@TableField(fill = ...)`），因此这四个字段保留了该声明；**表名、主键、逻辑删除策略一律不写注解**，全部在 `MybatisPlusConfig` 里用代码集中声明。
- **超级管理员判据**：前端与后端**只认 `superAdmin` 布尔值**；`*:*:*` 只是权限数组的展示值，任何判断逻辑都不匹配它。
- **参数校验只返回第一个错误**：`data` 是单个对象（`field` + `reason`），按 DTO 字段声明顺序取稳定的第一条，不回显用户提交的原值。
- **排序安全**：每个列表声明排序字段白名单（前端字段名 → 数据库列名），排序方向只接受 `asc` / `desc`，其余一律回落默认排序，用户输入永不直接拼进 `order by`。

---

## 九、本次不做

数据权限（按部门/本人过滤）、字段级权限、定时任务、消息通知、文件上传与管理、操作日志查询页面、导出功能、图形验证码、登录失败锁定、单点登录互斥、后端国际化、多租户、数据字典、代码生成器。
