package com.dormrepair.order.dto;
import jakarta.validation.constraints.*;
public record CreateRepairEvaluationRequest(@NotNull @Min(1) @Max(5) Integer score, @Size(max=1000) String content) {}
