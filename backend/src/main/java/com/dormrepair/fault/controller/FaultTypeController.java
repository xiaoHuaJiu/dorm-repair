package com.dormrepair.fault.controller;

import com.dormrepair.common.result.*;
import com.dormrepair.fault.dto.*;
import com.dormrepair.fault.service.FaultTypeService;
import com.dormrepair.fault.vo.FaultTypeResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class FaultTypeController {
    private final FaultTypeService service;
    public FaultTypeController(FaultTypeService service){this.service=service;}
    @PostMapping("/api/admin/fault-types") @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String,Long>> create(@Valid @RequestBody FaultTypeCreateRequest r){return Result.success(Map.of("id",service.create(r)));}
    @PutMapping("/api/admin/fault-types/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> update(@PathVariable Long id,@Valid @RequestBody FaultTypeUpdateRequest r){service.update(id,r);return Result.success();}
    @PutMapping("/api/admin/fault-types/{id}/status") @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> status(@PathVariable Long id,@RequestParam Integer status){service.updateStatus(id,status);return Result.success();}
    @GetMapping("/api/admin/fault-types/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Result<FaultTypeResponse> detail(@PathVariable Long id){return Result.success(service.detail(id));}
    @GetMapping("/api/admin/fault-types") @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<FaultTypeResponse>> page(@ModelAttribute FaultTypeQuery q){return Result.success(service.page(q));}
    @GetMapping("/api/fault-types/enabled") public Result<List<FaultTypeResponse>> enabled(){return Result.success(service.enabled());}
}
