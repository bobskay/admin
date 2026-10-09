package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作台统计出参：走只校验登录的专用接口，不依赖列表接口权限。
 */
@Data
public class SysCountVO {

    /** 未删除的用户数 */
    @Schema(description = "用户总数")
    private Long userCount;

    /** 未删除的角色数 */
    @Schema(description = "角色总数")
    private Long roleCount;

    /** 未删除且启用的菜单数 */
    @Schema(description = "菜单总数")
    private Long menuCount;
}
