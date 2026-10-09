package com.ming.admin.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.ming.admin.common.constant.CommonConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置：表名策略、主键策略、逻辑删除、分页插件全部在这里用代码声明。
 * <p>实体上不写表名 / 主键 / 逻辑删除注解；application.yml 里也不重复声明这些策略，配置只有一个来源。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /** 分页插件：单页上限 100 */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(CommonConstants.MAX_PAGE_SIZE);
        pagination.setOverflow(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }

    /** 全局策略：下划线转驼峰、不剥离表前缀、主键数据库自增、逻辑删除字段 deleted */
    @Bean
    public MybatisPlusPropertiesCustomizer adminMybatisPlusPropertiesCustomizer() {
        return properties -> {
            GlobalConfig globalConfig = properties.getGlobalConfig();
            globalConfig.setBanner(false);
            GlobalConfig.DbConfig dbConfig = globalConfig.getDbConfig();
            dbConfig.setIdType(IdType.AUTO);
            dbConfig.setTableUnderline(true);
            dbConfig.setTablePrefix(null);
            dbConfig.setLogicDeleteField("deleted");
            dbConfig.setLogicDeleteValue("1");
            dbConfig.setLogicNotDeleteValue("0");
        };
    }
}
