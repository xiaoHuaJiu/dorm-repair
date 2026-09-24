package com.dormrepair.leave.controller;

import com.dormrepair.common.result.PageResult;
import com.dormrepair.common.result.Result;
import com.dormrepair.leave.dto.CreateLeaveRequestDTO;
import com.dormrepair.leave.dto.LeaveRequestQuery;
import com.dormrepair.leave.service.LeaveRequestService;
import com.dormrepair.leave.vo.CreateLeaveRequestResponse;
import com.dormrepair.leave.vo.LeaveRequestDetailResponse;
import com.dormrepair.leave.vo.LeaveRequestListItem;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 维修人员请假接口。申请人与查询范围均取自登录上下文，前端不得指定 workerId。
 */
@RestController
@RequestMapping("/api/worker/leave-requests")
@PreAuthorize("hasRole('WORKER')")
public class WorkerLeaveController {
    private final LeaveRequestService service;
    public WorkerLeaveController(LeaveRequestService service) { this.service = service; }

    @PostMapping
    public Result<CreateLeaveRequestResponse> create(@Valid @RequestBody CreateLeaveRequestDTO request) {
        return Result.success(service.create(request));
    }

    @GetMapping
    public Result<PageResult<LeaveRequestListItem>> page(@ModelAttribute LeaveRequestQuery query) {
        return Result.success(service.pageForWorker(query));
    }

    @GetMapping("/{id}")
    public Result<LeaveRequestDetailResponse> detail(@PathVariable Long id) {
        return Result.success(service.detailForWorker(id));
    }
}
