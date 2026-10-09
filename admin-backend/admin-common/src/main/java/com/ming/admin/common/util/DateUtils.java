package com.ming.admin.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期工具门面。
 * <p>全项目日期字段一律使用 {@link java.util.Date}，禁止 LocalDateTime / LocalDate / Instant。</p>
 */
public final class DateUtils {

    public static final String PATTERN_DATETIME = "yyyy-MM-dd HH:mm:ss";

    public static final String PATTERN_DATE = "yyyy-MM-dd";

    /** 按格式解析，失败返回 null */
    public static Date parse(String text, String pattern) {
        if (StrUtils.isBlank(text)) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        format.setLenient(false);
        try {
            return format.parse(text.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 按约定顺序宽松解析（用于入参日期反序列化）：
     * <ol>
     *     <li>纯数字字符串 -> 毫秒时间戳</li>
     *     <li>长度 10 -> yyyy-MM-dd</li>
     *     <li>其他 -> yyyy-MM-dd HH:mm:ss</li>
     * </ol>
     *
     * @return 解析结果，全部失败返回 null
     */
    public static Date parseAuto(String text) {
        if (StrUtils.isBlank(text)) {
            return null;
        }
        String value = text.trim();
        if (isNumeric(value)) {
            return new Date(Long.parseLong(value));
        }
        if (value.length() == 10) {
            return parse(value, PATTERN_DATE);
        }
        return parse(value, PATTERN_DATETIME);
    }

    /** 是否为纯数字字符串 */
    public static boolean isNumeric(String text) {
        if (StrUtils.isBlank(text)) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** 格式化为 yyyy-MM-dd HH:mm:ss */
    public static String format(Date date) {
        return format(date, PATTERN_DATETIME);
    }

    /** 按格式输出，空值返回 null */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(pattern).format(date);
    }

    /** 毫秒时间戳转 Date，空值返回 null */
    public static Date of(Long epochMilli) {
        return epochMilli == null ? null : new Date(epochMilli);
    }

    /** Date 转毫秒时间戳，空值返回 null */
    public static Long toEpochMilli(Date date) {
        return date == null ? null : date.getTime();
    }

    /** 当前时间 */
    public static Date now() {
        return new Date();
    }

    private DateUtils() {
    }
}
