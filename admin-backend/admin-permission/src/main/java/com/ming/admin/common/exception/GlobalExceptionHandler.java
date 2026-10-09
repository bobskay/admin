package com.ming.admin.common.exception;

import com.ming.admin.common.constant.RedisKeyConstants;
import com.ming.admin.common.constant.ValidationReasonConstants;
import com.ming.admin.common.response.FieldErrorItem;
import com.ming.admin.common.response.R;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.ObjUtils;
import com.ming.admin.common.util.ServletUtils;
import com.ming.admin.common.util.StrUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 全局异常处理器：HTTP 状态码永远 200，业务状态放 body 的 code。
 * <p>参数校验失败只返回第一条错误（按 DTO 字段声明顺序取稳定的第一条），详情放在 data 的校验错误对象里。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: code={}, message={}, uri={}, traceId={}",
                e.getCode(), e.getMessage(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(e.getCode(), e.getMessage());
    }

    /** 请求体参数校验失败（@Valid @RequestBody） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<FieldErrorItem> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        FieldErrorItem item = resolveFirstError(e.getBindingResult());
        log.warn("Validation failed: field={}, reason={}, uri={}, traceId={}",
                item.getField(), item.getReason(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(ResultCode.VALIDATION_FAILED.getCode(),
                "Validation failed for field '" + item.getField() + "'", item);
    }

    /** 方法参数校验失败（@Validated 场景） */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<FieldErrorItem> handleConstraintViolation(ConstraintViolationException e) {
        FieldErrorItem item = FieldErrorItem.of("", ValidationReasonConstants.REQUIRED);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        if (CollUtils.isNotEmpty(violations)) {
            ConstraintViolation<?> violation = violations.iterator().next();
            String path = String.valueOf(violation.getPropertyPath());
            item = FieldErrorItem.of(normalizeField(path), normalizeReason(violation.getMessage()));
        }
        log.warn("Validation failed: field={}, reason={}, uri={}, traceId={}",
                item.getField(), item.getReason(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(ResultCode.VALIDATION_FAILED.getCode(),
                "Validation failed for field '" + item.getField() + "'", item);
    }

    /** 请求体不可读：JSON 格式错，或内部包装了日期解析失败等业务异常 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        BusinessException businessException = findBusinessException(e);
        if (businessException != null) {
            log.warn("Request body rejected: code={}, message={}, uri={}, traceId={}",
                    businessException.getCode(), businessException.getMessage(),
                    ServletUtils.getRequestUri(), currentTraceId());
            return R.fail(businessException.getCode(), businessException.getMessage());
        }
        log.warn("Malformed request body: {}, uri={}, traceId={}",
                e.getMessage(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(ResultCode.REQUEST_BODY_INVALID);
    }

    /** 参数类型不匹配 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<FieldErrorItem> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        FieldErrorItem item = FieldErrorItem.of(normalizeField(e.getName()), ValidationReasonConstants.TYPE);
        log.warn("Argument type mismatch: field={}, uri={}, traceId={}",
                item.getField(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(ResultCode.VALIDATION_FAILED.getCode(),
                "Validation failed for field '" + item.getField() + "'", item);
    }

    /** 请求方式不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Request method not supported: {}, uri={}, traceId={}",
                e.getMethod(), ServletUtils.getRequestUri(), currentTraceId());
        return R.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    /** 请求地址不存在（未匹配到处理器） */
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("No handler found: {}, traceId={}", e.getRequestURL(), currentTraceId());
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 请求地址不存在（静态资源未找到） */
    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("No resource found: {}, traceId={}", e.getResourcePath(), currentTraceId());
        return R.fail(ResultCode.NOT_FOUND);
    }

    /** 兜底异常：记录完整堆栈，不把堆栈返回给前端 */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("Unexpected system error, uri={}, traceId={}", ServletUtils.getRequestUri(), currentTraceId(), e);
        return R.fail(ResultCode.SYSTEM_ERROR);
    }

    /**
     * 取稳定的第一个校验错误：按目标对象字段声明顺序（子类优先）匹配，
     * 匹配不到时回退到 BindingResult 的第一条。
     */
    private FieldErrorItem resolveFirstError(BindingResult bindingResult) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        if (CollUtils.isEmpty(fieldErrors)) {
            ObjectError objectError = CollUtils.first(bindingResult.getGlobalErrors());
            String field = objectError == null ? "" : objectError.getObjectName();
            String reason = objectError == null ? ValidationReasonConstants.REQUIRED
                    : normalizeReason(objectError.getDefaultMessage());
            return FieldErrorItem.of(normalizeField(field), reason);
        }
        List<String> declaredNames = declaredFieldNames(bindingResult.getTarget());
        for (String declaredName : declaredNames) {
            for (FieldError fieldError : fieldErrors) {
                if (declaredName.equals(fieldError.getField())) {
                    return FieldErrorItem.of(normalizeField(fieldError.getField()),
                            normalizeReason(fieldError.getDefaultMessage()));
                }
            }
        }
        FieldError first = fieldErrors.get(0);
        return FieldErrorItem.of(normalizeField(first.getField()), normalizeReason(first.getDefaultMessage()));
    }

    /** 字段声明顺序：先子类，再逐级父类（不含 Object） */
    private List<String> declaredFieldNames(Object target) {
        List<String> names = new ArrayList<>();
        if (target == null) {
            return names;
        }
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!names.contains(field.getName())) {
                    names.add(field.getName());
                }
            }
            type = type.getSuperclass();
        }
        return names;
    }

    /** 剥掉嵌套索引与路径，只留最后一段字段名 */
    private String normalizeField(String field) {
        if (StrUtils.isBlank(field)) {
            return field;
        }
        String name = field.trim();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex >= 0) {
            name = name.substring(dotIndex + 1);
        }
        int bracketIndex = name.indexOf('[');
        if (bracketIndex >= 0) {
            name = name.substring(0, bracketIndex);
        }
        return name;
    }

    /** 校验原因归一化：只允许受控词表内的取值，词表外的取值打 WARN 并原样返回 */
    private String normalizeReason(String reason) {
        if (StrUtils.isBlank(reason)) {
            return ValidationReasonConstants.REQUIRED;
        }
        String value = reason.trim();
        if (!ValidationReasonConstants.isValid(value)) {
            log.warn("Validation reason out of controlled vocabulary: {}", value);
        }
        return value;
    }

    /** 沿异常 cause 链查找业务异常（自定义日期反序列化器抛出的业务异常会被包装） */
    private BusinessException findBusinessException(Throwable throwable) {
        Throwable current = throwable;
        int depth = 0;
        while (ObjUtils.isNotNull(current) && depth < 10) {
            if (current instanceof BusinessException businessException) {
                return businessException;
            }
            current = current.getCause();
            depth++;
        }
        return null;
    }

    private String currentTraceId() {
        return MDC.get(RedisKeyConstants.MDC_TRACE_ID);
    }
}
