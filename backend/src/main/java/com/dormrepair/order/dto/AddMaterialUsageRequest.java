package com.dormrepair.order.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AddMaterialUsageRequest(
    @NotBlank @Size(max = 100) String materialName,
    @Size(max = 100) String specification,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal quantity,
    @NotBlank @Size(max = 20) String unit,
    @Size(max = 500) String remark) {}
