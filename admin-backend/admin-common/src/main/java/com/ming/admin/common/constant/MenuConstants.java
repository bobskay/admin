package com.ming.admin.common.constant;

/**
 * 菜单相关取值常量。
 */
public final class MenuConstants {

    /** 类型：目录 */
    public static final Integer TYPE_DIRECTORY = 1;

    /** 类型：菜单 */
    public static final Integer TYPE_MENU = 2;

    /** 类型：按钮 */
    public static final Integer TYPE_BUTTON = 3;

    /** 显示 */
    public static final Integer VISIBLE_SHOW = 1;

    /** 隐藏（仅不出现在侧边栏，路由仍然注册） */
    public static final Integer VISIBLE_HIDE = 0;

    /** 缓存页面 */
    public static final Integer KEEP_ALIVE_YES = 1;

    /** 不缓存页面 */
    public static final Integer KEEP_ALIVE_NO = 0;

    private MenuConstants() {
    }
}
