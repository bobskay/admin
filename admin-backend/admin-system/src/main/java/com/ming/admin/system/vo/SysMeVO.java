package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * me 接口出参：前端登录后一次性拿到用户、角色、超管标识、权限标识与菜单路由树。
 * <p>超级管理员的判据只看 superAdmin 布尔值；perms 里的 *:*:* 仅作展示与兼容。</p>
 */
@Data
public class SysMeVO {

    /** 用户基本信息 */
    @Schema(description = "用户基本信息")
    private SysUserInfoVO user;

    /** 角色编码集合（只含启用角色） */
    @Schema(description = "角色编码集合")
    private List<String> roles = new ArrayList<>();

    /** 是否超级管理员（唯一判据） */
    @Schema(description = "是否超级管理员")
    private Boolean superAdmin;

    /** 权限标识集合；超级管理员返回单元素 [*:*:*]（仅展示用） */
    @Schema(description = "权限标识集合")
    private List<String> perms = new ArrayList<>();

    /** 可见菜单路由树（只含 type 1、2 的启用菜单） */
    @Schema(description = "可见菜单路由树")
    private List<SysMenuRouteVO> menus = new ArrayList<>();
}
