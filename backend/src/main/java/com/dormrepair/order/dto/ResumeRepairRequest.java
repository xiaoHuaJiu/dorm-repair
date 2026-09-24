package com.dormrepair.order.dto;

import jakarta.validation.constraints.Size;

public record ResumeRepairRequest(@Size(max = 1000) String remark) {}
