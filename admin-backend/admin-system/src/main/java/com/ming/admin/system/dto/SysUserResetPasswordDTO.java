package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 重置密码入参：字段声明顺序与前端重置密码弹窗一致。
 */
@Data
public class SysUserResetPasswordDTO {

    /** 目标用户主键 */
    @Schema(description = "用户 id")
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long id;

    /** 新密码：明文入参，入库前用 Argon2id 加密 */
    @Schema(description = "新密码（8-32 位，至少包含字母和数字）")
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    @Size(min = 8, max = 32, message = ValidationReasonConstants.LEN)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\S]{8,32}$", message = ValidationReasonConstants.PATTERN)
    private String newPassword;
}
