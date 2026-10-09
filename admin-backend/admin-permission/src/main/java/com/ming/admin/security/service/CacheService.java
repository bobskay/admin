package com.ming.admin.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.config.AdminProperties;
import com.ming.admin.security.model.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 缓存服务：登录 token 与权限集合的 Redis 存取，以及权限版本号。
 * <p>业务代码不要注入 RedissonClient，需要失效权限缓存时只调用本服务或 {@link PermissionService#refreshPermCache()}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedissonClient redissonClient;

    private final ObjectMapper objectMapper;

    private final AdminProperties adminProperties;

    // ==================== 登录 token ====================

    /** 写入登录用户 JSON（按配置的过期时长） */
    public void saveLoginUser(String tokenId, LoginUser loginUser) {
        RBucket<String> bucket = redissonClient.getBucket(RedisKeyConstants.TOKEN_PREFIX + tokenId);
        bucket.set(writeJson(loginUser), Duration.ofSeconds(adminProperties.getTokenExpireSeconds()));
    }

    /** 读取登录用户，键不存在返回 null */
    public LoginUser getLoginUser(String tokenId) {
        RBucket<String> bucket = redissonClient.getBucket(RedisKeyConstants.TOKEN_PREFIX + tokenId);
        String json = bucket.get();
        if (StrUtils.isBlank(json)) {
            return null;
        }
        return readJson(json);
    }

    /** 删除登录 session（登出） */
    public void deleteLoginUser(String tokenId) {
        redissonClient.getBucket(RedisKeyConstants.TOKEN_PREFIX + tokenId).delete();
    }

    /** 剩余过期秒数，键不存在返回 -2，无过期返回 -1 */
    public long remainSeconds(String tokenId) {
        return redissonClient.getBucket(RedisKeyConstants.TOKEN_PREFIX + tokenId).remainTimeToLive() / 1000;
    }

    /** 重置为完整过期时长（滑动续期） */
    public void renewLoginUser(String tokenId) {
        redissonClient.getBucket(RedisKeyConstants.TOKEN_PREFIX + tokenId)
                .expire(Duration.ofSeconds(adminProperties.getTokenExpireSeconds()));
    }

    // ==================== 权限集合与版本号 ====================

    /** 当前权限版本号，键不存在时为 0 */
    public long currentPermVersion() {
        RAtomicLong version = redissonClient.getAtomicLong(RedisKeyConstants.PERM_VERSION_KEY);
        return version.get();
    }

    /** 权限版本号自增（凡是影响权限的写操作都只做这一件事，旧键 30 分钟后自然过期） */
    public long increasePermVersion() {
        return redissonClient.getAtomicLong(RedisKeyConstants.PERM_VERSION_KEY).incrementAndGet();
    }

    /** 读取权限缓存，未命中返回 null（注意：空权限集合不会产生键，属于可接受的取舍） */
    public Set<String> getPermCache(Long userId, long version) {
        RSet<String> set = redissonClient.getSet(permKey(userId, version));
        if (!set.isExists()) {
            return null;
        }
        return new LinkedHashSet<>(set.readAll());
    }

    /** 写入权限缓存（按配置的权限缓存时长过期） */
    public void setPermCache(Long userId, long version, Set<String> perms) {
        if (perms == null || perms.isEmpty()) {
            return;
        }
        RSet<String> set = redissonClient.getSet(permKey(userId, version));
        set.addAll(perms);
        set.expire(Duration.ofSeconds(adminProperties.getPermCacheSeconds()));
    }

    private String permKey(Long userId, long version) {
        return RedisKeyConstants.PERM_PREFIX + userId + ":" + version;
    }

    private String writeJson(LoginUser loginUser) {
        try {
            return objectMapper.writeValueAsString(loginUser);
        } catch (Exception e) {
            log.error("Failed to serialize login user: {}", e.getMessage());
            return null;
        }
    }

    private LoginUser readJson(String json) {
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize login user: {}", e.getMessage());
            return null;
        }
    }
}
