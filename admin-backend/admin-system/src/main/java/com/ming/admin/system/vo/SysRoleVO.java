package com.ming.admin.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 角色出参。
 * <p>createTime 是 java.util.Date，按全局 Jackson 规则序列化为毫秒时间戳数字；id 按 Long 规则序列化为字符串。</p>
 */
@Data
public class SysRoleVO {

    /** 主键 */
    private Long id;

    /** 角色名 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用 0 禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private Date createTime;
}
