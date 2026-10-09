package com.ming.admin.security.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 登录用户模型：序列化为 JSON 后存入 Redis（键 admin:token:{tokenId}）。
 * <p>超级管理员的唯一判据是本对象的 superAdmin 布尔值，任何地方都不要去匹配 *:*:*。</p>
 * <p>密码与密码哈希绝不进入本对象。</p>
 */
@Data
public class LoginUser {

    /** 用户 id */
    private Long userId;

    /** 登录名 */
    private String username;

    /** 显示名 */
    private String nickname;

    /** 是否超级管理员（登录时按角色编码是否包含 admin 计算） */
    private Boolean superAdmin;

    /** 角色编码列表（只含启用角色） */
    private List<String> roles = new ArrayList<>();

    /** 登录时间 */
    private Date loginTime;

    /** 登录 IP */
    private String loginIp;

    /** 浏览器标识 */
    private String userAgent;
}
