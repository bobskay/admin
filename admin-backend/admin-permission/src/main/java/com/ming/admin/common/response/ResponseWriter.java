package com.ming.admin.common.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 统一返回体写回工具：过滤器与拦截器用它输出 HTTP 200 + JSON 响应体。
 * <p>使用全局配置的 ObjectMapper，保证 Long / 日期等字段的序列化行为与业务接口完全一致。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseWriter {

    private final ObjectMapper objectMapper;

    /** 写回统一返回体，HTTP 状态码固定 200 */
    public void write(HttpServletResponse response, R<?> body) {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            response.getWriter().write(objectMapper.writeValueAsString(body));
            response.getWriter().flush();
        } catch (IOException e) {
            log.warn("Failed to write response body: {}", e.getMessage());
        }
    }

    /** 写回指定状态码的统一返回体 */
    public void write(HttpServletResponse response, ResultCode resultCode) {
        write(response, R.fail(resultCode));
    }
}
