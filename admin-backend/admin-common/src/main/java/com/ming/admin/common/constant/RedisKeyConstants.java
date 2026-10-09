package com.ming.admin.common.constant;

/**
 * Redis 键前缀常量。
 * <p>业务代码不要自己拼 Redis 键名，一律通过 admin-permission 的缓存服务访问。</p>
 */
public final class RedisKeyConstants {

    /** 登录 token 键前缀：admin:token:{tokenId} */
    public static final String TOKEN_PREFIX = "admin:token:";

    /** 用户权限集合键前缀：admin:perm:{userId}:{permVersion} */
    public static final String PERM_PREFIX = "admin:perm:";

    /** 全局权限版本号键 */
    public static final String PERM_VERSION_KEY = "admin:perm:version";

    /** MDC 中 traceId 的键名 */
    public static final String MDC_TRACE_ID = "traceId";

    private RedisKeyConstants() {
    }
}
