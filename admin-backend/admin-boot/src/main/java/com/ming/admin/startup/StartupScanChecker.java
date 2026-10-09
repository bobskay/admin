package com.ming.admin.startup;

import com.ming.admin.common.constant.ValidationReasonConstants;
import com.ming.admin.common.util.CollUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.config.AdminProperties;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.security.scanner.ApiRegistry;
import com.ming.admin.security.scanner.PermissionItem;
import com.ming.admin.security.scanner.ValidationRuleItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 启动扫描校验组件：在启动时<b>一次性</b>完成扫描，把结果写入 {@link ApiRegistry} 缓存，请求时不再重新反射扫描。
 *
 * <p>产出三份结果：</p>
 * <ol>
 *     <li>权限标识清单：带 {@link Menu} 注解的处理器方法（value 为空取接口实际路径，非空取该值）；</li>
 *     <li>校验规则清单：com.ming.admin.system.dto 下所有 DTO 字段上的 jakarta 校验注解与受控词表比对结果；</li>
 *     <li>路径命名违规清单：接口实际路径与 5.1 命名规则推导结果的差异。</li>
 * </ol>
 *
 * <p>两份清单<b>始终收集</b>（/admin/system/sysmenu/perms 与 /admin/system/sysmenu/rules 两个接口依赖它们）；
 * 是否做路径比对与数据库对账由 {@code admin.startup-check-enabled} 控制（dev 开启、prod 关闭）。
 * 所有校验只打 WARN 日志，绝不阻断启动。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupScanChecker implements ApplicationRunner {

    /** 包名前缀：按 5.1 规则去掉这个前缀后再推导类级路径 */
    private static final String ROOT_PACKAGE_PREFIX = "com.ming.";

    /** 只扫描本项目自己的处理器，避免把 Spring 内置的 /error 之类混进清单 */
    private static final String OWN_PACKAGE_PREFIX = "com.ming.admin.";

    /** 路径中要整段删除的包段 */
    private static final String CONTROLLER_PACKAGE_SEGMENT = "controller";

    /** 类名结尾要去掉的后缀 */
    private static final String CONTROLLER_CLASS_SUFFIX = "Controller";

    /** DTO 扫描包：校验规则清单只来源于这里 */
    private static final String DTO_PACKAGE = "com.ming.admin.system.dto";

    /** 数据库已有权限标识的查询语句（只取未删除且有值的行） */
    private static final String DB_PERMS_SQL =
            "select perms from sys_menu where deleted = 0 and perms is not null and perms <> ''";

    /**
     * 需要收集的校验注解全限定名。
     * <p>用注解全限定名匹配而不是 import 注解类型：扫描器不需要为 @Length 引入 hibernate-validator 的编译期依赖，
     * 同时天然兼容不同校验实现（Hibernate Validator / Jakarta 参考实现）。</p>
     */
    private static final Set<String> VALIDATION_ANNOTATION_NAMES = Set.of(
            "jakarta.validation.constraints.NotNull",
            "jakarta.validation.constraints.NotBlank",
            "jakarta.validation.constraints.NotEmpty",
            "jakarta.validation.constraints.Size",
            "jakarta.validation.constraints.Pattern",
            "jakarta.validation.constraints.Email",
            "jakarta.validation.constraints.Min",
            "jakarta.validation.constraints.Max",
            "jakarta.validation.constraints.DecimalMin",
            "jakarta.validation.constraints.DecimalMax",
            "org.hibernate.validator.constraints.Length");

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final ApiRegistry apiRegistry;

    private final AdminProperties adminProperties;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();

        // 两份清单始终收集：清单接口依赖它们，与校验开关无关
        List<PermissionItem> permissions = collectPermissions(handlerMethods);
        List<ValidationRuleItem> rules = collectValidationRules();

        List<String> violations = new ArrayList<>();
        if (adminProperties.isStartupCheckEnabled()) {
            violations.addAll(checkPathNaming(handlerMethods));
            compareWithDatabase(permissions);
        } else {
            log.info("startup check is disabled (admin.startup-check-enabled=false), skip path naming check and database reconciliation");
        }

        apiRegistry.register(permissions, rules, violations);
        log.info("startup scan finished: permissionItems={}, validationRules={}, pathViolations={}, checkEnabled={}",
                permissions.size(), rules.size(), violations.size(), adminProperties.isStartupCheckEnabled());
        for (String violation : violations) {
            log.warn("path naming violation: {}", violation);
        }
    }

    // ------------------------------------------------------------------ 权限标识清单

    /**
     * 收集权限标识清单：方法上有 @Menu 注解时，value 为空取接口实际路径，非空取该值。
     * <p>module 取类级路径最后一段（如 sysuser），action 取方法名全小写（如 resetpassword）。</p>
     */
    private List<PermissionItem> collectPermissions(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        Map<String, PermissionItem> collected = new LinkedHashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (!isOwnHandler(handlerMethod)) {
                continue;
            }
            Menu menu = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), Menu.class);
            if (menu == null) {
                // 没有 @Menu：只校验登录态，不进入权限清单
                continue;
            }
            String path = firstPath(entry.getKey());
            if (StrUtils.isBlank(path)) {
                log.warn("cannot resolve request path when collecting permission item, handler={}", handlerName(handlerMethod));
                continue;
            }
            String perm = StrUtils.isBlank(menu.value()) ? path : menu.value().trim();
            String module = classModule(handlerMethod.getBeanType());
            String action = handlerMethod.getMethod().getName().toLowerCase(Locale.ROOT);
            // 两个接口共用一个权限标识时只保留一条，清单里不出现重复项
            collected.putIfAbsent(perm, PermissionItem.of(perm, module, action));
        }
        List<PermissionItem> items = new ArrayList<>(collected.values());
        // 按权限标识排序，前端下拉选择时按模块聚集、顺序稳定
        items.sort(Comparator.comparing(PermissionItem::getPerm));
        return items;
    }

    // ------------------------------------------------------------------ 校验规则清单

    /**
     * 收集校验规则清单：遍历 DTO 包下所有类的字段，读取校验注解的 message() 作为 reason。
     * <p>message 不在受控词表内时打 WARN 列出；同一个字段有多个校验注解时按字段名去重，只保留第一条
     * （清单用于前端比对词条，字段维度去重即可）。</p>
     */
    private List<ValidationRuleItem> collectValidationRules() {
        ClassPathScanningCandidateComponentProvider provider =
                new ClassPathScanningCandidateComponentProvider(false);
        // 收全部类：DTO 上没有可用于筛选的注解，靠包路径圈定范围
        provider.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

        Map<String, ValidationRuleItem> collected = new LinkedHashMap<>();
        for (BeanDefinition definition : provider.findCandidateComponents(DTO_PACKAGE)) {
            String beanClassName = definition.getBeanClassName();
            Class<?> dtoClass = loadClass(beanClassName);
            if (dtoClass == null) {
                continue;
            }
            for (Field field : dtoClass.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String reason = resolveValidationReason(field);
                if (reason == null) {
                    continue;
                }
                if (!ValidationReasonConstants.ALL.contains(reason)) {
                    log.warn("validation message out of controlled vocabulary: dto={}, field={}, message={}, allowed={}",
                            beanClassName, field.getName(), reason, ValidationReasonConstants.ALL);
                }
                collected.putIfAbsent(field.getName(), ValidationRuleItem.of(field.getName(), reason));
            }
        }
        return new ArrayList<>(collected.values());
    }

    /** 取字段上第一个校验注解的 message 作为 reason，没有校验注解返回 null */
    private String resolveValidationReason(Field field) {
        for (Annotation annotation : field.getAnnotations()) {
            if (!VALIDATION_ANNOTATION_NAMES.contains(annotation.annotationType().getName())) {
                continue;
            }
            String message = readAnnotationMessage(annotation);
            if (StrUtils.isNotBlank(message)) {
                return message.trim();
            }
        }
        return null;
    }

    /** 反射读注解的 message()：所有校验注解都有这个属性，不用为每种注解写一份分支 */
    private String readAnnotationMessage(Annotation annotation) {
        try {
            Method messageMethod = annotation.annotationType().getMethod("message");
            Object value = messageMethod.invoke(annotation);
            return value == null ? null : String.valueOf(value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private Class<?> loadClass(String className) {
        if (StrUtils.isBlank(className)) {
            return null;
        }
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (Throwable e) {
            log.warn("load dto class failed, className={}, message={}", className, e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------ 路径命名校验

    /**
     * 路径命名校验：把每个处理器方法的实际路径与 5.1 规则推导出的期望路径逐条比对。
     * <p>接口路径必须全小写，且由类级路径 + 方法级路径拼接而来，两者不一致、或路径中出现大写字母都算违规。</p>
     */
    private List<String> checkPathNaming(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        Set<String> violations = new LinkedHashSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (!isOwnHandler(handlerMethod)) {
                continue;
            }
            String handler = handlerName(handlerMethod);
            Set<String> patterns = patternsOf(entry.getKey());
            if (CollUtils.isEmpty(patterns)) {
                violations.add("未解析到请求路径, handler=" + handler);
                continue;
            }
            String expectedPath = expectedPath(handlerMethod);
            for (String pattern : patterns) {
                if (StrUtils.isBlank(pattern)) {
                    continue;
                }
                if (!pattern.equals(pattern.toLowerCase(Locale.ROOT))) {
                    violations.add("路径中出现大写字母, 实际=" + pattern + ", 期望=" + expectedPath + ", handler=" + handler);
                }
                if (!pattern.equals(expectedPath)) {
                    violations.add("路径与命名规则推导结果不一致, 实际=" + pattern + ", 期望=" + expectedPath + ", handler=" + handler);
                }
            }
        }
        return new ArrayList<>(violations);
    }

    /** 按 5.1 规则推导接口期望路径：类级路径 + "/" + 方法名全小写 */
    private String expectedPath(HandlerMethod handlerMethod) {
        return expectedClassPath(handlerMethod.getBeanType()) + "/"
                + handlerMethod.getMethod().getName().toLowerCase(Locale.ROOT);
    }

    /**
     * 按 5.1 规则推导类级路径：
     * 全限定名去 com.ming. → 去掉 controller 包段 → 去掉类名结尾的 Controller → 整段小写、"." 换成 "/" → 前面补 "/"。
     */
    private String expectedClassPath(Class<?> controllerType) {
        String className = controllerType.getName();
        if (className.startsWith(ROOT_PACKAGE_PREFIX)) {
            className = className.substring(ROOT_PACKAGE_PREFIX.length());
        }
        List<String> segments = StrUtils.split(className, ".");
        segments.removeIf(CONTROLLER_PACKAGE_SEGMENT::equals);
        if (!segments.isEmpty()) {
            int lastIndex = segments.size() - 1;
            String simpleName = segments.get(lastIndex);
            if (simpleName.endsWith(CONTROLLER_CLASS_SUFFIX)) {
                simpleName = simpleName.substring(0, simpleName.length() - CONTROLLER_CLASS_SUFFIX.length());
            }
            segments.set(lastIndex, simpleName);
        }
        return "/" + StrUtils.join("/", segments).toLowerCase(Locale.ROOT);
    }

    /** 权限清单里的模块名：类级路径最后一段，例如 /admin/system/sysuser → sysuser */
    private String classModule(Class<?> controllerType) {
        String classPath = expectedClassPath(controllerType);
        int index = classPath.lastIndexOf('/');
        return index < 0 ? classPath : classPath.substring(index + 1);
    }

    // ------------------------------------------------------------------ 数据库对账

    /**
     * 与数据库对账：数据库里有、代码里没有的权限标识打 WARN 列出（只提示、不阻断）。
     * <p>对账只是辅助手段，数据库不可用或表不存在时只告警，不影响启动。</p>
     */
    private void compareWithDatabase(List<PermissionItem> permissions) {
        Set<String> codePerms = new LinkedHashSet<>();
        for (PermissionItem item : permissions) {
            codePerms.add(item.getPerm());
        }
        List<String> dbPerms;
        try {
            dbPerms = jdbcTemplate.queryForList(DB_PERMS_SQL, String.class);
        } catch (Exception e) {
            log.warn("skip permission reconciliation, query sys_menu failed: {}", e.getMessage());
            return;
        }
        Set<String> dbOnlyPerms = new LinkedHashSet<>();
        for (String perm : dbPerms) {
            String value = StrUtils.trimToNull(perm);
            if (value != null && !codePerms.contains(value)) {
                dbOnlyPerms.add(value);
            }
        }
        if (CollUtils.isEmpty(dbOnlyPerms)) {
            log.info("permission reconciliation passed: sys_menu perms={}, code perms={}", dbPerms.size(), codePerms.size());
            return;
        }
        log.warn("permission reconciliation found {} identifier(s) existing in sys_menu but missing in code (接口地址可能被改过):", dbOnlyPerms.size());
        for (String perm : dbOnlyPerms) {
            log.warn("  db-only perm: {}", perm);
        }
    }

    // ------------------------------------------------------------------ 公共取数

    /** 只处理本项目自己的处理器方法 */
    private boolean isOwnHandler(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getName().startsWith(OWN_PACKAGE_PREFIX);
    }

    /** 取映射的全部路径模板（Spring 6 用 PathPatternsCondition，旧写法退回 PatternsCondition） */
    private Set<String> patternsOf(RequestMappingInfo mappingInfo) {
        Set<String> patterns = new LinkedHashSet<>();
        if (mappingInfo.getPathPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPathPatternsCondition().getPatternValues());
        } else if (mappingInfo.getPatternsCondition() != null) {
            patterns.addAll(mappingInfo.getPatternsCondition().getPatterns());
        }
        return patterns;
    }

    /** 取第一个路径模板（业务接口一律只有一个地址） */
    private String firstPath(RequestMappingInfo mappingInfo) {
        return CollUtils.first(patternsOf(mappingInfo));
    }

    /** 处理器完整标识：包名.类名#方法名 */
    private String handlerName(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().getName() + "#" + handlerMethod.getMethod().getName();
    }
}
