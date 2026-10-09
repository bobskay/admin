package com.ming.admin.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ming.admin.log.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper：只用到 MyBatis-Plus 提供的基础插入能力。
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
