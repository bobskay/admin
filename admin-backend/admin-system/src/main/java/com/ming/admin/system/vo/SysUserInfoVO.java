package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 当前登录用户的基本信息（me 接口的 user 字段）。
 * <p>不含 password 与 remark：密码只进不出，remark 不属于登录态信息。</p>
 */
@Data
public class SysUserInfoVO {

    /** 主键 */
    @Schema(description = "用户 id")
    private Long id;

    /** 登录名 */
    @Schema(description = "用户名")
    private String username;

    /** 显示名 */
    @Schema(description = "昵称")
    private String nickname;

    /** 邮箱 */
    @Schema(description = "邮箱")
    private String email;

    /** 手机号 */
    @Schema(description = "手机号")
    private String phone;

    /** 头像地址 */
    @Schema(description = "头像地址")
    private String avatar;

    /** 状态：1 启用 0 禁用 */
    @Schema(description = "状态：1 启用 0 禁用")
    private Integer status;

    /** 创建时间（毫秒时间戳） */
    @Schema(description = "创建时间（毫秒时间戳）")
    private Date createTime;

    /** 已分配的角色简要信息 */
    @Schema(description = "角色列表")
    private List<SysRoleBriefVO> roles = new ArrayList<>();
}
