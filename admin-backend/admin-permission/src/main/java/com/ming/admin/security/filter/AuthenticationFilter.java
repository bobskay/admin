package com.ming.admin.security.filter;

import com.ming.admin.common.constant.AttributeConstants;
import com.ming.admin.common.constant.SecurityConstants;
import com.ming.admin.common.response.ResponseWriter;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.security.context.LoginUserContext;
import com.ming.admin.security.model.LoginUser;
import com.ming.admin.security.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 认证过滤器：校验 token 并把登录用户写入线程上下文与请求属性。
 * <p>token 为空或已失效时，直接写回 HTTP 200 + code 200003 的响应体并结束本次请求。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final TokenService tokenService;

    private final ResponseWriter responseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. 跨域预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 2. 白名单直接放行（只允许明确列出的路径，不做通配前缀放行）
        if (isWhiteList(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 3. 取 token
        String tokenId = tokenService.resolveToken(request);
        if (StrUtils.isBlank(tokenId)) {
            responseWriter.write(response, ResultCode.UNAUTHORIZED);
            return;
        }
        // 4. 用 tokenId 取登录用户
        LoginUser loginUser = tokenService.getLoginUser(tokenId);
        if (loginUser == null) {
            responseWriter.write(response, ResultCode.UNAUTHORIZED);
            return;
        }
        // 5. 写入线程上下文与请求属性
        LoginUserContext.set(loginUser);
        request.setAttribute(AttributeConstants.LOGIN_USER, loginUser);
        // 6. 滑动续期
        tokenService.renewIfNeeded(tokenId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
        }
    }

    private boolean isWhiteList(String requestUri) {
        if (StrUtils.isBlank(requestUri)) {
            return false;
        }
        for (String pattern : SecurityConstants.WHITE_LIST) {
            if (PATH_MATCHER.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }
}
