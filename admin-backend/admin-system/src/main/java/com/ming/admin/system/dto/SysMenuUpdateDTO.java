package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 菜单修改入参。
 * <p>与新增的差别只有主键；父节点不能指向自己或自己的子孙、层级类型约束、perms 唯一性同样在 Service 校验。</p>
 */
@Data
public class SysMenuUpdateDTO {

    /** 主键 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long id;

    /** 上级菜单 id，0 或空表示顶级 */
    private Long parentId;

    /** 菜单名 */
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    @Size(max = 50, message = ValidationReasonConstants.LEN)
    private String name;

    /** 类型：1 目录 2 菜单 3 按钮 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 1, message = ValidationReasonConstants.RANGE)
    @Max(value = 3, message = ValidationReasonConstants.RANGE)
    private Integer type;

    /** 前端路由路径，目录与菜单必填，必须以 / 开头；按钮没有路由 */
    @Size(max = 200, message = ValidationReasonConstants.LEN)
    private String path;

    /** 前端组件路径，选填：留空时前端按 path 约定推导组件 */
    @Size(max = 200, message = ValidationReasonConstants.LEN)
    private String component;

    /** 图标名 */
    @Size(max = 50, message = ValidationReasonConstants.LEN)
    private String icon;

    /** 权限标识，按钮必填（等于对应接口地址）；目录与菜单必须留空 */
    @Size(max = 200, message = ValidationReasonConstants.LEN)
    private String perms;

    /** 排序 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Integer sort;

    /** 是否显示：1 显示 0 隐藏 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer visible;

    /** 是否缓存页面：1 是 0 否 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer keepAlive;

    /** 目录重定向地址 */
    @Size(max = 200, message = ValidationReasonConstants.LEN)
    private String redirect;

    /** 状态：1 启用 0 禁用（禁用则不下发给前端） */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer status;
}
