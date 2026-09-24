package com.dormrepair.order.vo;

import com.dormrepair.domain.entity.*;
import java.util.List;

public class RepairOrderDetailResponse {
    private final RepairOrderBaseInfoResponse baseInfo;
    private final List<RepairProcessRecord> processRecords;
    private final List<RepairMaterialUsage> materialRecords;
    private final List<RepairReworkRecord> reworkRecords;
    private final RepairEvaluation evaluation;
    private final List<RepairOrderFlow> flows;
    public RepairOrderDetailResponse(RepairOrderBaseInfoResponse baseInfo,List<RepairProcessRecord> processRecords,List<RepairMaterialUsage> materialRecords,List<RepairReworkRecord> reworkRecords,RepairEvaluation evaluation,List<RepairOrderFlow> flows){
        this.baseInfo=baseInfo;this.processRecords=List.copyOf(processRecords);this.materialRecords=List.copyOf(materialRecords);this.reworkRecords=List.copyOf(reworkRecords);this.evaluation=evaluation;this.flows=List.copyOf(flows);
    }
    public RepairOrderBaseInfoResponse getBaseInfo(){return baseInfo;} public List<RepairProcessRecord> getProcessRecords(){return processRecords;}
    public List<RepairMaterialUsage> getMaterialRecords(){return materialRecords;} public List<RepairReworkRecord> getReworkRecords(){return reworkRecords;}
    public RepairEvaluation getEvaluation(){return evaluation;} public List<RepairOrderFlow> getFlows(){return flows;}
}
