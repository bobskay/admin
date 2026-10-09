package com.ming.admin.config.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.ming.admin.common.exception.BusinessException;
import com.ming.admin.common.response.ResultCode;
import com.ming.admin.common.util.DateUtils;
import com.ming.admin.common.util.StrUtils;

import java.io.IOException;
import java.util.Date;

/**
 * 日期反序列化：按约定顺序解析，失败抛业务异常（100009 日期格式不正确）。
 * <ol>
 *     <li>数字（或纯数字字符串）-> 毫秒时间戳</li>
 *     <li>长度 10 -> yyyy-MM-dd</li>
 *     <li>其他 -> yyyy-MM-dd HH:mm:ss</li>
 *     <li>空值 -> null</li>
 * </ol>
 */
public class DateFromAnyDeserializer extends JsonDeserializer<Date> {

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return new Date(node.asLong());
        }
        String text = node.asText();
        if (StrUtils.isBlank(text)) {
            return null;
        }
        Date date = DateUtils.parseAuto(text.trim());
        if (date == null) {
            throw new BusinessException(ResultCode.DATE_FORMAT_INVALID);
        }
        return date;
    }
}
