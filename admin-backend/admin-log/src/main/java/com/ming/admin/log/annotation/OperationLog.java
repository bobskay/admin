package com.ming.admin.log.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作审计日志注解：标注在需要落库审计的 Controller 方法上。
 * <p>只能标注在方法上，标到类或 Service 上都不会被切面拦截。</p>
 * <p>module 与 action 必填（无默认值），会被原样写入 sys_operation_log 的 module、action 两列。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 模块名，例如 sysuser */
    String module();

    /** 动作名，例如 create / update / delete / resetpassword */
    String action();

    /** 是否记录入参（脱敏并截断后写入），默认记录 */
    boolean recordParams() default true;
}
