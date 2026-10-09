package com.ming.admin.log.aspect;

import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.util.ServletUtils;
import com.ming.admin.log.support.LogContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 请求日志切面：拦截所有 Controller 方法，把一次请求的关键信息打进日志。
 * <p>只写日志，<b>不写数据库</b>（落库审计走 {@link OperationLogAspect}）。</p>
 * <p>慢请求阈值从 {@code admin.slow-request-millis} 读取：admin-log 不能依赖 admin-permission，
 * 因此这里用 @Value 读同一个配置项，而不是注入 AdminProperties。</p>
 */
@Slf4j
@Aspect
@Component
public class RequestLogAspect {

    /** 慢请求阈值（毫秒），不配置时默认 1000 */
    @Value("${admin.slow-request-millis:1000}")
    private long slowRequestMillis = 1000L;

    /**
     * 拦截所有被 @RestController 标注的类（本项目全部 Controller 都是 @RestController）。
     * <p>用 finally 统一收口输出：正常、慢请求、异常三种情况各只打一条日志，不重复。</p>
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            // 记下异常用于日志，然后继续抛出，交给全局异常处理器统一转成返回体
            error = throwable;
            throw throwable;
        } finally {
            writeLog(joinPoint, result, error, System.currentTimeMillis() - start);
        }
    }

    /** 输出一条请求日志：异常 ERROR、慢请求 WARN、其余 INFO */
    private void writeLog(ProceedingJoinPoint joinPoint, Object result, Throwable error, long costMillis) {
        try {
            String uri = ServletUtils.getRequestUri();
            String httpMethod = ServletUtils.getMethod();
            String handler = handlerName(joinPoint);
            String username = LogContextUtils.currentLoginUser().username();
            String traceId = MDC.get(RedisKeyConstants.MDC_TRACE_ID);
            Integer resultCode = LogContextUtils.resolveResultCode(result, error);
            String params = LogContextUtils.maskArgs(joinPoint.getArgs(), LogContextUtils.REQUEST_PARAM_MAX_LENGTH);
            if (error != null) {
                log.error("request error uri={} method={} handler={} user={} code={} cost={}ms traceId={} params={} message={}",
                        uri, httpMethod, handler, username, resultCode, costMillis, traceId, params,
                        LogContextUtils.errorMessage(error, LogContextUtils.REQUEST_PARAM_MAX_LENGTH), error);
            } else if (costMillis > slowRequestMillis) {
                log.warn("slow request uri={} method={} handler={} user={} code={} cost={}ms threshold={}ms traceId={} params={}",
                        uri, httpMethod, handler, username, resultCode, costMillis, slowRequestMillis, traceId, params);
            } else {
                log.info("request uri={} method={} handler={} user={} code={} cost={}ms traceId={} params={}",
                        uri, httpMethod, handler, username, resultCode, costMillis, traceId, params);
            }
        } catch (RuntimeException e) {
            // 日志切面自身出错绝不能影响业务请求
            log.warn("write request log failed, handler={}, message={}", handlerName(joinPoint), e.getMessage());
        }
    }

    /** 控制器方法全名：包名.类名#方法名 */
    private String handlerName(ProceedingJoinPoint joinPoint) {
        Signature signature = joinPoint.getSignature();
        return signature.getDeclaringTypeName() + "#" + signature.getName();
    }
}
