package com.dormrepair.common.result;

public class PageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum == null || pageNum < 1 ? 1 : pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) this.pageSize = 10;
        else this.pageSize = Math.min(pageSize, 100);
    }
}
