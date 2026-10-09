package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色修改入参。
 * <p>code 允许提交：内置超管角色（code = admin）不允许改 code、不允许禁用，由 Service 拦下并返回 200017；
 * 其余角色改 code 需要重新校验唯一性。</p>
 */
@Data
public class SysRoleUpdateDTO {

    /** 主键 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long id;

    /** 角色名 */
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    @Size(max = 50, message = ValidationReasonConstants.LEN)
    private String name;

    /** 角色编码：唯一，只允许字母、数字、下划线、冒号 */
    @NotBlank(message = ValidationReasonConstants.REQUIRED)
    @Size(max = 50, message = ValidationReasonConstants.LEN)
    @Pattern(regexp = "^[A-Za-z0-9_:]+$", message = ValidationReasonConstants.PATTERN)
    private String code;

    /** 排序 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    private Integer sort;

    /** 状态：1 启用 0 禁用；内置超管角色不允许改成禁用 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = ValidationReasonConstants.LEN)
    private String remark;
}
