-- =============================================================================
-- 通用后台管理系统（admin）数据库脚本
-- 库名：admin        字符集：utf8mb4        排序规则：utf8mb4_general_ci        引擎：InnoDB
-- 说明：一个文件包含建库 + 建表 + 索引 + 种子数据，可直接执行，重复执行不报错。
--       权限标识（perms）必须与后端接口地址、前端指令字符串三处逐字一致。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `admin`;

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `sys_operation_log`;
DROP TABLE IF EXISTS `sys_role_menu`;
DROP TABLE IF EXISTS `sys_user_role`;
DROP TABLE IF EXISTS `sys_menu`;
DROP TABLE IF EXISTS `sys_role`;
DROP TABLE IF EXISTS `sys_user`;

-- -----------------------------------------------------------------------------
-- 用户表
-- username 为单列唯一索引，逻辑删除后用户名永久不可复用
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_user` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    varchar(50)  NOT NULL COMMENT '登录名',
  `password`    varchar(255) NOT NULL COMMENT '密码（Argon2id 哈希）',
  `nickname`    varchar(50)  NOT NULL COMMENT '显示名',
  `email`       varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone`       varchar(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`      varchar(255) DEFAULT NULL COMMENT '头像地址',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 0 禁用',
  `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by`   bigint       DEFAULT NULL COMMENT '创建人',
  `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`   bigint       DEFAULT NULL COMMENT '更新人',
  `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表';

-- -----------------------------------------------------------------------------
-- 角色表
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_role` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`        varchar(50)  NOT NULL COMMENT '角色名',
  `code`        varchar(50)  NOT NULL COMMENT '角色编码',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 0 禁用',
  `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
  `create_by`   bigint       DEFAULT NULL COMMENT '创建人',
  `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`   bigint       DEFAULT NULL COMMENT '更新人',
  `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_code` (`code`),
  KEY `idx_sys_role_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色表';

-- -----------------------------------------------------------------------------
-- 菜单表（一张表承担三件事：侧边栏展示、接口权限点、按钮权限点）
-- perms 唯一索引允许 NULL：MySQL 唯一索引对 NULL 不生效，因此多条 perms 为空的记录不受影响
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_menu` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`   bigint       NOT NULL DEFAULT 0 COMMENT '父节点：0 表示顶级',
  `name`        varchar(50)  NOT NULL COMMENT '菜单名',
  `type`        tinyint      NOT NULL COMMENT '类型：1 目录 2 菜单 3 按钮',
  `path`        varchar(200) DEFAULT NULL COMMENT '前端路由路径',
  `component`   varchar(200) DEFAULT NULL COMMENT '前端组件路径',
  `icon`        varchar(50)  DEFAULT NULL COMMENT '图标名',
  `perms`       varchar(200) DEFAULT NULL COMMENT '权限标识',
  `sort`        int          NOT NULL DEFAULT 0 COMMENT '排序',
  `visible`     tinyint      NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示 0 隐藏',
  `keep_alive`  tinyint      NOT NULL DEFAULT 0 COMMENT '是否缓存页面',
  `redirect`    varchar(200) DEFAULT NULL COMMENT '重定向地址',
  `status`      tinyint      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 0 禁用',
  `create_by`   bigint       DEFAULT NULL COMMENT '创建人',
  `create_time` datetime     DEFAULT NULL COMMENT '创建时间',
  `update_by`   bigint       DEFAULT NULL COMMENT '更新人',
  `update_time` datetime     DEFAULT NULL COMMENT '更新时间',
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_sys_menu_parent_id` (`parent_id`),
  UNIQUE KEY `uk_sys_menu_perms` (`perms`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '菜单表';

-- -----------------------------------------------------------------------------
-- 用户角色关联表（不做逻辑删除，变更时物理删除旧关联再批量插入）
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_user_role` (
  `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户 id',
  `role_id` bigint NOT NULL COMMENT '角色 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
  KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户角色关联表';

-- -----------------------------------------------------------------------------
-- 角色菜单关联表（不做逻辑删除）
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_role_menu` (
  `id`      bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id` bigint NOT NULL COMMENT '角色 id',
  `menu_id` bigint NOT NULL COMMENT '菜单 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
  KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '角色菜单关联表';

-- -----------------------------------------------------------------------------
-- 操作审计表
-- -----------------------------------------------------------------------------
CREATE TABLE `sys_operation_log` (
  `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     bigint       DEFAULT NULL COMMENT '操作人 id（未登录场景为空）',
  `username`    varchar(50)  DEFAULT NULL COMMENT '操作人用户名',
  `module`      varchar(50)  DEFAULT NULL COMMENT '模块名',
  `action`      varchar(50)  DEFAULT NULL COMMENT '动作名',
  `method`      varchar(10)  DEFAULT NULL COMMENT 'HTTP 方法',
  `uri`         varchar(255) DEFAULT NULL COMMENT '请求地址',
  `ip`          varchar(50)  DEFAULT NULL COMMENT '客户端 IP',
  `user_agent`  varchar(500) DEFAULT NULL COMMENT '浏览器标识',
  `params`      text COMMENT '请求参数（脱敏、截断到 2000 字符）',
  `result_code` int          DEFAULT NULL COMMENT '返回码',
  `error_msg`   text COMMENT '异常信息（截断后写入）',
  `cost_ms`     bigint       DEFAULT NULL COMMENT '耗时毫秒',
  `create_time` datetime     NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sys_operation_log_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作审计表';

-- =============================================================================
-- 种子数据
-- 说明：create_by / update_by 写 0，deleted 写 0，时间字段写固定值，保证重复执行结果可预期。
--       菜单的 perms 为空时写 NULL（不能写空字符串，否则触发唯一索引冲突）。
-- =============================================================================

-- 超级管理员账号：admin / Admin@123
-- 该哈希由 Spring Security 的 Argon2 编码器按（salt 16、hash 32、并行度 1、内存 19456、迭代 2）实测通过，禁止改写。
INSERT INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES (1, 'admin', '$argon2id$v=19$m=19456,t=2,p=1$qrXtYeezb+hQfqyIw46oIA$xCN+2eVI/EM/a3j2qVK0ofCH+XbJKyW7Qdg26ffupgs', '超级管理员', NULL, NULL, NULL, 1, '内置超级管理员账号', 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0);

-- 角色
INSERT INTO `sys_role` (`id`, `name`, `code`, `sort`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES (1, '超级管理员', 'admin', 1, 1, '内置超级管理员角色，不可删除、不可修改编码、不可禁用', 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
       (2, '普通角色', 'common', 2, 1, '演示用普通角色', 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0);

-- 用户角色关联
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`)
VALUES (1, 1, 1);

-- 菜单：目录与菜单（perms 留 NULL，只负责侧边栏展示与页面入口）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `perms`, `sort`, `visible`, `keep_alive`, `redirect`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES (1, 0, '系统管理', 1, '/system', NULL, 'Settings', NULL, 1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
       (2, 1, '用户管理', 2, '/system/user', NULL, 'User', NULL, 1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
       (3, 1, '角色管理', 2, '/system/role', NULL, 'Avatar', NULL, 2, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
       (4, 1, '菜单管理', 2, '/system/menu', NULL, 'Menu', NULL, 3, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
       (21, 1, '操作日志', 2, '/system/log', NULL, 'Document', NULL, 4, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0);

-- 菜单：按钮（perms 必填，值等于后端接口地址）
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `path`, `component`, `icon`, `perms`, `sort`, `visible`, `keep_alive`, `redirect`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`)
VALUES
  -- 用户管理
  (5,  2, '查询',     3, NULL, NULL, NULL, '/admin/system/sysuser/page',           1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (6,  2, '新增',     3, NULL, NULL, NULL, '/admin/system/sysuser/create',         2, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (7,  2, '编辑',     3, NULL, NULL, NULL, '/admin/system/sysuser/update',         3, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (8,  2, '删除',     3, NULL, NULL, NULL, '/admin/system/sysuser/delete',         4, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (9,  2, '重置密码', 3, NULL, NULL, NULL, '/admin/system/sysuser/resetpassword',  5, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (10, 2, '启用/禁用', 3, NULL, NULL, NULL, '/admin/system/sysuser/changestatus',  6, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (11, 2, '分配角色', 3, NULL, NULL, NULL, '/admin/system/sysuser/assignroles',    7, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  -- 角色管理
  (12, 3, '查询',     3, NULL, NULL, NULL, '/admin/system/sysrole/page',           1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (13, 3, '新增',     3, NULL, NULL, NULL, '/admin/system/sysrole/create',         2, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (14, 3, '编辑',     3, NULL, NULL, NULL, '/admin/system/sysrole/update',         3, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (15, 3, '删除',     3, NULL, NULL, NULL, '/admin/system/sysrole/delete',         4, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (16, 3, '分配权限', 3, NULL, NULL, NULL, '/admin/system/sysrole/assignmenus',    5, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  -- 菜单管理
  (17, 4, '查询',     3, NULL, NULL, NULL, '/admin/system/sysmenu/tree',           1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (18, 4, '新增',     3, NULL, NULL, NULL, '/admin/system/sysmenu/create',         2, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (19, 4, '编辑',     3, NULL, NULL, NULL, '/admin/system/sysmenu/update',         3, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  (20, 4, '删除',     3, NULL, NULL, NULL, '/admin/system/sysmenu/delete',         4, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0),
  -- 操作日志
  (22, 21, '查询',    3, NULL, NULL, NULL, '/admin/system/syslog/page',            1, 1, 0, NULL, 1, 0, '2025-01-01 00:00:00', 0, '2025-01-01 00:00:00', 0);

-- 角色菜单关联：admin 角色关联全部菜单与按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
       (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
       (1, 21), (1, 22);

-- 角色菜单关联：common 角色只关联「系统管理」目录、「用户管理」菜单、以及用户管理下的「查询」按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
VALUES (2, 1), (2, 2), (2, 5);
