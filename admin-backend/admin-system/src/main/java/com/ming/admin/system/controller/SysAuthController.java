package com.ming.admin.system.controller;

import com.ming.admin.common.response.R;
import com.ming.admin.system.dto.SysLoginDTO;
import com.ming.admin.system.service.SysAuthService;
import com.ming.admin.system.vo.SysCountVO;
import com.ming.admin.system.vo.SysLoginVO;
import com.ming.admin.system.vo.SysMeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口：登录、登出、当前登录用户信息、工作台统计。
 * <p>地址前缀 /admin/system/sysauth；四个接口都只校验登录态，不标注权限注解（登录、登出在白名单里）。</p>
 * <p>Controller 只做参数接收、注解标注、调用 Service、包装统一返回体，不写业务逻辑。</p>
 */
@Tag(name = "认证")
@RestController
@RequestMapping("/admin/system/sysauth")
@RequiredArgsConstructor
public class SysAuthController {

    private final SysAuthService sysAuthService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public R<SysLoginVO> login(@RequestBody @Valid SysLoginDTO loginDTO) {
        return R.success(sysAuthService.login(loginDTO));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        sysAuthService.logout(request);
        return R.success();
    }

    @Operation(summary = "当前登录用户信息（用户、角色、权限、菜单路由树）")
    @PostMapping("/me")
    public R<SysMeVO> me() {
        return R.success(sysAuthService.me());
    }

    @Operation(summary = "工作台统计（用户数、角色数、菜单数）")
    @PostMapping("/count")
    public R<SysCountVO> count() {
        return R.success(sysAuthService.count());
    }
}
