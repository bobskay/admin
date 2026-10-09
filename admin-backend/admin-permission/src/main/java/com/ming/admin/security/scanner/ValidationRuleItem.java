package com.ming.admin.security.scanner;

import lombok.Data;

/**
 * 校验规则清单条目：字段名、校验原因（受控词表值）、建议的字段标签键名。
 */
@Data
public class ValidationRuleItem {

    /** DTO 字段名 */
    private String field;

    /** 校验原因，取自受控词表 */
    private String reason;

    /** 建议的字段标签键名，即 field.{field} */
    private String labelKey;

    public static ValidationRuleItem of(String field, String reason) {
        ValidationRuleItem item = new ValidationRuleItem();
        item.setField(field);
        item.setReason(reason);
        item.setLabelKey("field." + field);
        return item;
    }
}
