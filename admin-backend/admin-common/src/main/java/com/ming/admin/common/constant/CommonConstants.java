package com.ming.admin.common.constant;

/**
 * 全局通用常量。
 */
public final class CommonConstants {

    /** 内置超级管理员角色编码（判据是登录用户的 superAdmin 布尔值，不是这个字符串） */
    public static final String SUPER_ADMIN_ROLE_CODE = "admin";

    /** 超级管理员权限数组的展示值，仅用于展示与兼容，任何判断逻辑都不要匹配它 */
    public static final String SUPER_ADMIN_PERM = "*:*:*";

    /** 脱敏占位符 */
    public static final String MASK = "***";

    /** 树的根父节点 id */
    public static final Long ROOT_PARENT_ID = 0L;

    /** 系统内置数据 / 无登录上下文时写入的操作人 id */
    public static final Long SYSTEM_USER_ID = 0L;

    /** 分页默认页码 */
    public static final long DEFAULT_PAGE_NUM = 1L;

    /** 分页默认每页条数 */
    public static final long DEFAULT_PAGE_SIZE = 10L;

    /** 分页单页最大条数 */
    public static final long MAX_PAGE_SIZE = 100L;

    /** 审计日志入参截断长度（字符） */
    public static final int AUDIT_PARAM_MAX_LENGTH = 2000;

    /** 审计日志异常信息截断长度（字符） */
    public static final int AUDIT_ERROR_MAX_LENGTH = 2000;

    private CommonConstants() {
    }
}
