package com.ming.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 自定义业务配置（统一前缀 admin），禁止在业务代码里散落读取配置。
 */
@Data
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /** token 过期时长，默认 2 小时 */
    private Duration tokenExpire = Duration.ofHours(2);

    /** 权限缓存时长，默认 30 分钟 */
    private Duration permCache = Duration.ofMinutes(30);

    /** 日志目录 */
    private String logDir = "logs";

    /** 慢请求阈值（毫秒） */
    private long slowRequestMillis = 1000L;

    /** 启动校验开关（默认 dev 开启、prod 关闭） */
    private boolean startupCheckEnabled = true;

    /** 接口文档开关 */
    private boolean docEnabled = true;

    /** 应用版本（用于文档与工作台展示） */
    private String version = "1.0.0";

    /** 审计日志线程池参数 */
    private Audit audit = new Audit();

    /** token 过期秒数 */
    public long getTokenExpireSeconds() {
        return tokenExpire == null || tokenExpire.getSeconds() < 1 ? 7200L : tokenExpire.getSeconds();
    }

    /** 权限缓存秒数 */
    public long getPermCacheSeconds() {
        return permCache == null || permCache.getSeconds() < 1 ? 1800L : permCache.getSeconds();
    }

    /** 审计线程池参数（有界线程池） */
    @Data
    public static class Audit {

        private int corePoolSize = 2;

        private int maxPoolSize = 8;

        private int queueCapacity = 500;

        private int keepAliveSeconds = 60;

        private String threadNamePrefix = "admin-audit-";
    }
}
