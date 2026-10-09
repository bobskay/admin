package com.ming.admin.common.response;

import lombok.Data;

/**
 * 统一返回体。
 * <p>只有 code / message / data / timestamp 四个字段：追踪号只走响应头与日志，错误详情一律装进 data。</p>
 * <p>HTTP 状态码永远是 200，业务状态放 code。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
public class R<T> {

    /** 状态码，200 表示成功 */
    private Integer code;

    /** 调试用英文短语，成功时为 null；前端不得依赖它做展示 */
    private String message;

    /** 业务数据；参数校验失败时承载校验错误对象 */
    private T data;

    /** 毫秒时间戳（出参按全局规则序列化为字符串） */
    private Long timestamp;

    /** 成功，无数据 */
    public static <T> R<T> success() {
        return build(ResultCode.SUCCESS.getCode(), null, null);
    }

    /** 成功，带数据 */
    public static <T> R<T> success(T data) {
        return build(ResultCode.SUCCESS.getCode(), null, data);
    }

    /** 失败，按状态码 */
    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败，按状态码 */
    public static <T> R<T> fail(Integer code) {
        ResultCode resultCode = ResultCode.of(code);
        return build(code, resultCode == null ? null : resultCode.getMessage(), null);
    }

    /** 失败，按状态码 + 调试消息 */
    public static <T> R<T> fail(Integer code, String message) {
        return build(code, message, null);
    }

    /** 失败，按状态码 + 调试消息 + 数据（校验失败时放校验错误对象） */
    public static <T> R<T> fail(Integer code, String message, T data) {
        return build(code, message, data);
    }

    /** 失败，按状态码 + 校验错误对象 */
    public static R<FieldErrorItem> fail(Integer code, FieldErrorItem data) {
        ResultCode resultCode = ResultCode.of(code);
        return build(code, resultCode == null ? null : resultCode.getMessage(), data);
    }

    private static <T> R<T> build(Integer code, String message, T data) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
