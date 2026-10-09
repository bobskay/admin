package com.ming.admin.common.util;

import cn.hutool.core.util.IdUtil;

/**
 * ID 工具门面：tokenId、traceId、UUID 一律从这里生成。
 */
public final class IdUtils {

    /** 32 位无横线的小写 UUID */
    public static String simpleUuid() {
        return IdUtil.fastSimpleUUID().toLowerCase();
    }

    /** 登录 tokenId：32 位无横线的小写 UUID */
    public static String tokenId() {
        return simpleUuid();
    }

    /** traceId：32 位无横线的小写 UUID */
    public static String traceId() {
        return simpleUuid();
    }

    /** 带横线的标准 UUID */
    public static String uuid() {
        return IdUtil.randomUUID();
    }

    private IdUtils() {
    }
}
