package com.ming.admin.common.constant;

import java.util.List;

/**
 * 参数校验原因受控词表（前后端契约）。
 * <p>后端校验注解的 message 只能取这六个值之一，前端按 validation.{reason} 配词条。
 * 新增任何取值都属于改契约，必须同步改前端词条与启动扫描的比对逻辑。</p>
 */
public final class ValidationReasonConstants {

    /** @NotNull、@NotBlank、@NotEmpty */
    public static final String REQUIRED = "required";

    /** @Size、@Length */
    public static final String LEN = "len";

    /** @Pattern */
    public static final String PATTERN = "pattern";

    /** @Email */
    public static final String EMAIL = "email";

    /** @Min、@Max、@DecimalMin、@DecimalMax */
    public static final String RANGE = "range";

    /** 自定义类型校验（状态、菜单类型取值等） */
    public static final String TYPE = "type";

    /** 受控词表全集 */
    public static final List<String> ALL = List.of(REQUIRED, LEN, PATTERN, EMAIL, RANGE, TYPE);

    /** 判断是否为受控词表内的取值 */
    public static boolean isValid(String reason) {
        return reason != null && ALL.contains(reason);
    }

    private ValidationReasonConstants() {
    }
}
