package com.dormrepair.order.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairWorker;
import com.dormrepair.domain.mapper.RepairWorkerMapper;
import com.dormrepair.security.context.UserContext;
import org.springframework.stereotype.Service;

@Service
public class CurrentWorkerResolver {
    private final RepairWorkerMapper mapper;
    public CurrentWorkerResolver(RepairWorkerMapper mapper){this.mapper=mapper;}
    public Long resolveCurrentWorkerId(){
        RepairWorker worker=mapper.selectByUserId(UserContext.getCurrentUserId());
        if(worker==null||Integer.valueOf(1).equals(worker.getDeleted())) throw new BusinessException(ResultCodeEnum.WORKER_NOT_FOUND);
        return worker.getId();
    }
}
