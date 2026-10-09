package com.ming.admin.common.exception;

import com.ming.admin.common.response.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带状态码，由全局异常处理器转成统一返回体。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
