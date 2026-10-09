package com.ming.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ming.admin.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关联 Mapper（由用户服务维护）。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
