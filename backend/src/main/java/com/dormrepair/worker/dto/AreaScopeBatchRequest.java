package com.dormrepair.worker.dto;
import jakarta.validation.Valid;import jakarta.validation.constraints.NotNull;import java.util.List;
public record AreaScopeBatchRequest(@NotNull(message="负责区域列表不能为空") List<@Valid AreaScopeItem> scopes) {}
