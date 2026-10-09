package com.ming.admin.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Servlet 请求上下文工具门面。
 * <p>注意：只能在同步的请求线程里使用，异步线程中不要调用。</p>
 */
public final class ServletUtils {

    /** 获取当前请求，无请求上下文返回 null */
    public static HttpServletRequest getRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    /** 读取请求头 */
    public static String getHeader(String name) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeader(name);
    }

    /** 读取请求属性 */
    public static Object getAttribute(String name) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getAttribute(name);
    }

    /** 请求地址（去掉上下文路径） */
    public static String getRequestUri() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRequestURI();
    }

    /** HTTP 方法 */
    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getMethod();
    }

    /** 客户端 IP */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String[] headers = {"x-forwarded-for", "X-Forwarded-For", "x-real-ip", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String value = request.getHeader(header);
            if (StrUtils.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
                int index = value.indexOf(',');
                return index > 0 ? value.substring(0, index).trim() : value.trim();
            }
        }
        return request.getRemoteAddr();
    }

    /** User-Agent */
    public static String getUserAgent() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }

    private ServletUtils() {
    }
}
