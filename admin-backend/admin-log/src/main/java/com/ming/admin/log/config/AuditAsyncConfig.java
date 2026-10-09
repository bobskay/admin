package com.ming.admin.log.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 审计日志异步线程池配置。
 * <p>线程池参数从 {@code admin.audit.*} 配置项读取：admin-log 不能依赖 admin-permission，
 * 因此这里用 @Value 逐个读取（与 AdminProperties.Audit 读的是同一批配置项与同一批默认值），
 * 而不是注入 AdminProperties。</p>
 * <p>拒绝策略：只打 WARN 日志并丢弃该条审计日志，绝不抛异常影响业务主流程（见任务书 22.3）。</p>
 * <p>@EnableAsync 只在本类声明一次，启动类与其他配置类不再重复声明。</p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AuditAsyncConfig {

    /** 审计线程池：有界队列 + 拒绝只告警 */
    @Bean("auditExecutor")
    public ThreadPoolTaskExecutor auditExecutor(
            @Value("${admin.audit.core-pool-size:2}") int corePoolSize,
            @Value("${admin.audit.max-pool-size:8}") int maxPoolSize,
            @Value("${admin.audit.queue-capacity:500}") int queueCapacity,
            @Value("${admin.audit.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${admin.audit.thread-name-prefix:admin-audit-}") String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 配置写错（最大线程小于核心线程）时兜底，避免启动直接失败
        int safeCorePoolSize = Math.max(corePoolSize, 1);
        executor.setCorePoolSize(safeCorePoolSize);
        executor.setMaxPoolSize(Math.max(maxPoolSize, safeCorePoolSize));
        executor.setQueueCapacity(Math.max(queueCapacity, 0));
        executor.setKeepAliveSeconds(Math.max(keepAliveSeconds, 0));
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler((task, pool) -> log.warn(
                "audit task rejected and dropped: poolSize={}, activeCount={}, queueSize={}, completedTaskCount={}",
                pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size(), pool.getCompletedTaskCount()));
        // 关闭时等待队列里的审计日志写完，避免丢最后几条记录
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
