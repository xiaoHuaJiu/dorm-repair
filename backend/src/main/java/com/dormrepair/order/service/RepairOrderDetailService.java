package com.dormrepair.order.service;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.vo.RepairOrderDetailResponse;
import com.dormrepair.order.vo.RepairOrderBaseInfoResponse;
import org.springframework.stereotype.Service;

@Service
public class RepairOrderDetailService {
    private final RepairOrderMapper orders; private final RepairOrderAccessService access;
    private final RepairProcessRecordMapper processes; private final RepairMaterialUsageMapper materials;
    private final RepairReworkRecordMapper reworks; private final RepairEvaluationMapper evaluations; private final RepairOrderFlowMapper flows;
    public RepairOrderDetailService(RepairOrderMapper orders,RepairOrderAccessService access,RepairProcessRecordMapper processes,RepairMaterialUsageMapper materials,RepairReworkRecordMapper reworks,RepairEvaluationMapper evaluations,RepairOrderFlowMapper flows){
        this.orders=orders;this.access=access;this.processes=processes;this.materials=materials;this.reworks=reworks;this.evaluations=evaluations;this.flows=flows;
    }
    public RepairOrderDetailResponse getDetail(Long id){
        RepairOrder order=orders.selectById(id);
        if(order==null||Integer.valueOf(1).equals(order.getDeleted())) throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);
        access.checkViewPermission(order);
        boolean admin=access.currentViewerIsAdmin();
        return new RepairOrderDetailResponse(RepairOrderBaseInfoResponse.from(order,admin),processes.selectByOrderId(id),access.currentViewerIsStudent()?java.util.List.of():materials.selectByOrderId(id),reworks.selectByOrderId(id),evaluations.selectByOrderId(id),flows.selectByOrderId(id));
    }
}
