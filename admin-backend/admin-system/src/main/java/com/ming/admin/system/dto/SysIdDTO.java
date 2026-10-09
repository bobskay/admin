package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 单主键入参：详情等按 id 查询的接口使用。
 */
@Data
public class SysIdDTO {

    /** 主键 */
    @Schema(description = "主键 id")
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long id;
}
