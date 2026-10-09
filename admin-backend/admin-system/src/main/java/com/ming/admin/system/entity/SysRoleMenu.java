package com.ming.admin.system.entity;

import lombok.Data;

/**
 * 角色菜单关联实体，对应表 sys_role_menu。
 * <p>关联表不做逻辑删除：分配权限时直接物理删除旧关联再批量插入。</p>
 */
@Data
public class SysRoleMenu {

    /** 主键，数据库自增 */
    private Long id;

    /** 角色 id */
    private Long roleId;

    /** 菜单 id */
    private Long menuId;
}
