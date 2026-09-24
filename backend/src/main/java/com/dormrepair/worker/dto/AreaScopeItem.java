package com.dormrepair.worker.dto;
import jakarta.validation.constraints.NotNull;
public record AreaScopeItem(@NotNull(message="校区不能为空") Long campusId, Long areaId, Long buildingId) {}
