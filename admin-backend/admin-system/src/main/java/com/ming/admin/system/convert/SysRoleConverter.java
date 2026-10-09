package com.ming.admin.system.convert;

import com.ming.admin.system.entity.SysRole;
import com.ming.admin.system.vo.SysRoleVO;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 角色实体与出参的转换器（MapStruct，编译期生成实现，Spring 容器内注入使用）。
 */
@Mapper(componentModel = "spring")
public interface SysRoleConverter {

    /** 实体 → 出参 */
    SysRoleVO toVO(SysRole role);

    /** 实体列表 → 出参列表 */
    List<SysRoleVO> toVOList(List<SysRole> roles);
}
