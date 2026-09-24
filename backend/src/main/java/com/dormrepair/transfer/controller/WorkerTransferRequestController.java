package com.dormrepair.transfer.controller;

import com.dormrepair.common.result.PageResult;
import com.dormrepair.common.result.Result;
import com.dormrepair.transfer.dto.CreateTransferRequest;
import com.dormrepair.transfer.dto.TransferQueryRequest;
import com.dormrepair.transfer.service.WorkerTransferRequestService;
import com.dormrepair.transfer.vo.CreateTransferResponse;
import com.dormrepair.transfer.vo.TransferRequestView;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize("hasRole('WORKER')")
public class WorkerTransferRequestController {
    private final WorkerTransferRequestService service;
    public WorkerTransferRequestController(WorkerTransferRequestService service) { this.service = service; }

    @PostMapping("/api/worker/repair-orders/{orderId}/transfer-requests")
    public Result<CreateTransferResponse> create(@PathVariable Long orderId, @Valid @RequestBody CreateTransferRequest request) {
        return Result.success(service.create(orderId, request));
    }

    @GetMapping("/api/worker/transfer-requests")
    public Result<PageResult<TransferRequestView>> page(@ModelAttribute TransferQueryRequest query) {
        return Result.success(service.page(query));
    }

    @GetMapping("/api/worker/transfer-requests/{id}")
    public Result<TransferRequestView> detail(@PathVariable Long id) { return Result.success(service.detail(id)); }
}
