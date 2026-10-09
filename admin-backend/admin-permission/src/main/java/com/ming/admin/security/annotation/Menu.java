package com.ming.admin.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解。
 * <p>只能标注在 Controller 的方法上（标到 Service 上不会生效）。</p>
 * <p>权限标识判定规则：</p>
 * <ol>
 *     <li>方法上没有本注解 -> 只校验登录态，不校验权限；</li>
 *     <li>注解 value 为空 -> 权限标识就是该接口的请求地址（默认用法，需要鉴权的接口都这么写）；</li>
 *     <li>注解 value 有值 -> 权限标识就是该值（只在两个接口共用一个权限时使用）。</li>
 * </ol>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Menu {

    /** 权限标识，留空时取接口请求地址 */
    String value() default "";
}
