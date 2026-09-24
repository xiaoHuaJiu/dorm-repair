package com.dormrepair.order.controller;

import com.dormrepair.common.result.*;
import com.dormrepair.order.dto.*;
import com.dormrepair.order.service.*;
import com.dormrepair.order.vo.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worker/repair-orders")
@PreAuthorize("hasRole('WORKER')")
public class WorkerRepairOrderController {
    private final RepairOrderQueryService queries;
    private final RepairOrderDetailService details;
    private final WorkerRepairOrderCommandService commands;
    public WorkerRepairOrderController(RepairOrderQueryService queries, RepairOrderDetailService details, WorkerRepairOrderCommandService commands) {
        this.queries = queries; this.details = details; this.commands = commands;
    }
    @GetMapping public Result<PageResult<RepairOrderListItem>> page(@ModelAttribute RepairOrderQueryRequest q) { q.setWorkerId(null); return Result.success(queries.page(q)); }
    @GetMapping("/{id}") public Result<RepairOrderDetailResponse> detail(@PathVariable Long id) { return Result.success(details.getDetail(id)); }
    @PostMapping("/{id}/accept") public Result<Void> accept(@PathVariable Long id, @Valid @RequestBody(required=false) AcceptRepairOrderRequest request) { commands.accept(id, request); return Result.success(); }
    @PostMapping("/{id}/process-records") public Result<Void> addProcess(@PathVariable Long id, @Valid @RequestBody AddRepairProcessRequest request) { commands.addProcess(id, request); return Result.success(); }
    @PostMapping("/{id}/materials") public Result<Void> addMaterial(@PathVariable Long id, @Valid @RequestBody AddMaterialUsageRequest request) { commands.addMaterial(id, request); return Result.success(); }
    @PostMapping("/{id}/interrupt") public Result<Void> interrupt(@PathVariable Long id, @Valid @RequestBody InterruptRepairRequest request) { commands.interrupt(id, request); return Result.success(); }
    @PostMapping("/{id}/resume") public Result<Void> resume(@PathVariable Long id, @Valid @RequestBody(required=false) ResumeRepairRequest request) { commands.resume(id, request); return Result.success(); }
    @PostMapping("/{id}/submit-result") public Result<Void> submitResult(@PathVariable Long id, @Valid @RequestBody SubmitRepairResultRequest request) { commands.submitResult(id, request); return Result.success(); }
}
