package com.ming.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ming.admin.system.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色菜单关联 Mapper（由角色服务维护）。
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {
}
