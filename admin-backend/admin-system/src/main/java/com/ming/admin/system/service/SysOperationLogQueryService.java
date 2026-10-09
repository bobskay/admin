package com.ming.admin.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ming.admin.common.page.PageResult;
import com.ming.admin.common.util.StrUtils;
import com.ming.admin.log.entity.SysOperationLog;
import com.ming.admin.log.mapper.SysOperationLogMapper;
import com.ming.admin.system.dto.SysOperationLogPageDTO;
import com.ming.admin.system.vo.SysOperationLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志查询服务（只读）。
 * <p>写入侧在 admin-log 的 SysOperationLogService（异步、吞异常）；这里只做后台页面的分页查询，
 * 不复用写入服务，避免把"写日志"的异步语义带进查询链路。</p>
 * <p>sys_operation_log 没有 deleted 列，全局逻辑删除策略对它不生效（MyBatis-Plus 按实体字段判断），
 * 因此这里不需要额外排除已删除数据。</p>
 */
@Service
@RequiredArgsConstructor
public class SysOperationLogQueryService {

    private final SysOperationLogMapper sysOperationLogMapper;

    /**
     * 操作日志分页查询。
     * <p>默认按操作时间倒序（日志总是看最新的）；排序只认 DTO 的白名单列名，非法输入回落默认排序。</p>
     */
    public PageResult<SysOperationLogVO> pageLogs(SysOperationLogPageDTO query) {
        long pageNum = query.safePageNum();
        long pageSize = query.safePageSize();

        LambdaQueryWrapper<SysOperationLog> wrapper = Wrappers.lambdaQuery();
        String username = StrUtils.trimToNull(query.getUsername());
        wrapper.like(username != null, SysOperationLog::getUsername, username);
        String module = StrUtils.trimToNull(query.getModule());
        wrapper.like(module != null, SysOperationLog::getModule, module);
        String action = StrUtils.trimToNull(query.getAction());
        wrapper.like(action != null, SysOperationLog::getAction, action);
        wrapper.eq(query.getResultCode() != null, SysOperationLog::getResultCode, query.getResultCode());
        // 关键字同时匹配请求地址与操作人，方便按接口地址反查
        String keyword = StrUtils.trimToNull(query.getKeyword());
        if (keyword != null) {
            wrapper.and(inner -> inner.like(SysOperationLog::getUri, keyword)
                    .or().like(SysOperationLog::getUsername, keyword));
        }
        applySort(wrapper, query);

        Page<SysOperationLog> pageRequest = new Page<>(pageNum, pageSize);
        Page<SysOperationLog> pageResult = sysOperationLogMapper.selectPage(pageRequest, wrapper);
        List<SysOperationLogVO> voList = new ArrayList<>();
        for (SysOperationLog entity : pageResult.getRecords()) {
            voList.add(toVO(entity));
        }
        return PageResult.of(voList, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    /**
     * 拼装排序（7.5 排序字段安全）。
     * <p>列名只能来自 {@link SysOperationLogPageDTO#resolveSortColumn()} 的白名单映射结果，方向只接受 asc / desc，
     * 未命中或非法时回落默认排序 create_time desc, id desc；用户输入永远不会原样进入 SQL。</p>
     */
    private void applySort(LambdaQueryWrapper<SysOperationLog> wrapper, SysOperationLogPageDTO query) {
        String column = query.resolveSortColumn();
        if (column == null || !(query.isAsc() || query.isDesc())) {
            wrapper.orderByDesc(SysOperationLog::getCreateTime).orderByDesc(SysOperationLog::getId);
            return;
        }
        wrapper.last("ORDER BY " + column + (query.isAsc() ? " ASC" : " DESC") + ", id DESC");
    }

    /** 实体转出参（字段一一对应，不做脱敏——入库时已经处理过） */
    private SysOperationLogVO toVO(SysOperationLog entity) {
        SysOperationLogVO vo = new SysOperationLogVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setUsername(entity.getUsername());
        vo.setModule(entity.getModule());
        vo.setAction(entity.getAction());
        vo.setMethod(entity.getMethod());
        vo.setUri(entity.getUri());
        vo.setIp(entity.getIp());
        vo.setUserAgent(entity.getUserAgent());
        vo.setParams(entity.getParams());
        vo.setResultCode(entity.getResultCode());
        vo.setErrorMsg(entity.getErrorMsg());
        vo.setCostMs(entity.getCostMs());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
