package com.ming.admin.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Date;

/**
 * 日期序列化：输出毫秒时间戳（数字）。
 */
public class DateToTimestampSerializer extends JsonSerializer<Date> {

    @Override
    public void serialize(Date value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
        if (value == null) {
            generator.writeNull();
        } else {
            generator.writeNumber(value.getTime());
        }
    }
}
