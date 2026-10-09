package com.ming.admin.config.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.StrUtils;

import java.io.IOException;

/**
 * Long 反序列化：同时接受 JSON 数字与字符串（前端一律传字符串）。
 * <p>该规则同样作用于 Redis 中的登录用户对象，因此认证过滤器读取时不会失败。</p>
 */
public class LongFromAnyDeserializer extends JsonDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.asLong();
        }
        String text = node.asText();
        if (StrUtils.isBlank(text)) {
            return null;
        }
        try {
            return Long.valueOf(text.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(),
                    "Validation failed for field 'id'");
        }
    }
}
