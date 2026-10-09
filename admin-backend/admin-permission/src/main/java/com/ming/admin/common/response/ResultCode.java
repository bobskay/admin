package com.ming.admin.common.response;

import java.util.Arrays;

/**
 * 状态码枚举。
 * <p>只有两个业务号段：100xxx 公共、200xxx 业务；200 是唯一的成功码。</p>
 * <p>message 是固定的英文调试短语，只给人看日志与接口调试，前端不得依赖它做展示。</p>
 */
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, null),

    /** 参数校验失败 */
    VALIDATION_FAILED(100004, "Validation failed"),

    /** 请求体格式不正确 */
    REQUEST_BODY_INVALID(100005, "Malformed request body"),

    /** 请求方式不允许 */
    METHOD_NOT_ALLOWED(100006, "Request method not supported"),

    /** 请求地址不存在 */
    NOT_FOUND(100007, "Request path not found"),

    /** 系统异常 */
    SYSTEM_ERROR(100008, "Internal server error"),

    /** 日期格式不正确 */
    DATE_FORMAT_INVALID(100009, "Invalid date format"),

    /** 数据已存在或状态冲突 */
    DATA_CONFLICT(100010, "Data already exists or status conflict"),

    /** 未登录或登录已过期 */
    UNAUTHORIZED(200003, "Not logged in or session expired"),

    /** 用户名已存在 */
    USERNAME_EXISTS(200004, "Username already exists"),

    /** 无访问权限 */
    ACCESS_DENIED(200007, "Access denied"),

    /** 用户不存在 */
    USER_NOT_FOUND(200008, "User not found"),

    /** 不能操作当前登录用户 */
    CANNOT_OPERATE_CURRENT_USER(200009, "Cannot operate on the current login user"),

    /** 不能操作超级管理员账号 */
    CANNOT_OPERATE_SUPER_ADMIN(200010, "Cannot operate on the super administrator account"),

    /** 原密码不正确 */
    OLD_PASSWORD_INCORRECT(200011, "Old password is incorrect"),

    /** 角色编码已存在 */
    ROLE_CODE_EXISTS(200012, "Role code already exists"),

    /** 角色不存在 */
    ROLE_NOT_FOUND(200013, "Role not found"),

    /** 该角色已被用户使用，不能删除 */
    ROLE_IN_USE(200014, "Role is assigned to users and cannot be deleted"),

    /** 用户名或密码错误 */
    BAD_CREDENTIALS(200015, "Invalid credentials"),

    /** 账号已被禁用 */
    ACCOUNT_DISABLED(200016, "Account is disabled"),

    /** 不能修改或删除内置超级管理员角色 */
    BUILTIN_SUPER_ADMIN_ROLE_PROTECTED(200017, "Built-in super administrator role cannot be modified, disabled or deleted"),

    /** 该菜单下存在子菜单，不能删除 */
    MENU_HAS_CHILDREN(200018, "Menu has child nodes and cannot be deleted"),

    /** 上级菜单不能是自己或自己的子菜单 */
    MENU_PARENT_INVALID(200019, "Parent menu cannot be itself or its own descendant"),

    /** 菜单权限标识已存在 */
    MENU_PERMS_EXISTS(200020, "Menu permission identifier already exists"),

    /** 菜单不存在 */
    MENU_NOT_FOUND(200021, "Menu not found");

    private final Integer code;

    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /** 按数字码查找，找不到返回 null */
    public static ResultCode of(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
