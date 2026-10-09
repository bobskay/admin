package com.ming.admin.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 操作日志出参。
 * <p>params 与 errorMsg 是长文本（入库前已脱敏并截断到 2000 字符），列表页不直接铺开，
 * 只在详情弹窗里按需展示。</p>
 */
@Data
public class SysOperationLogVO {

    /** 主键 */
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

    /** 操作时间 */
    private Date createTime;
}
