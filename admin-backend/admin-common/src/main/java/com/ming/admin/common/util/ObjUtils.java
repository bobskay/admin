package com.ming.admin.common.util;

import cn.hutool.core.util.ObjectUtil;

/**
 * 对象工具门面。
 */
public final class ObjUtils {

    public static boolean isNull(Object object) {
        return object == null;
    }

    public static boolean isNotNull(Object object) {
        return object != null;
    }

    public static boolean isEmpty(Object object) {
        return ObjectUtil.isEmpty(object);
    }

    public static boolean isNotEmpty(Object object) {
        return ObjectUtil.isNotEmpty(object);
    }

    public static <T> T defaultIfNull(T object, T defaultValue) {
        return object == null ? defaultValue : object;
    }

    public static boolean equals(Object first, Object second) {
        return ObjectUtil.equal(first, second);
    }

    public static String toStr(Object object, String defaultValue) {
        return object == null ? defaultValue : String.valueOf(object);
    }

    public static Long toLong(Object object, Long defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        if (object instanceof Number number) {
            return number.longValue();
        }
        try {
            String text = String.valueOf(object).trim();
            return text.isEmpty() ? defaultValue : Long.valueOf(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static Integer toInt(Object object, Integer defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        if (object instanceof Number number) {
            return number.intValue();
        }
        try {
            String text = String.valueOf(object).trim();
            return text.isEmpty() ? defaultValue : Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private ObjUtils() {
    }
}
