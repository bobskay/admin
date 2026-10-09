package com.ming.admin.system.dto;

import com.ming.admin.common.page.PageQuery;
import com.ming.admin.common.util.StrUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 角色分页查询入参。
 * <p>字段声明顺序与角色管理页搜索区自上而下一致（角色名、角色编码、状态），
 * 保证校验失败时返回的第一个字段稳定且与表单位置对得上。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRolePageDTO extends PageQuery {

    /** 角色名，模糊匹配 */
    private String name;

    /** 角色编码，模糊匹配 */
    private String code;

    /** 状态：1 启用 0 禁用，精确匹配；状态一律用 Integer 接收，禁止 Boolean / String */
    private Integer status;

    /**
     * 允许排序的前端字段名 → 数据库列名（7.5 的排序白名单）。
     * <p>Service 拼排序时只允许使用这份映射的结果，绝不把请求里的字符串直接拼进 order by。</p>
     */
    private static final Map<String, String> SORT_COLUMN_WHITELIST = Map.of(
            "id", "id",
            "name", "name",
            "code", "code",
            "sort", "sort",
            "status", "status",
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
