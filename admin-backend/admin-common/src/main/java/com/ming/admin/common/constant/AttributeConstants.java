package com.ming.admin.common.constant;

/**
 * Servlet 请求属性名常量。
 * <p>认证过滤器会把登录用户对象同时写入线程上下文与该请求属性，
 * admin-log 等不允许依赖 admin-permission 的模块只能从请求属性读取。</p>
 */
public final class AttributeConstants {

    /** 当前登录用户所在的请求属性名 */
    public static final String LOGIN_USER = "admin.loginUser";

    /** 当前请求 traceId 所在的请求属性名 */
    public static final String TRACE_ID = "admin.traceId";

    private AttributeConstants() {
    }
}
