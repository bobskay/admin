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
 * 角色新增入参。
 * <p>校验注解的 message 只能取 7.2 受控词表的六个值之一，前端按 validation.{reason} 拼完整文案。</p>
 */
@Data
public class SysRoleSaveDTO {

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

    /** 状态：1 启用 0 禁用 */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = ValidationReasonConstants.LEN)
    private String remark;
}
