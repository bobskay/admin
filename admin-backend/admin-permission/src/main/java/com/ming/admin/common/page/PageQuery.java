package com.ming.admin.common.page;

import com.ming.admin.common.constant.CommonConstants;
import com.ming.admin.common.constant.ValidationReasonConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页入参基类：页码、每页条数、关键字、排序字段、排序方向。
 * <p>其余业务过滤字段由各自的请求对象继承后追加。</p>
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    @Min(value = 1, message = ValidationReasonConstants.RANGE)
    private Long pageNum = CommonConstants.DEFAULT_PAGE_NUM;

    /** 每页条数，最大 100 */
    @Min(value = 1, message = ValidationReasonConstants.RANGE)
    @Max(value = 100, message = ValidationReasonConstants.RANGE)
    private Long pageSize = CommonConstants.DEFAULT_PAGE_SIZE;

    /** 关键字（各模块自行解释其匹配范围） */
    private String keyword;

    /** 排序字段，只允许取各列表声明的白名单字段 */
    private String sortField;

    /** 排序方向：asc 或 desc，其他值回落默认排序 */
    private String sortOrder;

    /** 安全的页码 */
    public long safePageNum() {
        return pageNum == null || pageNum < 1 ? CommonConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    /** 安全的每页条数（1 ~ 100） */
    public long safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return CommonConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, CommonConstants.MAX_PAGE_SIZE);
    }

    /** 排序方向是否升序 */
    public boolean isAsc() {
        return "asc".equalsIgnoreCase(sortOrder == null ? null : sortOrder.trim());
    }

    /** 排序方向是否降序 */
    public boolean isDesc() {
        return "desc".equalsIgnoreCase(sortOrder == null ? null : sortOrder.trim());
    }
}
