package com.ming.admin.log.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ming.admin.common.util.DateUtils;
import com.ming.admin.log.entity.SysOperationLog;
import com.ming.admin.log.mapper.SysOperationLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志服务：异步写入 sys_operation_log。
 * <p>线程池是 admin-log 自己声明的 auditExecutor（有界、拒绝时只打 WARN），
 * 写日志失败不影响主流程，因此异常在这里吞掉并打 WARN。</p>
 */
@Slf4j
@Service
public class SysOperationLogService extends ServiceImpl<SysOperationLogMapper, SysOperationLog> {

    /**
     * 异步保存一条操作审计日志。
     * <p>本方法运行在 auditExecutor 线程里：禁止读取 ThreadLocal / 请求属性 / MDC / RequestContextHolder，
     * 需要的数据全部由调用方（同步阶段）作为参数传进来；traceId 也由参数传入，只用于日志串联。</p>
     *
     * @param operationLog 已组装好的审计日志实体（字段均来自同步阶段）
     * @param traceId      链路追踪号，可为空
     */
    @Async("auditExecutor")
    public void saveAsync(SysOperationLog operationLog, String traceId) {
        if (operationLog == null) {
            return;
        }
        try {
            // 本表不属于统一审计填充的实体（没有 fill 声明），create_time 在插入前自行赋值
            operationLog.setCreateTime(DateUtils.now());
            save(operationLog);
        } catch (Exception e) {
            log.warn("save operation log failed, traceId={}, module={}, action={}, uri={}, message={}",
                    traceId, operationLog.getModule(), operationLog.getAction(), operationLog.getUri(), e.getMessage(), e);
        }
    }
}
