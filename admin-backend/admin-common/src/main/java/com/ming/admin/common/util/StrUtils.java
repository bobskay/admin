package com.ming.admin.common.util;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 字符串工具门面。
 * <p>业务代码统一使用本门面，不要直接引用第三方工具库。</p>
 */
public final class StrUtils {

    public static boolean isBlank(CharSequence text) {
        return StrUtil.isBlank(text);
    }

    public static boolean isNotBlank(CharSequence text) {
        return StrUtil.isNotBlank(text);
    }

    public static boolean isEmpty(CharSequence text) {
        return StrUtil.isEmpty(text);
    }

    public static boolean isNotEmpty(CharSequence text) {
        return StrUtil.isNotEmpty(text);
    }

    public static String trim(CharSequence text) {
        return StrUtil.trim(text);
    }

    public static String trimToNull(CharSequence text) {
        return StrUtil.trimToNull(text);
    }

    public static String trimToEmpty(CharSequence text) {
        return StrUtil.trimToEmpty(text);
    }

    public static boolean equals(CharSequence first, CharSequence second) {
        return StrUtil.equals(first, second);
    }

    public static boolean equalsIgnoreCase(CharSequence first, CharSequence second) {
        return StrUtil.equalsIgnoreCase(first, second);
    }

    public static String lowerCase(CharSequence text) {
        return text == null ? null : text.toString().toLowerCase();
    }

    public static String upperCase(CharSequence text) {
        return text == null ? null : text.toString().toUpperCase();
    }

    public static boolean startWith(CharSequence text, CharSequence prefix) {
        return StrUtil.startWith(text, prefix);
    }

    public static boolean endWith(CharSequence text, CharSequence suffix) {
        return StrUtil.endWith(text, suffix);
    }

    public static boolean contains(CharSequence text, CharSequence searchText) {
        return StrUtil.contains(text, searchText);
    }

    public static String replace(CharSequence text, CharSequence searchText, CharSequence replacement) {
        return StrUtil.replace(text, searchText, replacement);
    }

    /** 按分隔符切分，去除空白并丢弃空串 */
    public static List<String> split(CharSequence text, CharSequence separator) {
        if (isBlank(text)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String item : StrUtil.split(text, separator)) {
            if (isNotBlank(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    /** 用分隔符连接 */
    public static String join(CharSequence delimiter, Iterable<?> elements) {
        return StrUtil.join(delimiter, elements);
    }

    /** 截断到指定长度，超出部分丢弃 */
    public static String truncate(CharSequence text, int maxLength) {
        if (text == null) {
            return null;
        }
        String value = text.toString();
        if (maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    /** 首字母大写 */
    public static String capitalize(CharSequence text) {
        return StrUtil.upperFirst(StrUtil.nullToEmpty(text));
    }

    /** 首字母小写 */
    public static String uncapitalize(CharSequence text) {
        return StrUtil.lowerFirst(StrUtil.nullToEmpty(text));
    }

    private StrUtils() {
    }
}
