package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启用 / 禁用入参：状态必须用 Integer 接收，禁止 Boolean 或 String。
 */
@Data
public class SysUserChangeStatusDTO {

    /** 目标用户主键 */
    @Schema(description = "用户 id")
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long id;

    /** 目标状态：取值由 @Min/@Max 保证只能是 0 或 1 */
    @Schema(description = "状态：1 启用 0 禁用")
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer status;
}
