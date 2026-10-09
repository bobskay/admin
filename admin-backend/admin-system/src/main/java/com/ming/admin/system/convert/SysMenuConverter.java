package com.ming.admin.system.convert;

import com.ming.admin.system.entity.SysMenu;
import com.ming.admin.system.vo.SysMenuVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 菜单实体与树节点出参的转换器（MapStruct，编译期生成实现，Spring 容器内注入使用）。
 * <p>children 不是实体字段：转换时显式忽略，由 TreeUtils 在内存里挂载父子关系。</p>
 */
@Mapper(componentModel = "spring")
public interface SysMenuConverter {

    /** 实体 → 树节点出参（children 由树构建阶段填充） */
    @Mapping(target = "children", ignore = true)
    SysMenuVO toVO(SysMenu menu);

    /** 实体列表 → 树节点出参列表，逐项复用 toVO */
    List<SysMenuVO> toVOList(List<SysMenu> menus);
}
