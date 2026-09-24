package com.dormrepair.order.vo;

import com.dormrepair.domain.entity.RepairReworkRecord;
import com.dormrepair.file.vo.FileResponse;
import java.time.LocalDateTime;
import java.util.List;

public record RepairReworkRecordResponse(Long id,Long orderId,Integer reworkNo,Long applicantUid,String reason,
    Long originalAssigneeId,Integer status,Integer adminIntervention,LocalDateTime createTime,LocalDateTime finishTime,
    List<FileResponse> files){
    public static RepairReworkRecordResponse from(RepairReworkRecord record,List<FileResponse> files){
        return new RepairReworkRecordResponse(record.getId(),record.getOrderId(),record.getReworkNo(),record.getApplicantUid(),
            record.getReason(),record.getOriginalAssigneeId(),record.getStatus(),record.getAdminIntervention(),
            record.getCreateTime(),record.getFinishTime(),List.copyOf(files));
    }
}
