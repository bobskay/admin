package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 查询角色已关联菜单 id 的入参（角色分配权限弹窗回显用）。
 */
@Data
public class SysRoleMenuIdsDTO {

    /** 角色 id */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long roleId;
}
