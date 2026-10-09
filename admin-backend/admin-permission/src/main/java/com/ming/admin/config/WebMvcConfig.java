package com.ming.admin.config;

import com.ming.admin.common.constant.SecurityConstants;
import com.ming.admin.security.interceptor.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：注册权限拦截器（拦截 /admin/**，排除白名单）。
 * <p>开发环境走 Vite 代理属于同源请求，应用不配置 CORS，也不写 @CrossOrigin。</p>
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminProperties.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(SecurityConstants.WHITE_LIST.toArray(new String[0]));
    }
}
