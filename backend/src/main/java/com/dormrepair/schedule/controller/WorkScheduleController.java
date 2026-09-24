package com.dormrepair.schedule.controller;
import com.dormrepair.common.result.*;import com.dormrepair.schedule.dto.*;import com.dormrepair.schedule.service.WorkScheduleService;import com.dormrepair.domain.entity.RepairWorkSchedule;import com.dormrepair.security.context.UserContext;
import jakarta.validation.Valid;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;import java.util.Map;
@RestController @RequestMapping("/api/admin/work-schedules") @PreAuthorize("hasRole('ADMIN')") public class WorkScheduleController{
 private final WorkScheduleService service;public WorkScheduleController(WorkScheduleService service){this.service=service;}
 @PostMapping public Result<Map<String,Long>> create(@Valid @RequestBody WorkScheduleCreateRequest r){return Result.success(Map.of("id",service.create(r,UserContext.getCurrentUserId())));}
 @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id,@Valid @RequestBody WorkScheduleUpdateRequest r){service.update(id,r);return Result.success();}
 @PutMapping("/{id}/status") public Result<Void> status(@PathVariable Long id,@RequestParam Integer status){service.updateStatus(id,status);return Result.success();}
 @GetMapping("/{id}") public Result<RepairWorkSchedule> detail(@PathVariable Long id){return Result.success(service.detail(id));}
 @GetMapping public Result<PageResult<RepairWorkSchedule>> page(@ModelAttribute WorkScheduleQuery q){return Result.success(service.page(q));}
}
