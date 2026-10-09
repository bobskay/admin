package com.ming.admin.system.convert;

import com.ming.admin.system.entity.SysRole;
import com.ming.admin.system.entity.SysUser;
import com.ming.admin.system.vo.SysRoleBriefVO;
import com.ming.admin.system.vo.SysUserInfoVO;
import com.ming.admin.system.vo.SysUserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户相关对象转换器（MapStruct，编译期生成实现类并注册为 Spring Bean）。
 * <p>只做纯字段搬运，不写业务逻辑；角色信息由 Service 批量查询后回填，避免 N+1。</p>
 * <p>注意：源实体的 password 字段不会出现在任何 VO 的映射目标里，密码只进不出。</p>
 */
@Mapper(componentModel = "spring")
public interface SysUserConverter {

    /** 用户实体 → 列表/详情 VO（roles 由 Service 填充，这里显式忽略） */
    @Mapping(target = "roles", ignore = true)
    SysUserVO toVO(SysUser user);

    /** 用户实体集合 → VO 集合 */
    List<SysUserVO> toVOList(List<SysUser> users);

    /** 用户实体 → me 接口的用户信息（roles 由 Service 填充，这里显式忽略） */
    @Mapping(target = "roles", ignore = true)
    SysUserInfoVO toInfoVO(SysUser user);

    /** 角色实体 → 角色简要信息（三个字段全部显式声明映射） */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "code", source = "code")
    SysRoleBriefVO toRoleBrief(SysRole role);

    /** 角色实体集合 → 角色简要信息集合（元素映射复用 toRoleBrief） */
    List<SysRoleBriefVO> toRoleBriefList(List<SysRole> roles);
}
