package com.dormrepair.common.result;

import com.github.pagehelper.PageInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public final class PageResult<T> {
    private final long total;
    private final int pageNum;
    private final int pageSize;
    private final List<T> records;

    private PageResult(long total, int pageNum, int pageSize, List<T> records) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.records = List.copyOf(records);
    }

    public static <T> PageResult<T> from(PageInfo<T> pageInfo) {
        return new PageResult<>(pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize(), pageInfo.getList());
    }

    public static <T> PageResult<T> of(long total, int pageNum, int pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records);
    }

    public long getTotal() { return total; }
    public int getPageNum() { return pageNum; }
    public int getPageSize() { return pageSize; }
    public List<T> getRecords() { return records; }

    @JsonIgnore
    public List<T> getList() { return records; }
}
