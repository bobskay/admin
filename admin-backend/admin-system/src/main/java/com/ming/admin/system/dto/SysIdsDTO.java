package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量删除入参：字段名必须是 ids。
 * <p>全项目所有模块（用户、角色、菜单）的批量删除入参统一叫 ids（数组），
 * 单条删除也传单元素数组，后端只实现一个批量入口。</p>
 */
@Data
public class SysIdsDTO {

    /** 主键集合 */
    @Schema(description = "主键集合（批量删除，单条删除也传单元素数组）")
    @NotEmpty(message = ValidationReasonConstants.REQUIRED)
    private List<Long> ids;
}
