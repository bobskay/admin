package com.ming.admin.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 菜单实体，对应表 sys_menu。
 * <p>一张表承担三件事：侧边栏 / 路由展示（type 1、2）、接口权限点、按钮权限点（type 3）。</p>
 * <p>perms 唯一索引允许 NULL：目录与菜单的 perms 一律留空（写 NULL，不能写空字符串）。</p>
 */
@Data
public class SysMenu {

    /** 主键，数据库自增 */
    private Long id;

    /** 父节点，0 表示顶级 */
    private Long parentId;

    /** 菜单名（用户填什么显示什么，不做多语言字段） */
    private String name;

    /** 类型：1 目录 2 菜单 3 按钮 */
    private Integer type;

    /** 前端路由路径，目录与菜单必填，按钮为空 */
    private String path;

    /** 前端组件路径，允许为空：为空时前端按 path 约定推导组件 */
    private String component;

    /** 图标名 */
    private String icon;

    /** 权限标识：按钮必填（等于接口地址），目录与菜单留空 */
    private String perms;

    /** 排序 */
    private Integer sort;

    /** 是否显示：1 显示 0 隐藏（隐藏仅不出现在侧边栏，路由仍然注册） */
    private Integer visible;

    /** 是否缓存页面 */
    private Integer keepAlive;

    /** 目录重定向地址 */
    private String redirect;

    /** 状态：1 启用 0 禁用（禁用则不下发给前端） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    private Integer deleted;
}
