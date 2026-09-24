package com.dormrepair.order.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.security.context.UserContext;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderAccessService {
    private final CurrentWorkerResolver workers;
    public RepairOrderAccessService(CurrentWorkerResolver workers){this.workers=workers;}
    public boolean currentViewerIsAdmin(){return UserContext.isAdmin();}
    public boolean currentViewerIsStudent(){return UserContext.isStudent();}
    public void checkViewPermission(RepairOrder order){
        UserRoleEnum role=UserContext.getCurrentRoleType();
        boolean allowed=role==UserRoleEnum.ADMIN
            || role==UserRoleEnum.STUDENT&&UserContext.getCurrentUserId().equals(order.getStudentUid())
            || role==UserRoleEnum.WORKER&&workers.resolveCurrentWorkerId().equals(order.getCurrentAssigneeId());
        if(!allowed) throw new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED);
    }
}
