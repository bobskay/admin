package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 分配角色入参。
 * <p>roleIds 允许为空数组（表示清空该用户的全部角色），因此这里不加 @NotEmpty；
 * 与"新增用户时角色至少一个"的校验分组区分开。</p>
 */
@Data
public class SysUserAssignRolesDTO {

    /** 目标用户主键 */
    @Schema(description = "用户 id")
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long userId;

    /** 角色 id 集合：空数组表示清空角色 */
    @Schema(description = "角色 id 集合（空数组表示清空角色）")
    private List<Long> roleIds;
}
