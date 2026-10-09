package com.ming.admin.security.scanner;

import lombok.Data;

/**
 * 权限标识清单条目：权限标识（小写的接口地址）、所属模块（类路径末端）、动作名（方法名转小写）。
 */
@Data
public class PermissionItem {

    /** 权限标识，即小写的接口地址 */
    private String perm;

    /** 所属模块，类路径末端，例如 sysuser */
    private String module;

    /** 动作名，方法名转小写，例如 resetpassword */
    private String action;

    public static PermissionItem of(String perm, String module, String action) {
        PermissionItem item = new PermissionItem();
        item.setPerm(perm);
        item.setModule(module);
        item.setAction(action);
        return item;
    }
}
