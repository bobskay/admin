package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色简要信息（用户列表的角色标签、me 接口的用户角色展示共用）。
 */
@Data
public class SysRoleBriefVO {

    /** 角色主键 */
    @Schema(description = "角色 id")
    private Long id;

    /** 角色名 */
    @Schema(description = "角色名")
    private String name;

    /** 角色编码 */
    @Schema(description = "角色编码")
    private String code;
}
