package com.ming.admin.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体，对应表 sys_user。
 * <p>表名、主键、逻辑删除策略全部在 admin-permission 的 MyBatisPlusConfig 里用代码声明，
 * 实体上不写表名 / 主键 / 逻辑删除注解。</p>
 * <p>审计字段上的 fill 声明是 MyBatis-Plus 自动填充的唯一触发方式（框架要求实体侧声明填充时机），
 * 填充值由 admin-permission 的 AuditMetaObjectHandler 从线程上下文取当前用户写入。</p>
 */
@Data
public class SysUser {

    /** 主键，数据库自增 */
    private Long id;

    /** 登录名，单列唯一，逻辑删除后永久不可复用 */
    private String username;

    /** 密码（Argon2id 哈希），只进不出：不进入任何 VO 与日志 */
    private String password;

    /** 显示名 */
    private String nickname;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像地址 */
    private String avatar;

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
