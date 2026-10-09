package com.ming.admin.log.entity;

import lombok.Data;

import java.util.Date;

/**
 * 操作审计日志实体，对应表 sys_operation_log，字段与表列一一对应。
 * <p>表名、主键、逻辑删除策略统一在 admin-permission 的 MyBatisPlusConfig 里用代码声明，
 * 本实体上不写表名 / 主键 / 逻辑删除注解；本表不属于统一审计填充的实体，
 * create_time 由 SysOperationLogService 在插入前自行赋值。</p>
 */
@Data
public class SysOperationLog {

    /** 主键，数据库自增 */
    private Long id;

    /** 操作人 id，未登录场景为空 */
    private Long userId;

    /** 操作人用户名，未登录场景为空 */
    private String username;

    /** 模块名 */
    private String module;

    /** 动作名 */
    private String action;

    /** HTTP 方法 */
    private String method;

    /** 请求地址 */
    private String uri;

    /** 客户端 IP */
    private String ip;

    /** 浏览器标识 */
    private String userAgent;

    /** 请求参数（已脱敏、已截断） */
    private String params;

    /** 返回码 */
    private Integer resultCode;

    /** 异常信息（已截断） */
    private String errorMsg;

    /** 耗时毫秒 */
    private Long costMs;

    /** 创建时间，插入前自行赋值 */
    private Date createTime;
}
