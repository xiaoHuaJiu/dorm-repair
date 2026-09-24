package com.dormrepair.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AddRepairProcessRequest(
    @NotBlank @Size(max = 1000) String content,
    @Size(max = 9) List<@NotBlank @Size(max = 500) String> imageUrls) {}
