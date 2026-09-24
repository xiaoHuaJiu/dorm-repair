package com.dormrepair.worker.vo;
public record WorkerDetailResponse(Long workerId,Long userId,String username,String realName,String phone,
                                   String workerNo,Integer userStatus,Integer workStatus,String remark){}
