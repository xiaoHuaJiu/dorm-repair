package com.dormrepair.leave.controller;

import com.dormrepair.common.result.PageResult;
import com.dormrepair.common.result.Result;
import com.dormrepair.leave.dto.AdminLeaveRequestQuery;
import com.dormrepair.leave.dto.ReviewLeaveRequestDTO;
import com.dormrepair.leave.service.LeaveRequestService;
import com.dormrepair.leave.service.LeaveReviewService;
import com.dormrepair.leave.vo.LeaveRequestDetailResponse;
import com.dormrepair.leave.vo.LeaveRequestListItem;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员请假审批接口。
 */
@RestController
@RequestMapping("/api/admin/leave-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLeaveController {
    private final LeaveRequestService queries;
    private final LeaveReviewService reviews;
    public AdminLeaveController(LeaveRequestService queries, LeaveReviewService reviews) {
        this.queries = queries; this.reviews = reviews;
    }

    @GetMapping
    public Result<PageResult<LeaveRequestListItem>> page(@ModelAttribute AdminLeaveRequestQuery query) {
        return Result.success(queries.pageForAdmin(query));
    }

    @GetMapping("/{id}")
    public Result<LeaveRequestDetailResponse> detail(@PathVariable Long id) {
        return Result.success(queries.detailForAdmin(id));
    }

    @PostMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewLeaveRequestDTO request) {
        reviews.review(id, request);
        return Result.success();
    }
}
