package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 角色分配菜单入参。
 * <p>menuIds 允许为空数组（表示清空该角色的全部菜单权限），因此不加 @NotEmpty；
 * 服务端在插入前会先对 id 去重，避免 sys_role_menu 的联合唯一索引冲突。</p>
 */
@Data
public class SysRoleAssignMenusDTO {

    /** 角色 id */
    @NotNull(message = ValidationReasonConstants.REQUIRED)
    private Long roleId;

    /** 勾选的菜单 id 集合，允许为空数组 */
    private List<Long> menuIds;
}
