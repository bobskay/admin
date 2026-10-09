package com.ming.admin.common.constant;

/**
 * 状态值常量：用户 / 角色 / 菜单通用。
 * <p>所有状态字段一律用 Integer 接收并做取值校验。</p>
 */
public final class StatusConstants {

    /** 启用 */
    public static final Integer ENABLED = 1;

    /** 禁用 */
    public static final Integer DISABLED = 0;

    private StatusConstants() {
    }
}
