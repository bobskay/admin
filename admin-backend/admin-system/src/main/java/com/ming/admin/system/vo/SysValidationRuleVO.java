package com.ming.admin.system.vo;

import lombok.Data;

/**
 * 校验规则清单条目出参（来自启动扫描结果，供前端开发期比对 i18n 词条）。
 */
@Data
public class SysValidationRuleVO {

    /** DTO 字段名 */
    private String field;

    /** 校验原因，取自 7.2 受控词表 */
    private String reason;

    /** 建议的字段标签键名，即 field.{field} */
    private String labelKey;
}
