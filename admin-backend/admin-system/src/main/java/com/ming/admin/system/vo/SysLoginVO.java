package com.ming.admin.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录出参：只返回 tokenId 与过期秒数，绝不返回密码或密码哈希。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SysLoginVO {

    /** 32 位无横线的小写 tokenId */
    @Schema(description = "登录令牌 tokenId（32 位无横线小写字符串）")
    private String tokenId;

    /** 过期秒数 */
    @Schema(description = "令牌过期秒数")
    private Long expiresIn;
}
