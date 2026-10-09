package com.ming.admin.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ming.admin.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 * <p>复杂查询用 Lambda 条件构造器在 Service 里拼装，不写 XML。</p>
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
