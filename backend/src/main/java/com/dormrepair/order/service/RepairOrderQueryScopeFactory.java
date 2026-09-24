package com.dormrepair.order.service;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.order.dto.RepairOrderQueryRequest;
import com.dormrepair.order.model.RepairOrderQueryCondition;
import com.dormrepair.security.context.UserContext;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderQueryScopeFactory {
    private final CurrentWorkerResolver workers;
    public RepairOrderQueryScopeFactory(CurrentWorkerResolver workers){this.workers=workers;}
    public RepairOrderQueryCondition create(RepairOrderQueryRequest request){
        if(request.hasInvalidTimeRange()) throw new BusinessException(ResultCodeEnum.PARAM_ERROR,"报修开始时间不能晚于结束时间");
        RepairOrderQueryCondition condition=new RepairOrderQueryCondition(request);
        UserRoleEnum role=UserContext.getCurrentRoleType();
        if(role==UserRoleEnum.STUDENT) condition.setStudentUid(UserContext.getCurrentUserId());
        else if(role==UserRoleEnum.WORKER) condition.setCurrentAssigneeId(workers.resolveCurrentWorkerId());
        else if(role!=UserRoleEnum.ADMIN) throw new BusinessException(ResultCodeEnum.FORBIDDEN);
        return condition;
    }
}
