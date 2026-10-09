package com.ming.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端启动类。
 * <p>位于根包 com.ming.admin，组件扫描天然覆盖 admin-log、admin-doc、admin-permission、admin-system 全部模块。</p>
 * <p>Mapper 扫描只需在这里声明一次：通配包路径 com.ming.admin.**.mapper 覆盖各模块的 mapper 包。</p>
 * <p>@EnableAsync 只在 admin-log 的 AuditAsyncConfig 上声明一次，这里不重复声明。</p>
 */
@SpringBootApplication
@MapperScan("com.ming.admin.**.mapper")
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
