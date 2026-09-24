package com.dormrepair.area.controller;
import com.dormrepair.area.dto.*;import com.dormrepair.area.service.AreaService;import com.dormrepair.area.vo.AreaTreeNode;
import com.dormrepair.common.result.Result;import com.dormrepair.domain.entity.RepairArea;import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController public class AreaController{
 private final AreaService service;public AreaController(AreaService service){this.service=service;}
 @PostMapping("/api/admin/areas") @PreAuthorize("hasRole('ADMIN')") public Result<Map<String,Long>> create(@Valid @RequestBody AreaCreateRequest r){return Result.success(Map.of("id",service.create(r)));}
 @PutMapping("/api/admin/areas/{id}") @PreAuthorize("hasRole('ADMIN')") public Result<Void> update(@PathVariable Long id,@Valid @RequestBody AreaUpdateRequest r){service.update(id,r);return Result.success();}
 @PutMapping("/api/admin/areas/{id}/status") @PreAuthorize("hasRole('ADMIN')") public Result<Void> status(@PathVariable Long id,@RequestParam Integer status){service.updateStatus(id,status);return Result.success();}
 @GetMapping("/api/admin/areas/{id}") @PreAuthorize("hasRole('ADMIN')") public Result<RepairArea> detail(@PathVariable Long id){return Result.success(service.detail(id));}
 @GetMapping("/api/admin/areas/tree") @PreAuthorize("hasRole('ADMIN')") public Result<List<AreaTreeNode>> adminTree(){return Result.success(service.adminTree());}
 @GetMapping("/api/areas/tree") public Result<List<AreaTreeNode>> tree(){return Result.success(service.enabledTree());}
 @GetMapping("/api/areas/{parentId}/children") public Result<List<RepairArea>> children(@PathVariable Long parentId){return Result.success(service.children(parentId,true));}
}
