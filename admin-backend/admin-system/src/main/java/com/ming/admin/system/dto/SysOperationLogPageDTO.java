package com.ming.admin.system.dto;

import com.ming.admin.common.page.PageQuery;
import com.ming.admin.common.util.StrUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 操作日志分页查询入参。
 * <p>字段声明顺序与操作日志页搜索区自上而下一致（操作人、模块、动作、返回码），
 * 保证校验失败时返回的第一个字段稳定且与表单位置对得上。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperationLogPageDTO extends PageQuery {

    /** 操作人用户名，模糊匹配 */
    private String username;

    /** 模块名，模糊匹配（值来自 @OperationLog 的 module） */
    private String module;

    /** 动作名，模糊匹配（值来自 @OperationLog 的 action） */
    private String action;

    /** 返回码，精确匹配；200 为成功，其余为失败码 */
    private Integer resultCode;

    /**
     * 允许排序的前端字段名 → 数据库列名（7.5 的排序白名单）。
     * <p>Service 拼排序时只允许使用这份映射的结果，绝不把请求里的字符串直接拼进 order by。</p>
     */
    private static final Map<String, String> SORT_COLUMN_WHITELIST = Map.of(
            "id", "id",
            "resultCode", "result_code",
            "costMs", "cost_ms",
            "createTime", "create_time");

    /**
     * 解析排序字段对应的数据库列名。
     *
     * @return 白名单命中的数据库列名；字段为空、不在白名单时返回 null，由调用方回落到默认排序
     */
    public String resolveSortColumn() {
        String field = StrUtils.trimToNull(getSortField());
        return field == null ? null : SORT_COLUMN_WHITELIST.get(field);
    }
}
