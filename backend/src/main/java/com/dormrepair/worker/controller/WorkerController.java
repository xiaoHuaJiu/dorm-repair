package com.dormrepair.worker.controller;
import com.dormrepair.common.result.*;import com.dormrepair.domain.entity.*;import com.dormrepair.worker.dto.*;import com.dormrepair.worker.service.*;import com.dormrepair.worker.vo.WorkerDetailResponse;
import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/admin/workers") @PreAuthorize("hasRole('ADMIN')") public class WorkerController{
 private final WorkerService workers;private final WorkerConfigurationService configurations;
 public WorkerController(WorkerService w,WorkerConfigurationService c){workers=w;configurations=c;}
 @PostMapping public Result<Map<String,Long>> create(@Valid @RequestBody WorkerCreateRequest r){return Result.success(Map.of("id",workers.create(r)));}
 @GetMapping public Result<PageResult<WorkerDetailResponse>> page(@ModelAttribute WorkerQuery q){return Result.success(workers.page(q));}
 @GetMapping("/{id}") public Result<WorkerDetailResponse> detail(@PathVariable Long id){return Result.success(workers.detail(id));}
 @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id,@Valid @RequestBody WorkerUpdateRequest r){workers.update(id,r);return Result.success();}
 @PutMapping("/{id}/work-status") public Result<Void> workStatus(@PathVariable Long id,@RequestParam Integer status){workers.updateWorkStatus(id,status);return Result.success();}
 @PutMapping("/{id}/account-status") public Result<Void> accountStatus(@PathVariable Long id,@RequestParam Integer status){workers.updateAccountStatus(id,status);return Result.success();}
 @GetMapping("/{id}/fault-types") public Result<List<RepairWorkerFaultType>> faultTypes(@PathVariable Long id){return Result.success(configurations.faultTypes(id));}
 @PutMapping("/{id}/fault-types") public Result<Void> faultTypes(@PathVariable Long id,@Valid @RequestBody FaultTypeBatchRequest r){configurations.saveFaultTypes(id,r.faultTypeIds());return Result.success();}
 @GetMapping("/{id}/area-scopes") public Result<List<RepairWorkerAreaScope>> scopes(@PathVariable Long id){return Result.success(configurations.areaScopes(id));}
 @PutMapping("/{id}/area-scopes") public Result<Void> scopes(@PathVariable Long id,@Valid @RequestBody AreaScopeBatchRequest r){configurations.saveAreaScopes(id,r.scopes());return Result.success();}
}
