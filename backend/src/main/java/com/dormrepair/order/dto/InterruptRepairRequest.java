package com.dormrepair.order.dto;

import jakarta.validation.constraints.*;

public record InterruptRepairRequest(
    @NotNull @Min(1) @Max(4) Integer interruptReasonType,
    @NotBlank @Size(max = 1000) String content) {}
