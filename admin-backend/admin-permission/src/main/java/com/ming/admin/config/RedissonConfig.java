package com.ming.admin.config;

import com.ming.admin.common.util.StrUtils;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置：连接信息由 spring.data.redis 提供，这里只做编解码器、连接池参数与空密码的兜底处理。
 * <p>数据库序号在 application-dev.yml / application-prod.yml 中隔离，避免开发清缓存误删线上 token。</p>
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonAutoConfigurationCustomizer redissonAutoConfigurationCustomizer() {
        return config -> {
            config.setCodec(new JsonJacksonCodec());
            config.setThreads(4);
            config.setNettyThreads(4);
            // 单机模式的连接池参数（地址、密码、库序号仍由 spring.data.redis 决定）
            SingleServerConfig singleServer = config.useSingleServer();
            // 密码通过 ${REDIS_PASSWORD:} 读取，未设置环境变量时是空字符串；
            // 空字符串会被 Redisson 当作"要认证"，对未开启认证的 Redis 发送 AUTH 会直接连接失败，这里清成 null。
            if (StrUtils.isEmpty(singleServer.getPassword())) {
                singleServer.setPassword(null);
            }
            singleServer.setConnectionPoolSize(32)
                    .setConnectionMinimumIdleSize(8)
                    .setTimeout(5000)
                    .setRetryAttempts(3);
        };
    }
}
