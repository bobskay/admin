package com.ming.admin.common.constant;

/**
 * 请求头名称常量。
 * <p>总约定：所有自定义请求头一律用小写形式书写。</p>
 */
public final class HeaderConstants {

    /** 链路追踪号请求头 / 响应头 */
    public static final String TRACE_ID = "x-trace-id";

    /** 认证请求头 */
    public static final String AUTHORIZATION = "authorization";

    /** 语言请求头 */
    public static final String ACCEPT_LANGUAGE = "accept-language";

    /** 兼容的自定义 token 请求头 */
    public static final String TOKEN = "token";

    /** Bearer 前缀 */
    public static final String BEARER_PREFIX = "Bearer ";

    private HeaderConstants() {
    }
}
