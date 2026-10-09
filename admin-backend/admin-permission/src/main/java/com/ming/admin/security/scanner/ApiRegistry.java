package com.ming.admin.security.scanner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 启动扫描结果的内存注册表。
 * <p>由 admin-boot 的启动扫描组件在启动时一次性填充并缓存，接口读取时不再重新反射扫描。</p>
 * <p>本类放在 admin-permission，是为了让 admin-system 的清单接口与 admin-boot 的扫描器都能访问，
 * 同时保持依赖方向为 admin-system &lt;- admin-boot。</p>
 */
@Slf4j
@Component
public class ApiRegistry {

    private volatile List<PermissionItem> permissionItems = new ArrayList<>();

    private volatile List<ValidationRuleItem> validationRules = new ArrayList<>();

    private volatile List<String> pathViolations = new ArrayList<>();

    private volatile boolean scanned = false;

    /** 权限标识清单 */
    public List<PermissionItem> getPermissionItems() {
        return permissionItems;
    }

    /** 校验规则清单 */
    public List<ValidationRuleItem> getValidationRules() {
        return validationRules;
    }

    /** 路径命名不符合规则的接口（只警告、不阻断启动） */
    public List<String> getPathViolations() {
        return pathViolations;
    }

    /** 是否已完成扫描 */
    public boolean isScanned() {
        return scanned;
    }

    /** 写入扫描结果 */
    public void register(List<PermissionItem> permissions, List<ValidationRuleItem> rules, List<String> violations) {
        this.permissionItems = permissions == null ? new ArrayList<>() : List.copyOf(permissions);
        this.validationRules = rules == null ? new ArrayList<>() : List.copyOf(rules);
        this.pathViolations = violations == null ? new ArrayList<>() : List.copyOf(violations);
        this.scanned = true;
        log.info("API registry initialized: permissionItems={}, validationRules={}, pathViolations={}",
                this.permissionItems.size(), this.validationRules.size(), this.pathViolations.size());
    }

    /** 只读视图（防止调用方修改内部集合） */
    public List<PermissionItem> permissionItemsView() {
        return Collections.unmodifiableList(permissionItems);
    }
}
