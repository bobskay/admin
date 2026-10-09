package com.ming.admin.common.page;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页出参：数据列表、总条数、当前页、每页条数、总页数。
 * <p>total 是 Long，按全局规则会序列化成字符串，前端需要转成数字后再交给分页组件。</p>
 *
 * @param <T> 行数据类型
 */
@Data
public class PageResult<T> {

    /** 数据列表 */
    private List<T> list;

    /** 总条数 */
    private Long total;

    /** 当前页 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;

    /** 总页数 */
    private Long pages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list == null ? new ArrayList<>() : list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        long size = pageSize <= 0 ? 1 : pageSize;
        result.setPages((total + size - 1) / size);
        return result;
    }

    /** 由 MyBatis-Plus 分页对象构建 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 空结果 */
    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(new ArrayList<>(), 0L, pageNum, pageSize);
    }
}
