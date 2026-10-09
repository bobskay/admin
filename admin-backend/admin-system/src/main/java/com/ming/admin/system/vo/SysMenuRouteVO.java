package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单路由树节点（me 接口的 menus 字段）。
 * <p>只含 type 1（目录）与 2（菜单）的启用菜单；按钮节点只贡献权限标识，不下发。</p>
 */
@Data
public class SysMenuRouteVO {

    /** 主键 */
    @Schema(description = "菜单 id")
    private Long id;

    /** 父节点 id，0 表示顶级 */
    @Schema(description = "父节点 id，0 表示顶级")
    private Long parentId;

    /** 菜单名（用户填什么显示什么） */
    @Schema(description = "菜单名")
    private String name;

    /** 类型：1 目录 2 菜单 */
    @Schema(description = "类型：1 目录 2 菜单")
    private Integer type;

    /** 前端路由路径 */
    @Schema(description = "前端路由路径")
    private String path;

    /** 前端组件路径，为空时前端按 path 约定推导 */
    @Schema(description = "前端组件路径，为空时按 path 推导")
    private String component;

    /** 图标名 */
    @Schema(description = "图标名")
    private String icon;

    /** 排序 */
    @Schema(description = "排序")
    private Integer sort;

    /** 是否显示：1 显示 0 隐藏（隐藏仅不出现在侧边栏，路由仍然注册） */
    @Schema(description = "是否显示：1 显示 0 隐藏")
    private Integer visible;

    /** 是否缓存页面 */
    @Schema(description = "是否缓存页面：1 是 0 否")
    private Integer keepAlive;

    /** 目录重定向地址 */
    @Schema(description = "目录重定向地址")
    private String redirect;

    /** 子节点 */
    @Schema(description = "子节点列表")
    private List<SysMenuRouteVO> children = new ArrayList<>();
}
