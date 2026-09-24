package com.dormrepair.worker.dto;
import jakarta.validation.constraints.NotNull;import java.util.List;
public record FaultTypeBatchRequest(@NotNull(message="故障类型列表不能为空") List<Long> faultTypeIds) {}
