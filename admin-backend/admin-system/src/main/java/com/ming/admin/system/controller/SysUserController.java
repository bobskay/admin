package com.ming.admin.system.controller;

import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.response.R;
import com.ming.admin.log.annotation.OperationLog;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.system.dto.Group;
import com.ming.admin.system.dto.SysIdDTO;
import com.ming.admin.system.dto.SysIdsDTO;
import com.ming.admin.system.dto.SysUserAssignRolesDTO;
import com.ming.admin.system.dto.SysUserChangeStatusDTO;
import com.ming.admin.system.dto.SysUserCreateDTO;
import com.ming.admin.system.dto.SysUserPageDTO;
import com.ming.admin.system.dto.SysUserResetPasswordDTO;
import com.ming.admin.system.dto.SysUserUpdateDTO;
import com.ming.admin.system.service.SysUserService;
import com.ming.admin.system.vo.SysUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理接口（地址前缀 /admin/system/sysuser，路径一律全小写）。
 * <p>权限注解按任务书 13.2：page、create、update、delete、resetpassword、changestatus、assignroles 需要鉴权
 * （权限标识默认等于接口地址，因此 @Menu 不传值）；detail 只校验登录。</p>
 * <p>Controller 只做参数接收、注解标注、调用 Service、包装统一返回体，不写业务逻辑。</p>
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/admin/system/sysuser")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Menu
    @Operation(summary = "用户分页列表")
    @PostMapping("/page")
    public R<PageResult<SysUserVO>> page(@RequestBody @Valid SysUserPageDTO pageDTO) {
        return R.success(sysUserService.page(pageDTO));
    }

    @Operation(summary = "用户详情")
    @PostMapping("/detail")
    public R<SysUserVO> detail(@RequestBody @Valid SysIdDTO idDTO) {
        return R.success(sysUserService.detail(idDTO.getId()));
    }

    @Menu
    @OperationLog(module = "sysuser", action = "create")
    @Operation(summary = "新增用户")
    @PostMapping("/create")
    public R<Long> create(@RequestBody @Validated({Default.class, Group.Create.class}) SysUserCreateDTO createDTO) {
        return R.success(sysUserService.create(createDTO));
    }

    @Menu
    @OperationLog(module = "sysuser", action = "update")
    @Operation(summary = "修改用户")
    @PostMapping("/update")
    public R<Void> update(@RequestBody @Validated({Default.class, Group.Update.class}) SysUserUpdateDTO updateDTO) {
        sysUserService.update(updateDTO);
        return R.success();
    }

    @Menu
    @OperationLog(module = "sysuser", action = "delete")
    @Operation(summary = "删除用户（批量，逻辑删除）")
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody @Valid SysIdsDTO idsDTO) {
        sysUserService.delete(idsDTO.getIds());
        return R.success();
    }

    @Menu
    @OperationLog(module = "sysuser", action = "resetpassword")
    @Operation(summary = "重置密码")
    @PostMapping("/resetpassword")
    public R<Void> resetPassword(@RequestBody @Valid SysUserResetPasswordDTO resetPasswordDTO) {
        sysUserService.resetPassword(resetPasswordDTO);
        return R.success();
    }

    @Menu
    @OperationLog(module = "sysuser", action = "changestatus")
    @Operation(summary = "启用 / 禁用用户")
    @PostMapping("/changestatus")
    public R<Void> changeStatus(@RequestBody @Valid SysUserChangeStatusDTO changeStatusDTO) {
        sysUserService.changeStatus(changeStatusDTO);
        return R.success();
    }

    @Menu
    @OperationLog(module = "sysuser", action = "assignroles")
    @Operation(summary = "分配角色")
    @PostMapping("/assignroles")
    public R<Void> assignRoles(@RequestBody @Valid SysUserAssignRolesDTO assignRolesDTO) {
        sysUserService.assignRoles(assignRolesDTO);
        return R.success();
    }
}
