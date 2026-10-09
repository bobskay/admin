package com.ming.admin.log.aspect;

import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.util.ServletUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.log.annotation.OperationLog;
import com.ming.admin.log.entity.SysOperationLog;
import com.ming.admin.log.service.SysOperationLogService;
import com.ming.admin.log.support.LogContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 操作审计切面：拦截带 {@link OperationLog} 注解的方法，把操作记录异步写入 sys_operation_log。
 * <p>成功与失败都记录：失败时补记异常信息；返回码优先取统一返回体的 code，异常场景取异常自带的 code。</p>
 * <p><b>上下文传递规则（硬约束）</b>：请求地址、HTTP 方法、IP、User-Agent、登录用户、traceId、耗时、返回码、
 * 脱敏后的入参，全部在<b>同步阶段</b>取出并作为参数传给 Service 的异步方法；
 * 异步方法内部禁止读 ThreadLocal / 请求属性 / MDC / RequestContextHolder。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** user_agent 列长度上限，超出先截断，避免落库报"数据过长" */
    private static final int USER_AGENT_MAX_LENGTH = 500;

    private final SysOperationLogService sysOperationLogService;

    /** 拦截带 @OperationLog 注解的方法，注解实例通过参数名 operationLog 绑定 */
    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();

        // ===== 同步阶段：一次性把异步落库需要的数据全部取出（此时仍在请求线程里） =====
        String uri = ServletUtils.getRequestUri();
        String httpMethod = ServletUtils.getMethod();
        String ip = ServletUtils.getClientIp();
        String userAgent = StrUtils.truncate(ServletUtils.getUserAgent(), USER_AGENT_MAX_LENGTH);
        String traceId = MDC.get(RedisKeyConstants.MDC_TRACE_ID);
        LogContextUtils.LoginUserSnapshot loginUser = LogContextUtils.currentLoginUser();
        String params = operationLog.recordParams()
                ? LogContextUtils.maskArgs(joinPoint.getArgs(), CommonConstants.AUDIT_PARAM_MAX_LENGTH)
                : null;

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            // 失败也要记录，记下异常后继续抛出，不影响原有异常处理流程
            error = throwable;
            throw throwable;
        } finally {
            long costMillis = System.currentTimeMillis() - start;
            SysOperationLog logEntity = new SysOperationLog();
            logEntity.setUserId(loginUser.userId());
            logEntity.setUsername(loginUser.username());
            logEntity.setModule(operationLog.module());
            logEntity.setAction(operationLog.action());
            logEntity.setMethod(httpMethod);
            logEntity.setUri(uri);
            logEntity.setIp(ip);
            logEntity.setUserAgent(userAgent);
            logEntity.setParams(params);
            logEntity.setResultCode(LogContextUtils.resolveResultCode(result, error));
            logEntity.setErrorMsg(LogContextUtils.errorMessage(error, CommonConstants.AUDIT_ERROR_MAX_LENGTH));
            logEntity.setCostMs(costMillis);
            // traceId 作为参数显式透传（MDC 不跨线程），异步方法内部只把它写进日志，不再读上下文
            sysOperationLogService.saveAsync(logEntity, traceId);
        }
    }
}
