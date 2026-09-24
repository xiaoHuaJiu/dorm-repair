package com.dormrepair.transfer.controller;

import com.dormrepair.common.result.PageResult;
import com.dormrepair.common.result.Result;
import com.dormrepair.transfer.dto.ReviewTransferRequest;
import com.dormrepair.transfer.dto.TransferQueryRequest;
import com.dormrepair.transfer.service.AdminTransferRequestService;
import com.dormrepair.transfer.vo.TransferRequestView;
import com.dormrepair.transfer.vo.TransferReviewResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/transfer-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransferRequestController {
    private final AdminTransferRequestService service;
    public AdminTransferRequestController(AdminTransferRequestService service) { this.service = service; }
    @GetMapping public Result<PageResult<TransferRequestView>> page(@ModelAttribute TransferQueryRequest query) { return Result.success(service.page(query)); }
    @GetMapping("/{id}") public Result<TransferRequestView> detail(@PathVariable Long id) { return Result.success(service.detail(id)); }
    @PostMapping("/{id}/review") public Result<TransferReviewResponse> review(@PathVariable Long id, @Valid @RequestBody ReviewTransferRequest request) { return Result.success(service.review(id, request)); }
}
