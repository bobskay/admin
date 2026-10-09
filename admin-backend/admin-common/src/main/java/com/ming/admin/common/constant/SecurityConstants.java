package com.ming.admin.common.constant;

import java.util.List;

/**
 * 安全相关常量：白名单路径集中在此，禁止散落字符串，禁止通配前缀放行。
 */
public final class SecurityConstants {

    /** 登录接口 */
    public static final String LOGIN_URI = "/admin/system/sysauth/login";

    /** 登出接口 */
    public static final String LOGOUT_URI = "/admin/system/sysauth/logout";

    /** 认证白名单：只允许明确列出的路径，不做通配前缀放行（禁止放行 /admin/**） */
    public static final List<String> WHITE_LIST = List.of(
            LOGIN_URI,
            LOGOUT_URI,
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/favicon.ico",
            "/error",
            "/error/**",
            "/static/**",
            "/*.css",
            "/*.js",
            "/*.png",
            "/*.jpg",
            "/*.jpeg",
            "/*.gif",
            "/*.svg",
            "/*.ico",
            "/*.woff",
            "/*.woff2",
            "/*.ttf"
    );

    private SecurityConstants() {
    }
}
