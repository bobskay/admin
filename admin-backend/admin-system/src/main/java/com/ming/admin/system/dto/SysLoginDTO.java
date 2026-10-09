package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参。
 * <p>登录只做必填校验：密码强度规则属于"新增/重置"场景，
 * 登录时若按强度规则拦截，历史密码会得到 100004 而不是 200015，属于多余的信息泄露。</p>
 */
@Data
public class SysLoginDTO {

    /** 登录名 */
    @Schema(description = "用户名")
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    private String username;

    /** 登录密码（明文，仅用于本次校验，绝不落日志、不回传） */
    @Schema(description = "密码")
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    private String password;
}
