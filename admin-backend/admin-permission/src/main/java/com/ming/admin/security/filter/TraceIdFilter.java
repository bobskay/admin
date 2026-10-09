package com.ming.admin.security.filter;

import com.ming.admin.common.constant.AttributeConstants;
import com.ming.admin.common.constant.HeaderConstants;
import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.util.IdUtils;
import com.ming.admin.common.util.StrUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * traceId 过滤器：最外层过滤器，优先级高于认证过滤器。
 * <p>读请求头 x-trace-id，为空则生成 32 位无横线 UUID；写入 MDC、请求属性与响应头。
 * 响应头在 finally 里再写一次，保证 404、异常、未认证等任何分支都能拿到 traceId。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HeaderConstants.TRACE_ID);
        if (StrUtils.isBlank(traceId)) {
            traceId = IdUtils.traceId();
        } else {
            traceId = traceId.trim();
        }
        MDC.put(RedisKeyConstants.MDC_TRACE_ID, traceId);
        request.setAttribute(AttributeConstants.TRACE_ID, traceId);
        response.setHeader(HeaderConstants.TRACE_ID, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            response.setHeader(HeaderConstants.TRACE_ID, traceId);
            MDC.remove(RedisKeyConstants.MDC_TRACE_ID);
        }
    }
}
