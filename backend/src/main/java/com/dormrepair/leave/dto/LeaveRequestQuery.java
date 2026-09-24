package com.dormrepair.leave.dto;

import com.dormrepair.common.result.PageQuery;

/**
 * 维修人员查询本人请假列表。人员身份由登录上下文解析，前端不得指定 workerId。
 */
public class LeaveRequestQuery extends PageQuery {
    private Integer approvalStatus;
    public Integer getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(Integer approvalStatus) { this.approvalStatus = approvalStatus; }
}
