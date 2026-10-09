package com.ming.admin.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.MenuConstants;
import com.ming.admin.common.constant.StatusConstants;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.DateUtils;
import com.ming.admin.common.util.ObjUtils;
import com.ming.admin.common.util.PasswordUtils;
import com.ming.admin.common.util.ServletUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.common.util.TreeUtils;
import com.ming.admin.security.context.LoginUserContext;
import com.ming.admin.security.model.LoginUser;
import com.ming.admin.security.service.PermissionService;
import com.ming.admin.security.service.TokenService;
import com.ming.admin.system.convert.SysUserConverter;
import com.ming.admin.system.dto.SysLoginDTO;
import com.ming.admin.system.entity.SysMenu;
import com.ming.admin.system.entity.SysRole;
import com.ming.admin.system.entity.SysUser;
import com.ming.admin.system.entity.SysUserRole;
import com.ming.admin.system.mapper.SysMenuMapper;
import com.ming.admin.system.mapper.SysRoleMapper;
import com.ming.admin.system.mapper.SysUserMapper;
import com.ming.admin.system.mapper.SysUserRoleMapper;
import com.ming.admin.system.vo.SysCountVO;
import com.ming.admin.system.vo.SysLoginVO;
import com.ming.admin.system.vo.SysMeVO;
import com.ming.admin.system.vo.SysMenuRouteVO;
import com.ming.admin.system.vo.SysUserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 认证业务服务：登录、登出、当前登录用户信息（me）、工作台统计。
 * <p>认证模块没有对应的表，因此不继承 ServiceImpl，只用普通 @Service 组件。</p>
 */
@Service
@RequiredArgsConstructor
public class SysAuthService {

    /** 菜单排序：sort asc, id asc（空值排最后） */
    private static final Comparator<SysMenu> MENU_COMPARATOR =
            Comparator.comparing(SysMenu::getSort, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(SysMenu::getId, Comparator.nullsLast(Long::compareTo));

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final SysMenuMapper sysMenuMapper;

    private final SysUserConverter sysUserConverter;

    private final SysUserService sysUserService;

    private final TokenService tokenService;

    private final PermissionService permissionService;

    /**
     * 登录：查用户 → 校验状态 → 校验密码 → 组装登录用户 → 写 Redis → 返回 tokenId 与过期秒数。
     * <p>查不到用户与密码不匹配都返回 200015，不暴露"用户是否存在"的细节；
     * 任何分支都不返回密码与密码哈希。</p>
     */
    public SysLoginVO login(SysLoginDTO loginDTO) {
        String username = StrUtils.trim(loginDTO.getUsername());
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (ObjUtils.isNull(user)) {
            throw new BusinessException(ResultCode.BAD_CREDENTIALS);
        }
        if (StatusConstants.DISABLED.equals(user.getStatus())) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        if (!PasswordUtils.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_CREDENTIALS);
        }
        // 只取启用角色的编码，用来判定超级管理员并写入登录用户对象
        List<String> roleCodes = listEnabledRoleCodes(user.getId());

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setSuperAdmin(roleCodes.contains(CommonConstants.SUPER_ADMIN_ROLE_CODE));
        loginUser.setRoles(roleCodes);
        loginUser.setLoginTime(DateUtils.now());
        loginUser.setLoginIp(ServletUtils.getClientIp());
        loginUser.setUserAgent(ServletUtils.getUserAgent());

        String tokenId = tokenService.createToken(loginUser);
        return new SysLoginVO(tokenId, tokenService.getExpiresIn());
    }

    /**
     * 登出：删除 Redis 中的 token 键。
     * <p>登出在白名单里，必须自己校验 token；token 无效或已过期时同样返回成功，不报错。</p>
     */
    public void logout(HttpServletRequest request) {
        if (ObjUtils.isNull(request)) {
            return;
        }
        tokenService.logout(tokenService.resolveToken(request));
    }

