package com.dormrepair.area.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AreaUpdateRequest(@NotBlank(message="位置名称不能为空") @Size(max = 100,message="位置名称不能超过100个字符") String areaName,
                                @NotNull(message="排序不能为空") Integer sortNo, @Size(max = 500,message="备注不能超过500个字符") String remark) {}
