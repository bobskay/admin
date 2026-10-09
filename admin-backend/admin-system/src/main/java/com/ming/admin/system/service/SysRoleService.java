package com.ming.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.StatusConstants;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.security.service.PermissionService;
import com.ming.admin.system.convert.SysRoleConverter;
import com.ming.admin.system.dto.SysRoleAssignMenusDTO;
import com.ming.admin.system.dto.SysRolePageDTO;
import com.ming.admin.system.dto.SysRoleSaveDTO;
import com.ming.admin.system.dto.SysRoleUpdateDTO;
import com.ming.admin.system.entity.SysMenu;
import com.ming.admin.system.entity.SysRole;
import com.ming.admin.system.entity.SysRoleMenu;
import com.ming.admin.system.entity.SysUser;
import com.ming.admin.system.entity.SysUserRole;
import com.ming.admin.system.mapper.SysMenuMapper;
import com.ming.admin.system.mapper.SysRoleMapper;
import com.ming.admin.system.mapper.SysRoleMenuMapper;
import com.ming.admin.system.mapper.SysUserMapper;
import com.ming.admin.system.mapper.SysUserRoleMapper;
import com.ming.admin.system.vo.SysRoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色业务服务：分页、下拉列表、详情、增删改、菜单权限分配。
 * <p>不建 service.impl 包，一个业务域只有一个 Service 类，直接继承 MyBatis-Plus 的 ServiceImpl。</p>
 * <p>凡是影响权限的写操作（新增、修改、删除、分配菜单、启停）完成后都要让权限缓存失效：
 * 只调用 {@link PermissionService#refreshPermCache()} 让版本号自增，业务代码不直接碰缓存键。</p>
 */
@Service
@RequiredArgsConstructor
public class SysRoleService extends ServiceImpl<SysRoleMapper, SysRole> {

    /** 角色菜单关联（关联表不做逻辑删除，变更时物理删除旧关联再批量插入） */
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /** 用户角色关联（用于删除前统计角色是否仍被未删除的用户使用） */
    private final SysUserRoleMapper sysUserRoleMapper;

    /** 用户表（统计口径为未逻辑删除的用户） */
    private final SysUserMapper sysUserMapper;

    /** 菜单表（校验分配进来的菜单 id 是否真实存在） */
    private final SysMenuMapper sysMenuMapper;

    /** 实体与出参转换 */
    private final SysRoleConverter sysRoleConverter;

    /** 权限缓存失效（版本号自增） */
    private final PermissionService permissionService;

    /**
     * 角色分页查询。
     * <p>排序只认 DTO 里的白名单列名，字段或方向不合法一律回落 sort asc, id asc，不报错、不返回 500。</p>
     */
    public PageResult<SysRoleVO> pageRoles(SysRolePageDTO query) {
        long pageNum = query.safePageNum();
        long pageSize = query.safePageSize();

        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery();
        String name = StrUtils.trimToNull(query.getName());
        wrapper.like(name != null, SysRole::getName, name);
        String code = StrUtils.trimToNull(query.getCode());
        wrapper.like(code != null, SysRole::getCode, code);
        wrapper.eq(query.getStatus() != null, SysRole::getStatus, query.getStatus());
        String keyword = StrUtils.trimToNull(query.getKeyword());
        if (keyword != null) {
            // 关键字同时匹配角色名与角色编码
            wrapper.and(inner -> inner.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword));
        }
        applySort(wrapper, query);

        Page<SysRole> pageRequest = new Page<>(pageNum, pageSize);
        Page<SysRole> pageResult = page(pageRequest, wrapper);
        List<SysRoleVO> voList = sysRoleConverter.toVOList(pageResult.getRecords());
        return PageResult.of(voList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    /** 启用角色简要列表，供各页面的角色下拉使用（按 sort asc, id asc） */
    public List<SysRoleVO> listEnabledRoles() {
        List<SysRole> roles = list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getStatus, StatusConstants.ENABLED)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
        return sysRoleConverter.toVOList(roles);
    }

    /** 角色详情，角色不存在返回 200013 */
    public SysRoleVO getRoleDetail(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        return sysRoleConverter.toVO(role);
    }

    /** 新增角色：code 唯一，返回新主键 */
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(SysRoleSaveDTO dto) {
        String name = StrUtils.trim(dto.getName());
        String code = StrUtils.trim(dto.getCode());
        checkRoleCodeUnique(code, null);

        SysRole entity = new SysRole();
        entity.setName(name);
        entity.setCode(code);
        entity.setSort(dto.getSort());
        entity.setStatus(dto.getStatus());
        entity.setRemark(StrUtils.trimToNull(dto.getRemark()));
        try {
            save(entity);
        } catch (DataIntegrityViolationException e) {
            // 唯一索引兜底：code 唯一索引是单列索引，已逻辑删除的角色仍占着编码
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
        permissionService.refreshPermCache();
        return entity.getId();
    }

    /**
     * 修改角色。
     * <p>内置超管角色（code = admin）不允许改 code、不允许禁用，违规返回 200017；名称、排序、备注可以改。</p>
     * <p>备注这类可空字段用 UpdateWrapper 的 set 显式赋值，保证"清空备注"真的落库（实体为 null 的字段会被忽略）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRoleUpdateDTO dto) {
        SysRole existing = getById(dto.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        String code = StrUtils.trim(dto.getCode());
        boolean superAdminRole = CommonConstants.SUPER_ADMIN_ROLE_CODE.equals(existing.getCode());
        if (superAdminRole && (!CommonConstants.SUPER_ADMIN_ROLE_CODE.equals(code)
                || StatusConstants.DISABLED.equals(dto.getStatus()))) {
            throw new BusinessException(ResultCode.BUILTIN_SUPER_ADMIN_ROLE_PROTECTED);
        }
        checkRoleCodeUnique(code, dto.getId());

        SysRole entity = new SysRole();
        entity.setId(dto.getId());
        entity.setName(StrUtils.trim(dto.getName()));
        entity.setCode(code);
        entity.setSort(dto.getSort());
        entity.setStatus(dto.getStatus());

        LambdaUpdateWrapper<SysRole> updateWrapper = Wrappers.<SysRole>lambdaUpdate()
                .eq(SysRole::getId, dto.getId())
                .set(SysRole::getRemark, StrUtils.trimToNull(dto.getRemark()));
        try {
            update(entity, updateWrapper);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
        permissionService.refreshPermCache();
    }

    /**
     * 批量删除角色（逻辑删除）。
     * <p>整体拒绝策略：只要待删列表里出现内置超管角色，或任一角色仍被未逻辑删除的用户使用，整批不删。
     * 删除成功后一并物理清理 sys_user_role、sys_role_menu 里的关联行。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoles(List<Long> ids) {
        List<Long> roleIds = CollUtils.distinct(ids);
        if (CollUtils.isEmpty(roleIds)) {
            return;
        }
        List<SysRole> roles = listByIds(roleIds);
        if (CollUtils.isEmpty(roles)) {
            return;
        }
        List<Long> existingIds = new ArrayList<>();
        for (SysRole role : roles) {
            if (CommonConstants.SUPER_ADMIN_ROLE_CODE.equals(role.getCode())) {
                throw new BusinessException(ResultCode.BUILTIN_SUPER_ADMIN_ROLE_PROTECTED);
            }
            existingIds.add(role.getId());
        }
        if (isAnyRoleInUse(existingIds)) {
            throw new BusinessException(ResultCode.ROLE_IN_USE);
        }
        removeByIds(existingIds);
        // 关联表不做逻辑删除：角色的用户关联与菜单关联一并物理清理
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getRoleId, existingIds));
        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, existingIds));
        permissionService.refreshPermCache();
    }

    /** 查询角色已关联的菜单 id（分配权限弹窗回显） */
    public List<Long> selectMenuIds(Long roleId) {
        List<Long> menuIds = sysMenuMapper.selectMenuIdsByRoleId(roleId);
        return CollUtils.isEmpty(menuIds) ? CollUtils.newArrayList() : menuIds;
    }

    /**
     * 给角色分配菜单权限。
     * <p>先校验角色存在（否则 200013），再物理删除旧关联并批量插入去重后的新关联；menuIds 为空数组表示清空。
     * 关联表有 (role_id, menu_id) 联合唯一索引，插入前必须去重。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(SysRoleAssignMenusDTO dto) {
        SysRole role = getById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        // 去重并剔除不存在的菜单 id：弹窗打开期间菜单可能已被删除，这里选择忽略而不是整批报错
        List<Long> menuIds = filterExistingMenuIds(CollUtils.distinct(dto.getMenuIds()));

        sysRoleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, dto.getRoleId()));
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(dto.getRoleId());
            relation.setMenuId(menuId);
            sysRoleMenuMapper.insert(relation);
        }
        permissionService.refreshPermCache();
    }

    /** 角色编码唯一性校验（未逻辑删除记录范围内，排除自身） */
    private void checkRoleCodeUnique(String code, Long excludeId) {
        LambdaQueryWrapper<SysRole> wrapper = Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getCode, code)
                .ne(excludeId != null, SysRole::getId, excludeId);
        if (count(wrapper) > 0) {
            throw new BusinessException(ResultCode.ROLE_CODE_EXISTS);
        }
    }

    /**
     * 角色是否仍被未逻辑删除的用户使用。
     * <p>先取角色下的用户关联，再用一次 in 查询筛出未逻辑删除的用户（逻辑删除条件由 MyBatis-Plus 自动追加），
     * 避免 N+1，也避免把已删除用户的关联算成"使用中"。</p>
     */
    private boolean isAnyRoleInUse(List<Long> roleIds) {
        List<SysUserRole> relations = sysUserRoleMapper.selectList(Wrappers.<SysUserRole>lambdaQuery()
                .select(SysUserRole::getRoleId, SysUserRole::getUserId)
                .in(SysUserRole::getRoleId, roleIds));
        if (CollUtils.isEmpty(relations)) {
            return false;
        }
        Set<Long> userIds = new HashSet<>();
        for (SysUserRole relation : relations) {
            if (relation.getUserId() != null) {
                userIds.add(relation.getUserId());
            }
        }
        if (userIds.isEmpty()) {
            return false;
        }
        List<SysUser> aliveUsers = sysUserMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .select(SysUser::getId)
                .in(SysUser::getId, userIds));
        if (CollUtils.isEmpty(aliveUsers)) {
            return false;
        }
        Set<Long> aliveUserIds = new HashSet<>();
        for (SysUser user : aliveUsers) {
            aliveUserIds.add(user.getId());
        }
        for (SysUserRole relation : relations) {
            if (relation.getUserId() != null && aliveUserIds.contains(relation.getUserId())) {
                return true;
            }
        }
        return false;
    }

    /** 只保留真实存在（未逻辑删除）的菜单 id */
    private List<Long> filterExistingMenuIds(List<Long> menuIds) {
        List<Long> result = CollUtils.newArrayList();
        if (CollUtils.isEmpty(menuIds)) {
            return result;
        }
        List<SysMenu> menus = sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .select(SysMenu::getId)
                .in(SysMenu::getId, menuIds));
        if (CollUtils.isEmpty(menus)) {
            return result;
        }
        for (SysMenu menu : menus) {
            result.add(menu.getId());
        }
        return result;
    }

    /**
     * 拼装排序（7.5 排序字段安全）。
     * <p>列名只能来自 {@link SysRolePageDTO#resolveSortColumn()} 的白名单映射结果，方向只接受 asc / desc，
     * 未命中或非法时回落默认排序 sort asc, id asc；用户输入永远不会原样进入 SQL。</p>
     */
    private void applySort(LambdaQueryWrapper<SysRole> wrapper, SysRolePageDTO query) {
        String column = query.resolveSortColumn();
        if (column == null || !(query.isAsc() || query.isDesc())) {
            wrapper.orderByAsc(SysRole::getSort).orderByAsc(SysRole::getId);
            return;
        }
        wrapper.last("ORDER BY " + column + (query.isAsc() ? " ASC" : " DESC") + ", id ASC");
    }
}
