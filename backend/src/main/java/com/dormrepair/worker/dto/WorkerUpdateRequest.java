package com.dormrepair.worker.dto;
import jakarta.validation.constraints.NotBlank;import jakarta.validation.constraints.Size;
public record WorkerUpdateRequest(@NotBlank(message="维修人员编号不能为空") @Size(max=50,message="维修人员编号不能超过50个字符") String workerNo,
                                  @Size(max=500,message="备注不能超过500个字符") String remark){}
