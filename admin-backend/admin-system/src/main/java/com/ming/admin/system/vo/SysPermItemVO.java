package com.ming.admin.system.vo;

import lombok.Data;

/**
 * 权限标识清单条目出参（来自启动扫描结果，供菜单表单的权限标识下拉使用）。
 */
@Data
public class SysPermItemVO {

    /** 权限标识，即小写的接口地址 */
    private String perm;

    /** 所属模块，类路径末端，例如 sysrole */
    private String module;

    /** 动作名，方法名转小写，例如 assignmenus */
    private String action;
}
