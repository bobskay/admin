package com.ming.admin.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.util.DateUtils;
import com.ming.admin.security.context.LoginUserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 审计字段统一填充处理器：create_time、update_time、create_by、update_by。
 * <p>当前用户 id 从线程上下文取，取不到时（登录接口、异步线程、无请求上下文）写 0，不报错。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = DateUtils.now();
        Long userId = currentUserId();
        strictInsertFill(metaObject, "createTime", Date.class, now);
        strictInsertFill(metaObject, "updateTime", Date.class, now);
        strictInsertFill(metaObject, "createBy", Long.class, userId);
        strictInsertFill(metaObject, "updateBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Date now = DateUtils.now();
        Long userId = currentUserId();
        strictUpdateFill(metaObject, "updateTime", Date.class, now);
        strictUpdateFill(metaObject, "updateBy", Long.class, userId);
    }

    private Long currentUserId() {
        Long userId = LoginUserContext.getUserId();
        return userId == null ? CommonConstants.SYSTEM_USER_ID : userId;
    }
}
