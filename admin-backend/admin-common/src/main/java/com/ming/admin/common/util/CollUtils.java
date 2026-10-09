package com.ming.admin.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 集合工具门面。
 */
public final class CollUtils {

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static <T> List<T> newArrayList() {
        return new ArrayList<>();
    }

    public static <T> List<T> toList(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        if (iterable != null) {
            iterable.forEach(result::add);
        }
        return result;
    }

    /** 去重并保持原有顺序（批量插入关联表前必须先调用） */
    public static <T> List<T> distinct(Collection<T> collection) {
        if (isEmpty(collection)) {
            return new ArrayList<>();
        }
        Set<T> set = new LinkedHashSet<>(collection);
        set.remove(null);
        return new ArrayList<>(set);
    }

    /** 取第一个元素，空集合返回 null */
    public static <T> T first(Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return collection.iterator().next();
    }

    private CollUtils() {
    }
}
