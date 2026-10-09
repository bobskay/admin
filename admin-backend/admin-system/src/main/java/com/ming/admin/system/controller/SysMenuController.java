package com.ming.admin.system.controller;

import com.ming.admin.common.response.R;
import com.ming.admin.log.annotation.OperationLog;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.system.dto.SysIdDTO;
import com.ming.admin.system.dto.SysIdsDTO;
import com.ming.admin.system.dto.SysMenuSaveDTO;
import com.ming.admin.system.dto.SysMenuUpdateDTO;
import com.ming.admin.system.service.SysMenuService;
import com.ming.admin.system.vo.SysMenuVO;
import com.ming.admin.system.vo.SysPermItemVO;
import com.ming.admin.system.vo.SysValidationRuleVO;
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
 * 菜单管理接口。
 * <p>类级路径与方法级路径拼接后必须等于 5.1 推导出的地址（路径全小写）：/admin/system/sysmenu/xxx。</p>
 * <p>Controller 只做参数接收、权限注解标注、调用 Service、包装返回，不写任何业务逻辑。</p>
 * <p>@Menu 标注规则（10.5）：菜单树查询、增删改标 @Menu（权限标识即本接口地址）；
 * 启用菜单树、权限标识清单、校验规则清单、详情不标注，只校验登录态。</p>
 * <p>@OperationLog 标注规则（22.3）：增删改与"状态改变"类接口都要标，审计记录异步落库。</p>
 */
@Tag(name = "菜单管理")
@Validated
@RestController
@RequestMapping("/admin/system/sysmenu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /** 菜单管理页的全量菜单树（权限点：/admin/system/sysmenu/tree） */
    @Menu
    @Operation(summary = "全量菜单树")
    @PostMapping("/tree")
    public R<List<SysMenuVO>> tree() {
        return R.success(sysMenuService.treeMenus());
    }

    /** 角色分配权限弹窗用的启用菜单树，含目录 / 菜单 / 按钮三级（只校验登录） */
    @Operation(summary = "启用菜单树")
    @PostMapping("/list")
    public R<List<SysMenuVO>> list() {
        return R.success(sysMenuService.listEnabledMenus());
    }

    /** 权限标识清单，供菜单表单的权限标识下拉使用（只校验登录） */
    @Operation(summary = "权限标识清单")
    @PostMapping("/perms")
    public R<List<SysPermItemVO>> perms() {
        return R.success(sysMenuService.listPermItems());
    }

    /** 校验规则清单，供前端开发期比对 i18n 词条（只校验登录） */
    @Operation(summary = "校验规则清单")
    @PostMapping("/rules")
    public R<List<SysValidationRuleVO>> rules() {
        return R.success(sysMenuService.listValidationRules());
    }

    /** 菜单详情（只校验登录） */
    @Operation(summary = "菜单详情")
    @PostMapping("/detail")
    public R<SysMenuVO> detail(@Valid @RequestBody SysIdDTO dto) {
        return R.success(sysMenuService.getMenuDetail(dto.getId()));
    }

    /** 新增菜单（权限点：/admin/system/sysmenu/create） */
    @Menu
    @OperationLog(module = "sysmenu", action = "create")
    @Operation(summary = "新增菜单")
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody SysMenuSaveDTO dto) {
        return R.success(sysMenuService.createMenu(dto));
    }

    /** 修改菜单（权限点：/admin/system/sysmenu/update） */
    @Menu
    @OperationLog(module = "sysmenu", action = "update")
    @Operation(summary = "修改菜单")
    @PostMapping("/update")
    public R<Void> update(@Valid @RequestBody SysMenuUpdateDTO dto) {
        sysMenuService.updateMenu(dto);
        return R.success();
    }

    /** 批量删除菜单（权限点：/admin/system/sysmenu/delete） */
    @Menu
    @OperationLog(module = "sysmenu", action = "delete")
    @Operation(summary = "批量删除菜单")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody SysIdsDTO dto) {
        sysMenuService.deleteMenus(dto.getIds());
        return R.success();
    }
}
