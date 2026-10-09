package com.ming.admin.security.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限查询 Mapper（admin-permission 内部使用，只返回权限标识与角色编码）。
 * <p>统计口径：只统计启用状态、未逻辑删除的菜单，以及启用状态、未逻辑删除的角色。</p>
 */
@Mapper
public interface PermissionMapper {

    /** 查询用户的权限标识集合（菜单行的 perms，非空去重） */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_role r ON r.id = rm.role_id
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND m.perms IS NOT NULL
              AND m.perms <> ''
              AND m.status = 1
              AND m.deleted = 0
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectPermsByUserId(@Param("userId") Long userId);

    /** 查询用户的角色编码集合（只含启用角色） */
    @Select("""
            SELECT DISTINCT r.code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.status = 1
              AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
