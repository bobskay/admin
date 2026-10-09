package com.ming.admin.log.support;

import com.ming.admin.common.constant.AttributeConstants;
import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.util.JsonUtils;
import com.ming.admin.common.util.ObjUtils;
import com.ming.admin.common.util.ServletUtils;
import com.ming.admin.common.util.StrUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 日志切面专用取数工具：脱敏、截断、登录用户快照、返回码提取。
 * <p>本类只依赖 admin-common：admin-log 不允许依赖 admin-permission（会形成循环依赖），
 * 因此这里不能引用 {@code com.ming.admin.common.response.R} 与 {@code com.ming.admin.security.model.LoginUser}，
 * 需要取它们的字段时一律用"访问器名反射"或"JSON 转 Map"两种与类型解耦的方式。</p>
 * <p>所有方法都必须在<b>同步的请求线程</b>里调用：切面必须在异步之前把数据取完，
 * 异步方法内部不允许再读 ThreadLocal / 请求属性 / MDC / RequestContextHolder。</p>
 */
public final class LogContextUtils {

    /** 请求日志入参截断长度（字符） */
    public static final int REQUEST_PARAM_MAX_LENGTH = 1000;

    /** 需要脱敏的字段名关键字：字段名包含其中之一（忽略大小写）就把它的值替换成 MASK */
    private static final String[] SENSITIVE_WORDS = {"password", "token", "secret", "authorization"};

    /**
     * 匹配 JSON 中敏感字段并替换其值的正则。
     * <p>只替换值、保留键名，日志里仍能看出是哪个字段被脱敏；值既支持字符串也支持数字 / 布尔 / null。</p>
     */
    private static final Pattern SENSITIVE_VALUE_PATTERN = Pattern.compile(
            "(\"[A-Za-z0-9_]*(?:" + String.join("|", SENSITIVE_WORDS) + ")[A-Za-z0-9_]*\"\\s*:\\s*)"
                    + "(\"(?:\\\\.|[^\"\\\\])*\"|[^,}\\]]*)",
            Pattern.CASE_INSENSITIVE);

    /** 登录用户 id 的访问器名 */
    private static final String USER_ID_GETTER = "getUserId";

    /** 登录用户名的访问器名 */
    private static final String USERNAME_GETTER = "getUsername";

    /** 返回码的访问器名 */
    private static final String CODE_GETTER = "getCode";

    /** 登录用户 JSON 中的用户 id 键名 */
    private static final String USER_ID_KEY = "userId";

    /** 登录用户 JSON 中的用户名键名 */
    private static final String USERNAME_KEY = "username";

    /**
     * 脱敏 + 截断。
     *
     * @param text      待处理文本（通常是 JSON）
     * @param maxLength 截断长度，&lt;=0 表示不截断
     */
    public static String maskAndTruncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        String masked = SENSITIVE_VALUE_PATTERN.matcher(text)
                .replaceAll("$1" + Matcher.quoteReplacement("\"" + CommonConstants.MASK + "\""));
        return StrUtils.truncate(masked, maxLength);
    }

    /**
     * 把方法入参转成"已脱敏、已截断"的字符串。
     * <p>序列化失败时（例如入参里有 Servlet 对象）只记录参数类型，绝不调用 toString()，
     * 避免对象自带的字符串形式把密码等敏感值带进日志。</p>
     */
    public static String maskArgs(Object[] args, int maxLength) {
        if (args == null || args.length == 0) {
            return null;
        }
        String json = JsonUtils.toJson(args);
        if (StrUtils.isBlank(json)) {
            String types = Arrays.stream(args)
                    .map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
                    .collect(Collectors.joining(","));
            return StrUtils.truncate(types, maxLength);
        }
        return maskAndTruncate(json, maxLength);
    }

    /**
     * 取当前登录用户快照（从请求属性 {@link AttributeConstants#LOGIN_USER} 读取）。
     * <p>取值方式一：反射调用 getUserId() / getUsername()，不引用 admin-permission 的类型；
     * 取值方式二（兜底）：把属性值转成 JSON 再按 userId / username 两个键取出。</p>
     * <p>未登录（白名单接口、登录接口本身）时返回两个字段都为 null 的快照，不抛异常。</p>
     */
    public static LoginUserSnapshot currentLoginUser() {
        Object attribute = ServletUtils.getAttribute(AttributeConstants.LOGIN_USER);
        if (attribute == null) {
            return new LoginUserSnapshot(null, null);
        }
        Long userId = toLong(invokeGetter(attribute, USER_ID_GETTER));
        String username = toText(invokeGetter(attribute, USERNAME_GETTER));
        if (userId != null || StrUtils.isNotBlank(username)) {
            return new LoginUserSnapshot(userId, username);
        }
        // 反射没取到，退化为 JSON 取值：同样是按字段名读取，不依赖具体类型
        Map<String, Object> map = JsonUtils.parseMap(JsonUtils.toJson(attribute));
        return new LoginUserSnapshot(ObjUtils.toLong(map.get(USER_ID_KEY), null),
                ObjUtils.toStr(map.get(USERNAME_KEY), null));
    }

    /**
     * 提取返回码：优先取返回体（统一返回体的 code），失败时取异常对象上的 code。
     * <p>取不到（例如系统异常本身没有 code）返回 null，按约定记 null 而不是编造状态码。</p>
     */
    public static Integer resolveResultCode(Object result, Throwable error) {
        Object source = result != null ? result : error;
        if (source == null) {
            return null;
        }
        Object code = invokeGetter(source, CODE_GETTER);
        return code instanceof Number number ? number.intValue() : null;
    }

    /** 异常信息（异常类名 + 消息），按长度截断 */
    public static String errorMessage(Throwable error, int maxLength) {
        if (error == null) {
            return null;
        }
        String message = error.getClass().getSimpleName() + ": " + StrUtils.trimToEmpty(error.getMessage());
        return StrUtils.truncate(message, maxLength);
    }

    /** 按访问器名反射取值，任何失败都返回 null（调用方自行兜底），绝不影响主流程 */
    private static Object invokeGetter(Object target, String getterName) {
        try {
            return target.getClass().getMethod(getterName).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static Long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private LogContextUtils() {
    }

    /**
     * 登录用户快照：只带异步落库需要的两个字段，不把整个用户对象传进异步线程。
     *
     * @param userId   用户 id，未登录为 null
     * @param username 用户名，未登录为 null
     */
    public record LoginUserSnapshot(Long userId, String username) {
    }
}
