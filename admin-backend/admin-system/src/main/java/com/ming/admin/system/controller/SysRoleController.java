package com.ming.admin.system.controller;

import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.response.R;
import com.ming.admin.log.annotation.OperationLog;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.system.dto.SysIdDTO;
import com.ming.admin.system.dto.SysIdsDTO;
import com.ming.admin.system.dto.SysRoleAssignMenusDTO;
import com.ming.admin.system.dto.SysRoleMenuIdsDTO;
import com.ming.admin.system.dto.SysRolePageDTO;
import com.ming.admin.system.dto.SysRoleSaveDTO;
import com.ming.admin.system.dto.SysRoleUpdateDTO;
import com.ming.admin.system.service.SysRoleService;
import com.ming.admin.system.vo.SysRoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口。
 * <p>类级路径与方法级路径拼接后必须等于 5.1 推导出的地址（路径全小写）：/admin/system/sysrole/xxx。</p>
 * <p>Controller 只做参数接收、权限注解标注、调用 Service、包装返回，不写任何业务逻辑。</p>
 * <p>@Menu 标注规则（10.5）：需要鉴权的接口标 @Menu 且不传值（权限标识即本接口地址）；
 * 下拉列表、详情、回显这类被多个页面共用或依赖列表结果的接口不标注，只校验登录态。</p>
 * <p>@OperationLog 标注规则（22.3）：增删改与"状态改变"类接口都要标，审计记录异步落库。</p>
 */
@Tag(name = "角色管理")
@Validated
@RestController
@RequestMapping("/admin/system/sysrole")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /** 角色分页查询（权限点：/admin/system/sysrole/page） */
    @Menu
    @Operation(summary = "角色分页查询")
    @PostMapping("/page")
    public R<PageResult<SysRoleVO>> page(@Valid @RequestBody SysRolePageDTO dto) {
        return R.success(sysRoleService.pageRoles(dto));
    }

    /** 启用角色简要列表，供各页面的角色下拉使用（只校验登录） */
    @Operation(summary = "启用角色下拉列表")
    @PostMapping("/list")
    public R<List<SysRoleVO>> list() {
        return R.success(sysRoleService.listEnabledRoles());
    }

    /** 角色详情（只校验登录） */
    @Operation(summary = "角色详情")
    @PostMapping("/detail")
    public R<SysRoleVO> detail(@Valid @RequestBody SysIdDTO dto) {
        return R.success(sysRoleService.getRoleDetail(dto.getId()));
    }

    /** 新增角色（权限点：/admin/system/sysrole/create） */
    @Menu
    @OperationLog(module = "sysrole", action = "create")
    @Operation(summary = "新增角色")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody SysRoleSaveDTO dto) {
        return R.success(sysRoleService.createRole(dto));
    }

    /** 修改角色（权限点：/admin/system/sysrole/update） */
    @Menu
    @OperationLog(module = "sysrole", action = "update")
    @Operation(summary = "修改角色")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysRoleUpdateDTO dto) {
        sysRoleService.updateRole(dto);
        return R.success();
    }

    /** 批量删除角色（权限点：/admin/system/sysrole/delete） */
    @Menu
    @OperationLog(module = "sysrole", action = "delete")
    @Operation(summary = "批量删除角色")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody SysIdsDTO dto) {
        sysRoleService.deleteRoles(dto.getIds());
        return R.success();
    }

    /** 查询角色已关联的菜单 id，供分配权限弹窗回显（只校验登录） */
    @Operation(summary = "查询角色已关联的菜单 id")
    @PostMapping("/menuids")
    public R<List<Long>> menuids(@Valid @RequestBody SysRoleMenuIdsDTO dto) {
        return R.success(sysRoleService.selectMenuIds(dto.getRoleId()));
    }

    /** 给角色分配菜单权限（权限点：/admin/system/sysrole/assignmenus） */
    @Menu
    @OperationLog(module = "sysrole", action = "assignmenus")
    @Operation(summary = "分配菜单权限")
    @PostMapping("/assignmenus")
    public R<Void> assignMenus(@Valid @RequestBody SysRoleAssignMenusDTO dto) {
        sysRoleService.assignMenus(dto);
        return R.success();
    }
}
