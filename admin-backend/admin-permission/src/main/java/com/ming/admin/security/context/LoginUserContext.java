package com.ming.admin.security.context;

import com.ming.admin.security.model.LoginUser;

/**
 * 登录用户线程上下文。
 * <p>认证过滤器在放行前写入，用 try/finally 保证请求结束后清理，避免线程池复用导致串号。</p>
 * <p>异步线程里读不到本上下文（ThreadLocal 不跨线程），需要显式透传。</p>
 */
public final class LoginUserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 当前登录用户 id，未登录返回 null */
    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    /** 当前登录用户名，未登录返回 null */
    public static String getUsername() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getUsername();
    }

    /** 当前登录用户是否为超级管理员（只认 superAdmin 布尔值） */
    public static boolean isSuperAdmin() {
        LoginUser loginUser = HOLDER.get();
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    public static void clear() {
        HOLDER.remove();
    }

    private LoginUserContext() {
    }
}
