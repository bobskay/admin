package com.ming.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ming.admin.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单 Mapper。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询某用户可见的路由菜单（只含 type 1、2，只含启用菜单）。
     * <p>只统计启用、未逻辑删除的角色与菜单。</p>
     */
    @Select("""
            SELECT DISTINCT m.*
            FROM sys_menu m
            INNER JOIN sys_role_menu rm ON rm.menu_id = m.id
            INNER JOIN sys_role r ON r.id = rm.role_id
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND m.type IN (1, 2)
              AND m.status = 1
              AND m.deleted = 0
              AND r.status = 1
              AND r.deleted = 0
            ORDER BY m.sort ASC, m.id ASC
            """)
    List<SysMenu> selectRouteMenusByUserId(@Param("userId") Long userId);

    /**
     * 查询某角色的菜单 id 列表（用于角色分配权限弹窗回显）。
     */
    @Select("""
            SELECT rm.menu_id
            FROM sys_role_menu rm
            INNER JOIN sys_menu m ON m.id = rm.menu_id
            WHERE rm.role_id = #{roleId}
              AND m.deleted = 0
            """)
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);
}
