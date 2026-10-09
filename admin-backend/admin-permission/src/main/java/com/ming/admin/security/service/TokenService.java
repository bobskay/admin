package com.ming.admin.security.service;

import com.ming.admin.common.constant.HeaderConstants;
import com.ming.admin.common.util.IdUtils;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.config.AdminProperties;
import com.ming.admin.security.model.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * token 服务：tokenId 生成、解析、读取、登出与滑动续期。
 * <p>同一账号允许多端同时在线，各自持有独立 tokenId，互不影响。</p>
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final CacheService cacheService;

    private final AdminProperties adminProperties;

    /**
     * 从请求头解析 tokenId：优先 authorization（兼容带 / 不带 Bearer 前缀），其次自定义头 token。
     *
     * @return tokenId，取不到返回 null
     */
    public String resolveToken(HttpServletRequest request) {
        String value = request.getHeader(HeaderConstants.AUTHORIZATION);
        if (StrUtils.isBlank(value)) {
            value = request.getHeader(HeaderConstants.TOKEN);
        }
        if (StrUtils.isBlank(value)) {
            return null;
        }
        String token = value.trim();
        if (token.toLowerCase().startsWith(HeaderConstants.BEARER_PREFIX.toLowerCase())) {
            token = token.substring(HeaderConstants.BEARER_PREFIX.length()).trim();
        }
        return StrUtils.isBlank(token) ? null : token;
    }

    /** 生成 tokenId 并写入 Redis */
    public String createToken(LoginUser loginUser) {
        String tokenId = IdUtils.tokenId();
        cacheService.saveLoginUser(tokenId, loginUser);
        return tokenId;
    }

    /** 按 tokenId 读取登录用户，失效返回 null */
    public LoginUser getLoginUser(String tokenId) {
        if (StrUtils.isBlank(tokenId)) {
            return null;
        }
        return cacheService.getLoginUser(tokenId);
    }

    /** 登出：删除 session；token 无效时也静默成功 */
    public void logout(String tokenId) {
        if (StrUtils.isNotBlank(tokenId)) {
            cacheService.deleteLoginUser(tokenId);
        }
    }

    /** 滑动续期：仅当剩余时间小于完整时长的一半时才重置 */
    public void renewIfNeeded(String tokenId) {
        long totalSeconds = adminProperties.getTokenExpireSeconds();
        long remainSeconds = cacheService.remainSeconds(tokenId);
        if (remainSeconds > 0 && remainSeconds < totalSeconds / 2) {
            cacheService.renewLoginUser(tokenId);
        }
    }

    /** token 过期秒数（登录接口出参） */
    public long getExpiresIn() {
        return adminProperties.getTokenExpireSeconds();
    }
}
