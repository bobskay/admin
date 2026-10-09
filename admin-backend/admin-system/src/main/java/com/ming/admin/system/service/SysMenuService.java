package com.ming.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.MenuConstants;
import com.ming.admin.common.constant.StatusConstants;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.common.util.TreeUtils;
import com.ming.admin.security.scanner.ApiRegistry;
import com.ming.admin.security.scanner.PermissionItem;
import com.ming.admin.security.scanner.ValidationRuleItem;
import com.ming.admin.security.service.PermissionService;
import com.ming.admin.system.convert.SysMenuConverter;
import com.ming.admin.system.dto.SysMenuSaveDTO;
import com.ming.admin.system.dto.SysMenuUpdateDTO;
import com.ming.admin.system.entity.SysMenu;
import com.ming.admin.system.entity.SysRoleMenu;
import com.ming.admin.system.mapper.SysMenuMapper;
import com.ming.admin.system.mapper.SysRoleMenuMapper;
import com.ming.admin.system.vo.SysMenuVO;
import com.ming.admin.system.vo.SysPermItemVO;
import com.ming.admin.system.vo.SysValidationRuleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单业务服务：全量树、启用菜单树、权限标识与校验规则清单、详情、增删改。
 * <p>菜单一张表承担三件事（侧边栏展示、接口权限点、按钮权限点），因此这里的每条写规则都必须同时顾及三者：
 * 目录与菜单不贡献权限（perms 写 null），按钮必须有 perms 且必须唯一。</p>
 * <p>菜单的增删改与启停都会改变权限集合，写操作完成后一律调用
 * {@link PermissionService#refreshPermCache()} 让权限缓存版本号自增。</p>
 */
@Service
@RequiredArgsConstructor
public class SysMenuService extends ServiceImpl<SysMenuMapper, SysMenu> {

    /** 角色菜单关联（删除菜单时物理清理关联行） */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** 实体与树节点出参转换 */
    private final SysMenuConverter sysMenuConverter;

    /** 启动扫描结果的内存注册表：权限标识清单与校验规则清单 */
    private final ApiRegistry apiRegistry;

    /** 权限缓存失效（版本号自增） */
    private final PermissionService permissionService;

    /** 菜单管理页用的全量树（含按钮、含禁用菜单），按 sort asc, id asc 排序后内存构建 */
    public List<SysMenuVO> treeMenus() {
        return buildTree(list(Wrappers.<SysMenu>lambdaQuery()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId)));
    }

    /** 角色分配权限弹窗用的启用菜单树（只含 status = 1，目录 / 菜单 / 按钮三级全含） */
    public List<SysMenuVO> listEnabledMenus() {
        return buildTree(list(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, StatusConstants.ENABLED)
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getId)));
    }

    /** 菜单详情，不存在返回 200021 */
    public SysMenuVO getMenuDetail(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            throw new BusinessException(ResultCode.MENU_NOT_FOUND);
        }
        SysMenuVO vo = sysMenuConverter.toVO(menu);
        // 与树节点结构保持一致：单节点出参的 children 也给空列表，前端不用做 null 判断
        vo.setChildren(CollUtils.newArrayList());
        return vo;
    }

    /** 权限标识清单：直接读启动扫描结果，清单为空时返回空数组，不报错 */
    public List<SysPermItemVO> listPermItems() {
        List<PermissionItem> items = apiRegistry.getPermissionItems();
        List<SysPermItemVO> result = CollUtils.newArrayList();
        if (CollUtils.isEmpty(items)) {
            return result;
        }
        for (PermissionItem item : items) {
            SysPermItemVO vo = new SysPermItemVO();
            vo.setPerm(item.getPerm());
            vo.setModule(item.getModule());
            vo.setAction(item.getAction());
            result.add(vo);
        }
        return result;
    }

    /** 校验规则清单：直接读启动扫描结果，清单为空时返回空数组，不报错 */
    public List<SysValidationRuleVO> listValidationRules() {
        List<ValidationRuleItem> items = apiRegistry.getValidationRules();
        List<SysValidationRuleVO> result = CollUtils.newArrayList();
        if (CollUtils.isEmpty(items)) {
            return result;
        }
        for (ValidationRuleItem item : items) {
            SysValidationRuleVO vo = new SysValidationRuleVO();
            vo.setField(item.getField());
            vo.setReason(item.getReason());
            vo.setLabelKey(item.getLabelKey());
            result.add(vo);
        }
        return result;
    }

    /** 新增菜单：校验层级类型、必填项（目录/菜单的 path、按钮的 perms）、perms 唯一性，返回新主键 */
    @Transactional(rollbackFor = Exception.class)
    public Long createMenu(SysMenuSaveDTO dto) {
        Long parentId = normalizeParentId(dto.getParentId());
        SysMenu parent = loadParent(parentId);
        checkTypeHierarchy(dto.getType(), parentId, parent);
        String path = normalizePath(dto.getType(), dto.getPath());
        String perms = normalizePerms(dto.getType(), dto.getPerms());
        checkPermsUnique(perms, null);

        SysMenu entity = new SysMenu();
        entity.setParentId(parentId);
        entity.setName(StrUtils.trim(dto.getName()));
        entity.setType(dto.getType());
        entity.setPath(path);
        entity.setComponent(StrUtils.trimToNull(dto.getComponent()));
        entity.setIcon(StrUtils.trimToNull(dto.getIcon()));
        entity.setPerms(perms);
        entity.setSort(dto.getSort());
        entity.setVisible(dto.getVisible());
        entity.setKeepAlive(dto.getKeepAlive());
        entity.setRedirect(StrUtils.trimToNull(dto.getRedirect()));
        entity.setStatus(dto.getStatus());
        try {
            save(entity);
        } catch (DataIntegrityViolationException e) {
            // perms 唯一索引兜底：已逻辑删除的菜单行仍占着权限标识
            throw new BusinessException(ResultCode.MENU_PERMS_EXISTS);
        }
        permissionService.refreshPermCache();
        return entity.getId();
    }

    /**
     * 修改菜单。
     * <p>校验顺序：菜单存在 → 上级菜单存在 → 层级类型约束 → 上级不能是自己或自己的子孙 → 必填项 → perms 唯一性。</p>
     * <p>可空字段（path、component、icon、perms、redirect）用 UpdateWrapper 的 set 显式赋值，
     * 保证"清空"操作真的落库（实体里为 null 的字段会被 MyBatis-Plus 忽略）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(SysMenuUpdateDTO dto) {
        SysMenu existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.MENU_NOT_FOUND);
        }
        Long parentId = normalizeParentId(dto.getParentId());
        SysMenu parent = loadParent(parentId);
        checkTypeHierarchy(dto.getType(), parentId, parent);
        checkParentNotSelfOrDescendant(dto.getId(), parentId);
        String path = normalizePath(dto.getType(), dto.getPath());
        String perms = normalizePerms(dto.getType(), dto.getPerms());
        checkPermsUnique(perms, dto.getId());

        SysMenu entity = new SysMenu();
        entity.setId(dto.getId());
        entity.setParentId(parentId);
        entity.setName(StrUtils.trim(dto.getName()));
        entity.setType(dto.getType());
        entity.setSort(dto.getSort());
        entity.setVisible(dto.getVisible());
        entity.setKeepAlive(dto.getKeepAlive());
        entity.setStatus(dto.getStatus());

        LambdaUpdateWrapper<SysMenu> updateWrapper = Wrappers.<SysMenu>lambdaUpdate()
                .eq(SysMenu::getId, dto.getId())
                .set(SysMenu::getPath, path)
                .set(SysMenu::getComponent, StrUtils.trimToNull(dto.getComponent()))
                .set(SysMenu::getIcon, StrUtils.trimToNull(dto.getIcon()))
                .set(SysMenu::getPerms, perms)
                .set(SysMenu::getRedirect, StrUtils.trimToNull(dto.getRedirect()));
        try {
            update(entity, updateWrapper);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ResultCode.MENU_PERMS_EXISTS);
        }
        permissionService.refreshPermCache();
    }

    /**
     * 批量删除菜单（逻辑删除）。
     * <p>整体拒绝策略：待删列表里任一菜单存在未删除的子节点，整批都不删（不允许"删一半"）并返回 200018。
     * 删除成功后物理清理 sys_role_menu 里这些菜单的关联行。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenus(List<Long> ids) {
        List<Long> menuIds = CollUtils.distinct(ids);
        if (CollUtils.isEmpty(menuIds)) {
            return;
        }
        List<SysMenu> menus = listByIds(menuIds);
        if (CollUtils.isEmpty(menus)) {
            return;
        }
        List<Long> existingIds = new ArrayList<>();
        for (SysMenu menu : menus) {
            existingIds.add(menu.getId());
        }
        long childCount = count(Wrappers.<SysMenu>lambdaQuery().in(SysMenu::getParentId, existingIds));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.MENU_HAS_CHILDREN);
        }
        removeByIds(existingIds);
        // 关联表不做逻辑删除：菜单被删后遗留的角色关联行要一并物理清理
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getMenuId, existingIds));
        permissionService.refreshPermCache();
    }

    /** 全量取出后按内存递归构建树（禁止递归 SQL），父节点缺失的孤儿节点由 TreeUtils 自愈为顶级 */
    private List<SysMenuVO> buildTree(List<SysMenu> menus) {
        List<SysMenuVO> nodes = sysMenuConverter.toVOList(menus);
        return TreeUtils.build(nodes,
                SysMenuVO::getId,
                SysMenuVO::getParentId,
                SysMenuVO::setChildren,
                CommonConstants.ROOT_PARENT_ID);
    }

    /** 上级菜单 id 归一化：空视为顶级 */
    private Long normalizeParentId(Long parentId) {
        return parentId == null ? CommonConstants.ROOT_PARENT_ID : parentId;
    }

    /** 取上级菜单：顶级返回 null；上级不存在属于"上级菜单不合法"，返回 200019 */
    private SysMenu loadParent(Long parentId) {
        if (CommonConstants.ROOT_PARENT_ID.equals(parentId)) {
            return null;
        }
        SysMenu parent = getById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.MENU_PARENT_INVALID);
        }
        return parent;
    }

    /**
     * 层级类型约束（11.3）：目录挂顶级或目录、菜单挂目录、按钮挂菜单。
     * <p>不合法一律 100004：这是跨字段规则，注解表达不了，用业务异常携带校验失败码。</p>
     */
    private void checkTypeHierarchy(Integer type, Long parentId, SysMenu parent) {
        if (CommonConstants.ROOT_PARENT_ID.equals(parentId)) {
            if (!MenuConstants.TYPE_DIRECTORY.equals(type)) {
                throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                        "Only directory can be a top level menu");
            }
            return;
        }
        Integer parentType = parent.getType();
        boolean allowed = (MenuConstants.TYPE_DIRECTORY.equals(type) && MenuConstants.TYPE_DIRECTORY.equals(parentType))
                || (MenuConstants.TYPE_MENU.equals(type) && MenuConstants.TYPE_DIRECTORY.equals(parentType))
                || (MenuConstants.TYPE_BUTTON.equals(type) && MenuConstants.TYPE_MENU.equals(parentType));
        if (!allowed) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "Menu type does not match parent menu type");
        }
    }

    /**
     * 上级菜单不能是自己或自己的子孙。
     * <p>全量取出 id 与 parent_id 后在内存里自顶向下广度遍历子孙（禁止递归 SQL），
     * 用 visited 集合兜住脏数据形成的环。</p>
     */
    private void checkParentNotSelfOrDescendant(Long id, Long parentId) {
        if (id == null || parentId == null || CommonConstants.ROOT_PARENT_ID.equals(parentId)) {
            return;
        }
        if (id.equals(parentId)) {
            throw new BusinessException(ResultCode.MENU_PARENT_INVALID);
        }
        List<SysMenu> all = list(Wrappers.<SysMenu>lambdaQuery().select(SysMenu::getId, SysMenu::getParentId));
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (SysMenu menu : all) {
            if (menu.getId() == null || menu.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(menu.getParentId(), key -> new ArrayList<>()).add(menu.getId());
        }
        Deque<Long> queue = new ArrayDeque<>();
        List<Long> children = childrenMap.get(id);
        if (children != null) {
            queue.addAll(children);
        }
        Set<Long> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current == null || !visited.add(current)) {
                continue;
            }
            if (current.equals(parentId)) {
                throw new BusinessException(ResultCode.MENU_PARENT_INVALID);
            }
            List<Long> grandChildren = childrenMap.get(current);
            if (grandChildren != null) {
                queue.addAll(grandChildren);
            }
        }
    }

    /**
     * 路由路径规范化：目录与菜单必填且必须以 / 开头；按钮没有路由，统一写 null。
     * <p>不合法返回 100004。</p>
     */
    private String normalizePath(Integer type, String path) {
        if (MenuConstants.TYPE_BUTTON.equals(type)) {
            return null;
        }
        String value = StrUtils.trimToNull(path);
        if (value == null) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "Path is required for directory and menu");
        }
        if (!StrUtils.startWith(value, "/")) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "Path must start with '/'");
        }
        return value;
    }

    /**
     * 权限标识规范化：按钮必填（为空返回 100004）；目录与菜单不贡献权限，一律写 null。
     * <p>写 null 而不是空字符串是硬要求：perms 上有唯一索引，空字符串会让第二条目录直接撞索引。</p>
     */
    private String normalizePerms(Integer type, String perms) {
        if (!MenuConstants.TYPE_BUTTON.equals(type)) {
            return null;
        }
        String value = StrUtils.trimToNull(perms);
        if (value == null) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "Perms is required for button");
        }
        return value;
    }

    /** perms 唯一性校验（未逻辑删除记录范围内，排除自身），重复返回 200020 */
    private void checkPermsUnique(String perms, Long excludeId) {
        if (perms == null) {
            return;
        }
        LambdaQueryWrapper<SysMenu> wrapper = Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getPerms, perms)
                .ne(excludeId != null, SysMenu::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.MENU_PERMS_EXISTS);
        }
    }
}
