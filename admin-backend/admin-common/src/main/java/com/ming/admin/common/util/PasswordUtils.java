package com.ming.admin.common.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

/**
 * 密码工具门面：Argon2id 编码与校验。
 * <p>全项目只在这里构造唯一编码器实例，参数为 OWASP 推荐档位：
 * salt 16 字节、hash 32 字节、并行度 1、内存 19456 KiB（19 MiB）、迭代 2 次。</p>
 */
public final class PasswordUtils {

    private static final int SALT_LENGTH = 16;

    private static final int HASH_LENGTH = 32;

    private static final int PARALLELISM = 1;

    private static final int MEMORY_KIB = 19456;

    private static final int ITERATIONS = 2;

    private static final Argon2PasswordEncoder ENCODER =
            new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);

    /** 生成密码哈希 */
    public static String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return ENCODER.encode(rawPassword);
    }

    /** 校验密码，哈希为空时直接返回 false（不抛异常） */
    public static boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || StrUtils.isBlank(encodedPassword)) {
            return false;
        }
        try {
            return ENCODER.matches(rawPassword, encodedPassword);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private PasswordUtils() {
    }
}
