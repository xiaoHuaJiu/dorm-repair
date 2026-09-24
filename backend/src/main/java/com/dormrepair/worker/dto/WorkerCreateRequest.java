package com.dormrepair.worker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WorkerCreateRequest(Long existingUserId, @Size(max=100,message="用户名不能超过100个字符") String username,
                                  @Size(min=8,max=64,message="密码长度必须为8到64个字符") String password, @Size(max=100,message="姓名不能超过100个字符") String realName,
                                  @Pattern(regexp="^$|^1[3-9]\\d{9}$",message="手机号格式不正确") String phone,
                                  @NotBlank(message="维修人员编号不能为空") @Size(max=50,message="维修人员编号不能超过50个字符") String workerNo,
                                  @Size(max=500,message="备注不能超过500个字符") String remark) {}
