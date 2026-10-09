package com.ming.admin.common.util;

import com.ming.admin.common.constant.CommonConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 树构建工具：全量取出后用内存递归构建，禁止递归 SQL。
 * <p>排序由调用方在传入前完成（例如按 sort asc, id asc）。</p>
 * <p>数据自愈：父节点不存在（或父节点 id 为根值）的节点按顶级处理，绝不因为找不到父节点而抛异常。</p>
 */
public final class TreeUtils {

    /**
     * 构建树。
     *
     * @param nodes          已排好序的扁平节点
     * @param idGetter       取节点 id
     * @param parentIdGetter 取节点父 id
     * @param childrenSetter 设置子节点（无子节点时设置为空列表）
     * @param rootParentId   根节点的父 id，传 null 时使用 {@link CommonConstants#ROOT_PARENT_ID}
     */
    public static <T> List<T> build(List<T> nodes,
                                    Function<T, Long> idGetter,
                                    Function<T, Long> parentIdGetter,
                                    BiConsumer<T, List<T>> childrenSetter,
                                    Long rootParentId) {
        List<T> roots = new ArrayList<>();
        if (CollUtils.isEmpty(nodes)) {
            return roots;
        }
        Long rootId = rootParentId == null ? CommonConstants.ROOT_PARENT_ID : rootParentId;

        Set<Long> idSet = new HashSet<>();
        for (T node : nodes) {
            Long id = idGetter.apply(node);
            if (id != null) {
                idSet.add(id);
            }
        }

        Map<Long, List<T>> childrenMap = new HashMap<>();
        for (T node : nodes) {
            Long parentId = parentIdGetter.apply(node);
            if (parentId == null || parentId.equals(rootId) || !idSet.contains(parentId)) {
                roots.add(node);
            } else {
                childrenMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(node);
            }
        }

        Set<Long> visited = new HashSet<>();
        for (T node : roots) {
            attach(node, childrenMap, idGetter, childrenSetter, visited);
        }
        return roots;
    }

    private static <T> void attach(T node,
                                   Map<Long, List<T>> childrenMap,
                                   Function<T, Long> idGetter,
                                   BiConsumer<T, List<T>> childrenSetter,
                                   Set<Long> visited) {
        Long id = idGetter.apply(node);
        if (id == null || !visited.add(id)) {
            childrenSetter.accept(node, new ArrayList<>());
            return;
        }
        List<T> children = childrenMap.get(id);
        if (CollUtils.isEmpty(children)) {
            childrenSetter.accept(node, new ArrayList<>());
            return;
        }
        childrenSetter.accept(node, children);
        for (T child : children) {
            attach(child, childrenMap, idGetter, childrenSetter, visited);
        }
    }

    private TreeUtils() {
    }
}
