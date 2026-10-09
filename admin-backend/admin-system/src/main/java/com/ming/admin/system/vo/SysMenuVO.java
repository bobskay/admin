package com.ming.admin.system.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 菜单树节点出参。
 * <p>同一个节点结构同时服务两个场景：菜单管理页的全量树（含按钮）与角色分配权限弹窗的启用菜单树。
 * children 由 TreeUtils 在内存里挂载（禁止递归 SQL），无子节点时为空列表。</p>
 */
@Data
public class SysMenuVO {

    /** 主键 */
    private Long id;

    /** 父节点 id，0 表示顶级 */
    private Long parentId;

    /** 菜单名 */
    private String name;

    /** 类型：1 目录 2 菜单 3 按钮 */
    private Integer type;

    /** 前端路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 图标名 */
    private String icon;

    /** 权限标识：按钮必填，目录与菜单为空 */
    private String perms;

    /** 排序 */
    private Integer sort;

    /** 是否显示：1 显示 0 隐藏 */
    private Integer visible;

    /** 是否缓存页面：1 是 0 否 */
    private Integer keepAlive;

    /** 目录重定向地址 */
    private String redirect;

    /** 状态：1 启用 0 禁用 */
    private Integer status;

    /** 创建时间 */
    private Date createTime;

    /** 子节点，无子节点时为空列表 */
    private List<SysMenuVO> children;
}