    /**
     * 当前登录用户信息：用户基本信息、角色编码、超管标识、权限标识集合与菜单路由树。
     * <p>超管判据只看登录用户对象里的 superAdmin 布尔值，不去匹配 *:*:*。</p>
     */
    public SysMeVO me() {
        LoginUser loginUser = LoginUserContext.get();
        if (ObjUtils.isNull(loginUser) || ObjUtils.isNull(loginUser.getUserId())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = loginUser.getUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (ObjUtils.isNull(user)) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        boolean superAdmin = Boolean.TRUE.equals(loginUser.getSuperAdmin());

        SysUserInfoVO userInfo = sysUserConverter.toInfoVO(user);
        userInfo.setRoles(sysUserService.listRoleBriefs(userId));

        SysMeVO me = new SysMeVO();
        me.setUser(userInfo);
        me.setRoles(permissionService.getRoleCodes(userId));
        me.setSuperAdmin(superAdmin);
        if (superAdmin) {
            // *:*:* 只是权限数组的展示值，前端判断一律看 superAdmin
            me.setPerms(List.of(CommonConstants.SUPER_ADMIN_PERM));
        } else {
            me.setPerms(new ArrayList<>(permissionService.getPerms(userId)));
        }
        me.setMenus(buildRouteMenus(superAdmin, userId));
        return me;
    }

    /**
     * 工作台统计：只校验登录、不依赖列表接口权限。
     * <p>用户数、角色数统计未逻辑删除的记录；菜单数统计未删除且启用的菜单。</p>
     */
    public SysCountVO count() {
        SysCountVO countVO = new SysCountVO();
        countVO.setUserCount(sysUserMapper.selectCount(Wrappers.emptyWrapper()));
        countVO.setRoleCount(sysRoleMapper.selectCount(Wrappers.emptyWrapper()));
        countVO.setMenuCount(sysMenuMapper.selectCount(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, StatusConstants.ENABLED)));
        return countVO;
    }

    /**
     * 构建当前用户可见的菜单路由树（只含 type 1、2，只含启用且未删除的菜单）。
     * <p>超级管理员取全部启用目录与菜单；普通用户按角色关联查询。
     * 先按 sort asc, id asc 排序，再用 TreeUtils 内存构树（父节点缺失的孤儿节点按顶级处理）。</p>
     */
    private List<SysMenuRouteVO> buildRouteMenus(boolean superAdmin, Long userId) {
        List<SysMenu> menus;
        if (superAdmin) {
            menus = sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getType, List.of(MenuConstants.TYPE_DIRECTORY, MenuConstants.TYPE_MENU))
                    .eq(SysMenu::getStatus, StatusConstants.ENABLED)
                    .orderByAsc(SysMenu::getSort)
                    .orderByAsc(SysMenu::getId));
        } else {
            menus = sysMenuMapper.selectRouteMenusByUserId(userId);
        }
        List<SysMenu> sortedMenus = new ArrayList<>(CollUtils.isEmpty(menus) ? new ArrayList<>() : menus);
        sortedMenus.sort(MENU_COMPARATOR);

        List<SysMenuRouteVO> nodes = new ArrayList<>();
        for (SysMenu menu : sortedMenus) {
            nodes.add(toRouteVO(menu));
        }
        return TreeUtils.build(nodes, SysMenuRouteVO::getId, SysMenuRouteVO::getParentId,
                SysMenuRouteVO::setChildren, CommonConstants.ROOT_PARENT_ID);
    }

    /** 菜单实体转路由节点（只含前端渲染需要的字段） */
    private SysMenuRouteVO toRouteVO(SysMenu menu) {
        SysMenuRouteVO routeVO = new SysMenuRouteVO();
        routeVO.setId(menu.getId());
        routeVO.setParentId(menu.getParentId());
        routeVO.setName(menu.getName());
        routeVO.setType(menu.getType());
        routeVO.setPath(menu.getPath());
        routeVO.setComponent(menu.getComponent());
        routeVO.setIcon(menu.getIcon());
        routeVO.setSort(menu.getSort());
        routeVO.setVisible(menu.getVisible());
        routeVO.setKeepAlive(menu.getKeepAlive());
        routeVO.setRedirect(menu.getRedirect());
        return routeVO;
    }

    /** 用户的启用角色编码列表：sys_user_role + sys_role 组装，只取未删除且启用的角色 */
    private List<String> listEnabledRoleCodes(Long userId) {
        List<SysUserRole> relations = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = new ArrayList<>();
        for (SysUserRole relation : relations) {
            roleIds.add(relation.getRoleId());
        }
        List<Long> distinctRoleIds = CollUtils.distinct(roleIds);
        if (CollUtils.isEmpty(distinctRoleIds)) {
            return new ArrayList<>();
        }
        List<SysRole> roles = sysRoleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .in(SysRole::getId, distinctRoleIds)
                .eq(SysRole::getStatus, StatusConstants.ENABLED)
                .orderByAsc(SysRole::getSort)
                .orderByAsc(SysRole::getId));
        List<String> roleCodes = new ArrayList<>();
        for (SysRole role : roles) {
            if (StrUtils.isNotBlank(role.getCode())) {
                roleCodes.add(role.getCode());
            }
        }
        return roleCodes;
    }
}
