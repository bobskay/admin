package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户入参：只含基本信息，不含密码与角色。
 * <p>字段声明顺序与前端编辑弹窗从上到下一致。</p>
 * <p>校验注解全部落在 {@link Group.Update} 分组，Controller 上同时启用 Default 与 Update。</p>
 */
@Data
public class SysUserUpdateDTO {

    /** 主键 */
    @Schema(description = "用户 id")
    @NotNull(message = ValidationReasonConstants.REQUIRED, groups = Group.Update.class)
    private Long id;

    /** 用户名：只读语义，仅保留字段用于回显，后端一律忽略，不写库（用户名不支持修改） */
    @Schema(description = "用户名（只读，后端忽略该字段）")
    private String username;

    /** 显示名 */
    @Schema(description = "昵称（最多 50 位）")
    @NotBlank(message = ValidationReasonConstants.REQUIRED, groups = Group.Update.class)
    @Size(max = 50, message = ValidationReasonConstants.LEN, groups = Group.Update.class)
    private String nickname;

    /** 邮箱，可空 */
    @Schema(description = "邮箱（最多 100 位）")
    @Email(message = ValidationReasonConstants.EMAIL, groups = Group.Update.class)
    @Size(max = 100, message = ValidationReasonConstants.LEN, groups = Group.Update.class)
    private String email;

    /** 手机号，可空 */
    @Schema(description = "手机号（最多 20 位）")
    @Size(max = 20, message = ValidationReasonConstants.LEN, groups = Group.Update.class)
    private String phone;

    /** 状态：必须用 Integer 接收，禁止 Boolean/String */
    @Schema(description = "状态：1 启用 0 禁用")
    @NotNull(message = ValidationReasonConstants.REQUIRED, groups = Group.Update.class)
    @Min(value = 0, message = ValidationReasonConstants.RANGE, groups = Group.Update.class)
    @Max(value = 1, message = ValidationReasonConstants.RANGE, groups = Group.Update.class)
    private Integer status;

    /** 备注，可空 */
    @Schema(description = "备注（最多 255 位）")
    @Size(max = 255, message = ValidationReasonConstants.LEN, groups = Group.Update.class)
    private String remark;
}
