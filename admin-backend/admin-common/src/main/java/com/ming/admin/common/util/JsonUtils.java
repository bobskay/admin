package com.ming.admin.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JSON 工具门面。
 * <p>只对外暴露 String / Map / List / 实体类型，不把 Jackson 的类型泄漏到业务代码。</p>
 */
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /** 对象转 JSON 字符串，失败返回 null */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串转对象，失败返回 null */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StrUtils.isBlank(json)) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON 字符串转 Map，失败返回空 Map */
    public static Map<String, Object> parseMap(String json) {
        if (StrUtils.isBlank(json)) {
            return Collections.emptyMap();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /** JSON 字符串转 List，失败返回空 List */
    public static <T> List<T> parseList(String json, Class<T> clazz) {
        if (StrUtils.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private JsonUtils() {
    }
}
