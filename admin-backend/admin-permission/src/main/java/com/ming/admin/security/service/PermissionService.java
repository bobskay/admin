package com.ming.admin.security.service;

import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.security.mapper.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限服务：用户权限标识集合、角色编码集合与权限缓存失效。
 * <p>缓存方案：键 admin:perm:{userId}:{permVersion}，另有全局键 admin:perm:version 存当前版本号；
 * 凡是影响权限的写操作只做一件事——版本号自增，旧键不再被读取，30 分钟后自然过期。</p>
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;

    private final CacheService cacheService;

    /** 取用户权限标识集合（命中缓存直接返回） */
    public Set<String> getPerms(Long userId) {
        if (userId == null) {
            return new LinkedHashSet<>();
        }
        long version = cacheService.currentPermVersion();
        Set<String> cached = cacheService.getPermCache(userId, version);
        if (cached != null) {
            return cached;
        }
        List<String> perms = permissionMapper.selectPermsByUserId(userId);
        Set<String> result = new LinkedHashSet<>(CollUtils.isEmpty(perms) ? new ArrayList<>() : perms);
        cacheService.setPermCache(userId, version, result);
        return result;
    }

    /** 取用户角色编码集合（只含启用角色） */
    public List<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<String> codes = permissionMapper.selectRoleCodesByUserId(userId);
        return CollUtils.isEmpty(codes) ? new ArrayList<>() : codes;
    }

    /** 是否超级管理员：只按角色编码里是否包含 admin 判定 */
    public boolean isSuperAdmin(List<String> roleCodes) {
        return CollUtils.isNotEmpty(roleCodes) && roleCodes.contains(CommonConstants.SUPER_ADMIN_ROLE_CODE);
    }

    /** 权限缓存失效：版本号自增，业务代码在影响权限的写操作后调用 */
    public void refreshPermCache() {
        cacheService.increasePermVersion();
    }
}
