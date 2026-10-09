package com.ming.admin.common.response;

import lombok.Data;

/**
 * 参数校验错误对象（放在统一返回体的 data 里）。
 * <p>只有两个字段：field 与 reason。不回显任何用户提交的原值（避免密码等敏感内容外泄）。</p>
 */
@Data
public class FieldErrorItem {

    /** 出错字段名，已归一化（剥掉嵌套索引与路径），与前端表单项 prop 精确对应 */
    private String field;

    /** 校验原因，取自受控词表：required / len / pattern / email / range / type */
    private String reason;

    public static FieldErrorItem of(String field, String reason) {
        FieldErrorItem item = new FieldErrorItem();
        item.setField(field);
        item.setReason(reason);
        return item;
    }
}
