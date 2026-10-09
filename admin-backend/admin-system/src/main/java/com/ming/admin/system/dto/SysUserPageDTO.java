package com.ming.admin.system.dto;

import com.ming.admin.common.constant.ValidationReasonConstants;
import com.ming.admin.common.page.PageQuery;
import com.ming.admin.common.util.StrUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 用户分页查询入参：分页字段 + 用户名、昵称、状态、角色。
 * <p>字段声明顺序与前端查询表单从上到下一致。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPageDTO extends PageQuery {

    /**
     * 允许排序的字段白名单：前端字段名 → 数据库列名。
     * <p>Service 拼排序时只使用映射后的列名，严禁把请求里的字符串直接传给 order by。</p>
     */
    public static final Map<String, String> SORT_FIELD_WHITELIST = Map.of(
            "id", "id",
            "username", "username",
            "nickname", "nickname",
            "status", "status",
            "createTime", "create_time");

    /** 用户名，模糊匹配 */
    @Schema(description = "用户名（模糊匹配）")
    private String username;

    /** 昵称，模糊匹配 */
    @Schema(description = "昵称（模糊匹配）")
    private String nickname;

    /** 状态是可选过滤条件，因此只做取值校验，不加 @NotNull（不传表示全部） */
    @Schema(description = "状态：1 启用 0 禁用；不传表示全部")
    @Min(value = 0, message = ValidationReasonConstants.RANGE)
    @Max(value = 1, message = ValidationReasonConstants.RANGE)
    private Integer status;

    /** 角色过滤：先按角色查用户 id 列表再 in 过滤 */
    @Schema(description = "角色 id（按角色过滤用户）")
    private Long roleId;

    /**
     * 排序字段解析：命中白名单返回映射后的数据库列名，未命中返回 null（由调用方回落默认排序）。
     */
    public String resolveSortColumn() {
        if (StrUtils.isBlank(getSortField())) {
            return null;
        }
        return SORT_FIELD_WHITELIST.get(StrUtils.trim(getSortField()));
    }
}
