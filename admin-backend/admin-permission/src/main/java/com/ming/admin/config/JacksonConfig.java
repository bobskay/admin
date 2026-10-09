package com.ming.admin.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.ming.admin.config.serializer.DateFromAnyDeserializer;
import com.ming.admin.config.serializer.DateToTimestampSerializer;
import com.ming.admin.config.serializer.LongFromAnyDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.TimeZone;

/**
 * 全局 Jackson 配置：日期与 Long 的序列化 / 反序列化规则集中在此，业务代码里不写任何序列化注解。
 * <ul>
 *     <li>日期 -> 毫秒时间戳（数字）</li>
 *     <li>Long / long -> 字符串（防前端精度丢失）</li>
 *     <li>日期入参：数字、yyyy-MM-dd、yyyy-MM-dd HH:mm:ss</li>
 *     <li>Long 入参：数字与字符串都接受</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer adminJacksonCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            // 日期：出参为毫秒时间戳数字，入参按约定顺序解析
            builder.serializerByType(Date.class, new DateToTimestampSerializer());
            builder.deserializerByType(Date.class, new DateFromAnyDeserializer());
            // Long：出参为字符串，入参兼容字符串与数字
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            builder.deserializerByType(Long.class, new LongFromAnyDeserializer());
            builder.deserializerByType(Long.TYPE, new LongFromAnyDeserializer());
        };
    }
}
