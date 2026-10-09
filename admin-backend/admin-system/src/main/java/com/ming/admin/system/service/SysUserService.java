package com.ming.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.StatusConstants;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.ObjUtils;
import com.ming.admin.common.util.PasswordUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.security.context.LoginUserContext;
import com.ming.admin.security.service.PermissionService;
import com.ming.admin.system.convert.SysUserConverter;
import com.ming.admin.system.dto.SysUserAssignRolesDTO;
import com.ming.admin.system.dto.SysUserChangeStatusDTO;
import com.ming.admin.system.dto.SysUserCreateDTO;
import com.ming.admin.system.dto.SysUserPageDTO;
import com.ming.admin.system.dto.SysUserResetPasswordDTO;
import com.ming.admin.system.dto.SysUserUpdateDTO;
import com.ming.admin.system.entity.SysRole;
import com.ming.admin.system.entity.SysUser;
import com.ming.admin.system.entity.SysUserRole;
import com.ming.admin.system.mapper.SysRoleMapper;
import com.ming.admin.system.mapper.SysUserMapper;
import com.ming.admin.system.mapper.SysUserRoleMapper;
import com.ming.admin.system.vo.SysRoleBriefVO;
import com.ming.admin.system.vo.SysUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户业务服务：分页、详情、新增、修改、批量逻辑删除、重置密码、启停用、分配角色。
 * <p>不建 service.impl 包，直接在这里标注 Spring 组件；单表条件用条件构造器拼装，不写 XML。</p>
 * <p>凡是影响权限的写操作完成后调用 {@link PermissionService#refreshPermCache()}（版本号自增使旧缓存失效）。</p>
 */
@Service
@RequiredArgsConstructor
public class SysUserService extends ServiceImpl<SysUserMapper, SysUser> {

    /**
     * 用户名唯一性判定 SQL：必须覆盖已逻辑删除的记录。
     * <p>MyBatis-Plus 的逻辑删除会对所有 BaseMapper 方法自动追加 deleted = 0，条件构造器无法绕过；
     * 而 5 个契约 Mapper 不允许改动，因此这里用 Spring 的 JdbcTemplate 做一次不带逻辑删除过滤的存在性判断。
     * 业务代码依赖白名单允许 org.springframework.*，这里没有引入任何新的第三方依赖。</p>
     */
    private static final String SQL_COUNT_USERNAME = "SELECT COUNT(1) FROM sys_user WHERE username = ?";

    /** 数据库列名（拼排序与条件时只用这里的常量，绝不使用请求里的原始字符串） */
    private static final String COL_ID = "id";

    private static final String COL_USERNAME = "username";

    private static final String COL_NICKNAME = "nickname";

    private static final String COL_STATUS = "status";

    private static final String COL_CREATE_TIME = "create_time";

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserConverter sysUserConverter;

    private final PermissionService permissionService;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 用户分页列表：用户名/昵称模糊、状态精确、角色 in 过滤，排序走白名单。
     * <p>角色信息用「一次批量查关联表 + 一次批量查角色表」组装，避免 N+1。</p>
     */
    public PageResult<SysUserVO> page(SysUserPageDTO query) {
        long pageNum = query.safePageNum();
        long pageSize = query.safePageSize();

        QueryWrapper<SysUser> wrapper = Wrappers.query();
        if (StrUtils.isNotBlank(query.getUsername())) {
            wrapper.like(COL_USERNAME, StrUtils.trim(query.getUsername()));
        }
        if (StrUtils.isNotBlank(query.getNickname())) {
            wrapper.like(COL_NICKNAME, StrUtils.trim(query.getNickname()));
        }
        if (ObjUtils.isNotNull(query.getStatus())) {
            wrapper.eq(COL_STATUS, query.getStatus());
        }
        if (ObjUtils.isNotNull(query.getRoleId())) {
            List<Long> userIds = listUserIdsByRoleId(query.getRoleId());
            if (CollUtils.isEmpty(userIds)) {
                // 该角色下没有任何用户：直接返回空分页，不再查库
                return PageResult.empty(pageNum, pageSize);
            }
            wrapper.in(COL_ID, userIds);
        }
        applySort(wrapper, query);

        Page<SysUser> pageRequest = new Page<>(pageNum, pageSize);
        Page<SysUser> pageData = baseMapper.selectPage(pageRequest, wrapper);

        List<SysUserVO> records = sysUserConverter.toVOList(pageData.getRecords());
        fillRoles(records);
        return PageResult.of(records, pageData.getTotal(), pageData.getCurrent(), pageData.getSize());
    }

    /** 用户详情：带角色信息；用户不存在返回 200008 */
    public SysUserVO detail(Long id) {
        SysUser user = getById(id);
        if (ObjUtils.isNull(user)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        SysUserVO userVO = sysUserConverter.toVO(user);
        userVO.setRoles(listRoleBriefs(id));
        return userVO;
    }

    /** 某个用户已分配角色的简要信息（详情与 me 接口共用） */
    public List<SysRoleBriefVO> listRoleBriefs(Long userId) {
        if (ObjUtils.isNull(userId)) {
            return new ArrayList<>();
        }
        return loadUserRoleBriefs(List.of(userId)).getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 新增用户：用户名唯一（含已删除记录）、角色必须都是启用状态、密码加密入库。
     * <p>先写 sys_user 拿到自增 id，再批量插入 sys_user_role（先去重）。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SysUserCreateDTO dto) {
        String username = StrUtils.trim(dto.getUsername());
        // 1. 用户名唯一性：范围是全部记录（含已逻辑删除），逻辑删除后用户名永久不可复用
        if (existsUsername(username)) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        // 2. 角色至少一个（校验分组已保证），且每个角色都存在、未删除、处于启用状态
        List<Long> roleIds = CollUtils.distinct(dto.getRoleIds());
        checkRolesEnabled(roleIds);

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode(dto.getPassword()));
        user.setNickname(StrUtils.trim(dto.getNickname()));
        user.setEmail(StrUtils.trimToNull(dto.getEmail()));
        user.setPhone(StrUtils.trimToNull(dto.getPhone()));
        user.setStatus(dto.getStatus());
        user.setRemark(StrUtils.trimToNull(dto.getRemark()));
        try {
            save(user);
        } catch (DuplicateKeyException e) {
            // 兜底：并发插入或历史数据造成唯一索引冲突时同样按 200004 返回，不把 100008 抛给用户
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        saveUserRoles(user.getId(), roleIds);
        permissionService.refreshPermCache();
        return user.getId();
    }

    /**
     * 修改用户基本信息：用户名不允许修改（DTO 里的 username 一律忽略，不写库），角色走独立接口。
     */
    public void update(SysUserUpdateDTO dto) {
        SysUser existing = getById(dto.getId());
        if (ObjUtils.isNull(existing)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        SysUser update = new SysUser();
        update.setId(dto.getId());
        update.setNickname(StrUtils.trim(dto.getNickname()));
        update.setEmail(patchText(dto.getEmail()));
        update.setPhone(patchText(dto.getPhone()));
        update.setStatus(dto.getStatus());
        update.setRemark(patchText(dto.getRemark()));
        updateById(update);
        permissionService.refreshPermCache();
    }

    /**
     * 批量逻辑删除用户：逐个判断，命中当前登录用户或超级管理员账号即整体拒绝，不做"删一半"。
     * <p>删除后同步物理清理这些用户的 sys_user_role 关联。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        List<Long> targetIds = CollUtils.distinct(ids);
        if (CollUtils.isEmpty(targetIds)) {
            return;
        }
        Long currentUserId = LoginUserContext.getUserId();
        if (ObjUtils.isNotNull(currentUserId) && targetIds.contains(currentUserId)) {
            throw new BusinessException(ResultCode.CANNOT_OPERATE_CURRENT_USER);
        }
        // 逐个判断是否包含超级管理员账号：只要命中一个就整体拒绝
        List<SysUser> targets = listByIds(targetIds);
        for (SysUser target : targets) {
            if (isSuperAdminAccount(target.getId())) {
                throw new BusinessException(ResultCode.CANNOT_OPERATE_SUPER_ADMIN);
            }
        }
        // 逻辑删除（策略已在 admin-permission 的配置类里集中声明），关联表按设计物理删除
        removeByIds(targetIds);
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, targetIds));
        permissionService.refreshPermCache();
    }

    /** 重置密码：新密码按 Argon2id 加密后更新，明文与哈希都不回传、不落日志 */
    public void resetPassword(SysUserResetPasswordDTO dto) {
        SysUser existing = getById(dto.getId());
        if (ObjUtils.isNull(existing)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        SysUser update = new SysUser();
        update.setId(dto.getId());
        update.setPassword(PasswordUtils.encode(dto.getNewPassword()));
        updateById(update);
    }

    /** 启用 / 禁用：不允许禁用当前登录用户，也不允许禁用超级管理员账号 */
    public void changeStatus(SysUserChangeStatusDTO dto) {
        SysUser existing = getById(dto.getId());
        if (ObjUtils.isNull(existing)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (StatusConstants.DISABLED.equals(dto.getStatus())) {
            if (ObjUtils.equals(LoginUserContext.getUserId(), dto.getId())) {
                throw new BusinessException(ResultCode.CANNOT_OPERATE_CURRENT_USER);
            }
            if (isSuperAdminAccount(dto.getId())) {
                throw new BusinessException(ResultCode.CANNOT_OPERATE_SUPER_ADMIN);
            }
        }
        SysUser update = new SysUser();
        update.setId(dto.getId());
        update.setStatus(dto.getStatus());
        updateById(update);
    }

    /**
     * 分配角色：roleIds 允许空数组（表示清空角色）。
     * <p>先物理删除旧关联，再批量插入新关联（先去重），整体在事务内完成。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(SysUserAssignRolesDTO dto) {
        SysUser existing = getById(dto.getUserId());
        if (ObjUtils.isNull(existing)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        List<Long> roleIds = CollUtils.distinct(dto.getRoleIds());
        if (CollUtils.isNotEmpty(roleIds)) {
            // 只接受存在、未删除且启用状态的角色，空数组表示清空角色
            checkRolesEnabled(roleIds);
        }
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, dto.getUserId()));
        saveUserRoles(dto.getUserId(), roleIds);
        permissionService.refreshPermCache();
    }

    /**
     * 排序：只使用 DTO 白名单映射后的数据库列名，排序方向只接受 asc / desc（忽略大小写）。
     * <p>字段为空、不在白名单、方向非法 → 回落默认排序 create_time desc, id desc，不报错。
     * 严禁把请求里的字符串直接传给 order by。</p>
     */
    private void applySort(QueryWrapper<SysUser> wrapper, SysUserPageDTO query) {
        String column = query.resolveSortColumn();
        if (StrUtils.isNotBlank(column) && (query.isAsc() || query.isDesc())) {
            if (query.isAsc()) {
                wrapper.orderByAsc(column);
            } else {
                wrapper.orderByDesc(column);
            }
            return;
        }
        wrapper.orderByDesc(COL_CREATE_TIME).orderByDesc(COL_ID);
    }

    /** 用户名是否已存在（含已逻辑删除的记录）：存在即不可复用 */
    private boolean existsUsername(String username) {
        if (StrUtils.isBlank(username)) {
            return false;
        }
        Long count = jdbcTemplate.queryForObject(SQL_COUNT_USERNAME, Long.class, username);
        return ObjUtils.isNotNull(count) && count > 0L;
    }

    /** 校验角色 id 集合：必须至少一个，且每个角色都存在、未逻辑删除、状态为启用 */
    private void checkRolesEnabled(List<Long> roleIds) {
        if (CollUtils.isEmpty(roleIds)) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, StatusConstants.ENABLED));
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND);
        }
    }

    /** 按角色查用户 id 列表（先查关联表，再在用户表上 in 过滤） */
    private List<Long> listUserIdsByRoleId(Long roleId) {
        List<SysUserRole> relations = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, roleId));
        List<Long> userIds = new ArrayList<>();
        for (SysUserRole relation : relations) {
            userIds.add(relation.getUserId());
        }
        return CollUtils.distinct(userIds);
    }

    /** 批量插入用户角色关联（调用方已去重） */
    private void saveUserRoles(Long userId, List<Long> roleIds) {
        if (ObjUtils.isNull(userId) || CollUtils.isEmpty(roleIds)) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            sysUserRoleMapper.insert(relation);
        }
    }

    /** 列表补齐角色信息（一次批量查关联表 + 一次批量查角色表） */
    private void fillRoles(List<SysUserVO> users) {
        if (CollUtils.isEmpty(users)) {
            return;
        }
        List<Long> userIds = new ArrayList<>();
        for (SysUserVO user : users) {
            userIds.add(user.getId());
        }
        Map<Long, List<SysRoleBriefVO>> roleMap = loadUserRoleBriefs(userIds);
        for (SysUserVO user : users) {
            user.setRoles(roleMap.getOrDefault(user.getId(), new ArrayList<>()));
        }
    }

    /** 批量组装用户的角色简要信息，避免 N+1 查询 */
    private Map<Long, List<SysRoleBriefVO>> loadUserRoleBriefs(Collection<Long> userIds) {
        Map<Long, List<SysRoleBriefVO>> result = new LinkedHashMap<>();
        List<Long> distinctUserIds = CollUtils.distinct(userIds);
        if (CollUtils.isEmpty(distinctUserIds)) {
            return result;
        }
        List<SysUserRole> relations = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().in(SysUserRole::getUserId, distinctUserIds));
        if (CollUtils.isEmpty(relations)) {
            return result;
        }
        List<Long> roleIds = new ArrayList<>();
        for (SysUserRole relation : relations) {
            roleIds.add(relation.getRoleId());
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, CollUtils.distinct(roleIds))
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
        Map<Long, SysRole> roleMap = new LinkedHashMap<>();
        for (SysRole role : roles) {
            roleMap.put(role.getId(), role);
        }
        Map<Long, List<SysRole>> userRoles = new LinkedHashMap<>();
        for (SysUserRole relation : relations) {
            SysRole role = roleMap.get(relation.getRoleId());
            if (ObjUtils.isNull(role)) {
                // 角色已被物理删除等异常数据：跳过，不影响用户列表查询
                continue;
            }
            userRoles.computeIfAbsent(relation.getUserId(), key -> new ArrayList<>()).add(role);
        }
        for (Map.Entry<Long, List<SysRole>> entry : userRoles.entrySet()) {
            result.put(entry.getKey(), sysUserConverter.toRoleBriefList(entry.getValue()));
        }
        return result;
    }

    /** 超级管理员账号判据：该用户的启用角色编码里包含 admin（与登录时的判定口径一致） */
    private boolean isSuperAdminAccount(Long userId) {
        if (ObjUtils.isNull(userId)) {
            return false;
        }
        return permissionService.getRoleCodes(userId).contains(CommonConstants.SUPER_ADMIN_ROLE_CODE);
    }

    /**
     * 可空文本字段的补丁语义：null 表示请求未提交该字段（保持原值），空白字符串表示清空该字段。
     * <p>原因：MyBatis-Plus 默认的 NOT_NULL 更新策略会忽略 null，无法把字段清空；
     * 空串能正常写入，从而让"清空邮箱/手机号/备注"这类编辑生效。</p>
     */
    private String patchText(String text) {
        return ObjUtils.isNull(text) ? null : StrUtils.trimToEmpty(text);
    }
}
