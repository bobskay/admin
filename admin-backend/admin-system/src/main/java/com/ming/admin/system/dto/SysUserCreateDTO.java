package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增用户入参。
 * <p>字段声明顺序与前端新增弹窗从上到下一致：用户名、密码、昵称、邮箱、手机号、状态、角色、备注。</p>
 * <p>校验注解全部落在 {@link Group.Create} 分组，Controller 上同时启用 Default 与 Create。</p>
 */
@Data
public class SysUserCreateDTO {

    /** 用户名：登录名，单列唯一，逻辑删除后永久不可复用 */
    @Schema(description = "用户名（3-50 位，只允许字母、数字、下划线）")
    @NotBlank(message = ValidationReasonConstants.REQUIRED, groups = Group.Create.class)
    @Size(min = 3, max = 50, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = ValidationReasonConstants.PATTERN, groups = Group.Create.class)
    private String username;

    /** 密码：明文入参，入库前用 Argon2id 加密 */
    @Schema(description = "密码（8-32 位，至少包含字母和数字）")
    @NotBlank(message = ValidationReasonConstants.REQUIRED, groups = Group.Create.class)
    @Size(min = 8, max = 32, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[\\S]{8,32}$", message = ValidationReasonConstants.PATTERN,
            groups = Group.Create.class)
    private String password;

    /** 显示名 */
    @Schema(description = "昵称（最多 50 位）")
    @NotBlank(message = ValidationReasonConstants.REQUIRED, groups = Group.Create.class)
    @Size(max = 50, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    private String nickname;

    /** 邮箱，可空 */
    @Schema(description = "邮箱（最多 100 位）")
    @Email(message = ValidationReasonConstants.EMAIL, groups = Group.Create.class)
    @Size(max = 100, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    private String email;

    /** 手机号，可空 */
    @Schema(description = "手机号（最多 20 位）")
    @Size(max = 20, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    private String phone;

    /** 状态：必须用 Integer 接收，禁止 Boolean/String */
    @Schema(description = "状态：1 启用 0 禁用")
    @NotNull(message = ValidationReasonConstants.REQUIRED, groups = Group.Create.class)
    @Min(value = 0, message = ValidationReasonConstants.RANGE, groups = Group.Create.class)
    @Max(value = 1, message = ValidationReasonConstants.RANGE, groups = Group.Create.class)
    private Integer status;

    /** 角色：创建时至少一个（分配角色的接口允许空数组，两者校验分组不同） */
    @Schema(description = "角色 id 集合（新增时至少一个）")
    @NotEmpty(message = ValidationReasonConstants.REQUIRED, groups = Group.Create.class)
    private List<Long> roleIds;

    /** 备注，可空 */
    @Schema(description = "备注（最多 255 位）")
    @Size(max = 255, message = ValidationReasonConstants.LEN, groups = Group.Create.class)
    private String remark;
}
