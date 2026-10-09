package com.ming.admin.security.interceptor;

import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.response.ResponseWriter;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.security.context.LoginUserContext;
import com.ming.admin.security.model.LoginUser;
import com.ming.admin.security.service.PermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Set;

/**
 * 权限拦截器：判定当前登录用户是否拥有该接口要求的权限标识。
 * <p>超级管理员直接放行（判据是登录用户对象的 superAdmin 布尔值，不匹配 *:*:*）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    private final ResponseWriter responseWriter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 非 Controller 方法（静态资源等）直接放行
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        // 2. 解析权限标识
        Menu menu = handlerMethod.getMethodAnnotation(Menu.class);
        if (menu == null) {
            return true;
        }
        String permission = StrUtils.isBlank(menu.value()) ? resolveRequestPath(request) : menu.value().trim();
        if (StrUtils.isBlank(permission)) {
            return true;
        }
        // 3. 取登录用户（认证过滤器已挡住未登录，这里是双保险）
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null) {
            responseWriter.write(response, ResultCode.UNAUTHORIZED);
            return false;
        }
        // 4. 超级管理员直接放行
        if (Boolean.TRUE.equals(loginUser.getSuperAdmin())) {
            return true;
        }
        // 5. 权限集合匹配
        Set<String> perms = permissionService.getPerms(loginUser.getUserId());
        if (perms.contains(permission)) {
            return true;
        }
        log.warn("Access denied: username={}, permission={}, uri={}, traceId={}",
                loginUser.getUsername(), permission, request.getRequestURI(), MDC.get(RedisKeyConstants.MDC_TRACE_ID));
        responseWriter.write(response, ResultCode.ACCESS_DENIED);
        return false;
    }

    /**
     * 解析请求地址：优先取 Spring MVC 的最佳匹配模板路径（类级 + 方法级映射注解的拼接结果），
     * 拿不到时回退为去掉上下文路径的请求 URI。结果一律转小写，与映射注解里书写的路径保持一致。
     */
    private String resolveRequestPath(HttpServletRequest request) {
        Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = bestMatchingPattern == null ? null : String.valueOf(bestMatchingPattern);
        if (StrUtils.isBlank(path)) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (StrUtils.isNotBlank(contextPath) && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }
        return path == null ? null : path.trim().toLowerCase();
    }
}
