package com.ming.admin.system.entity;

import lombok.Data;

/**
 * 用户角色关联实体，对应表 sys_user_role。
 * <p>关联表不做逻辑删除：分配关系变化时直接物理删除旧关联再批量插入。</p>
 */
@Data
public class SysUserRole {

    /** 主键，数据库自增 */
    private Long id;

    /** 用户 id */
    private Long userId;

    /** 角色 id */
    private Long roleId;
}
