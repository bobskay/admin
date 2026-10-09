package com.ming.admin.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 角色实体，对应表 sys_role。
 * <p>表名、主键、逻辑删除策略在 admin-permission 的 MyBatisPlusConfig 里用代码声明。</p>
 */
@Data
public class SysRole {

    /** 主键，数据库自增 */
    private Long id;

    /** 角色名 */
    private String name;

    /** 角色编码，唯一，只允许字母数字下划线冒号；内置超管角色编码为 admin */
    private String code;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用 0 禁用 */
    private Integer status;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    private Integer deleted;
}
