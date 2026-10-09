package com.ming.admin.system.dto;

/**
 * 校验分组：新增用 Create 分组、修改用 Update 分组。
 * <p>目的：避免"新增要密码、修改不要密码"这类同一字段在不同场景校验规则冲突的问题。</p>
 * <p>两个分组都是空接口，且不继承 {@link jakarta.validation.groups.Default}：
 * 不加 groups 的字段落在 Default 分组，因此 Controller 上要同时启用 Default 与对应分组
 * （例如 {@code @Validated({Default.class, Group.Create.class})}）。</p>
 */
public interface Group {

    /** 新增场景 */
    interface Create {
    }

    /** 修改场景 */
    interface Update {
    }
}
