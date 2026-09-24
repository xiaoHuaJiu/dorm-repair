package com.dormrepair.order.controller;
import com.dormrepair.common.result.*; import com.dormrepair.order.dto.RepairOrderQueryRequest; import com.dormrepair.order.service.*; import com.dormrepair.order.vo.*;
import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/repair-orders") @PreAuthorize("hasRole('ADMIN')")
public class AdminRepairOrderController { private final RepairOrderQueryService queries; private final RepairOrderDetailService details; public AdminRepairOrderController(RepairOrderQueryService q,RepairOrderDetailService d){queries=q;details=d;} @GetMapping public Result<PageResult<RepairOrderListItem>> page(@ModelAttribute RepairOrderQueryRequest q){return Result.success(queries.page(q));} @GetMapping("/{id}") public Result<RepairOrderDetailResponse> detail(@PathVariable Long id){return Result.success(details.getDetail(id));} }
