package com.ming.admin.system.controller;

import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.response.R;
import com.ming.admin.security.annotation.Menu;
import com.ming.admin.system.dto.SysOperationLogPageDTO;
import com.ming.admin.system.service.SysOperationLogQueryService;
import com.ming.admin.system.vo.SysOperationLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口。
 * <p>类级路径与方法级路径拼接后等于 5.1 推导出的地址（路径全小写）：/admin/system/syslog/page。</p>
 * <p>日志只查不改，所以本模块没有任何写接口，也就没有 @OperationLog 标注
 * （否则"查询日志"这个动作本身会不断写入审计表）。</p>
 */
@Tag(name = "操作日志")
@Validated
@RestController
@RequestMapping("/admin/system/syslog")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogQueryService sysOperationLogQueryService;

    /** 操作日志分页查询（权限点：/admin/system/syslog/page） */
    @Menu
    @Operation(summary = "操作日志分页查询")
    @PostMapping("/page")
    public R<PageResult<SysOperationLogVO>> page(@Valid @RequestBody SysOperationLogPageDTO dto) {
        return R.success(sysOperationLogQueryService.pageLogs(dto));
    }
}
