package com.dormrepair.order.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record CreateReworkRequest(@NotBlank @Size(max=1000) String reason,
    @Size(max=9) List<@NotBlank @Size(max=500) String> imageUrls) {}
