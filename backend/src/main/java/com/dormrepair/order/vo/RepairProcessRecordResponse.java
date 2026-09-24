package com.dormrepair.order.vo;

import com.dormrepair.domain.entity.RepairProcessRecord;
import com.dormrepair.file.vo.FileResponse;
import java.time.LocalDateTime;
import java.util.List;

public record RepairProcessRecordResponse(Long id,Long orderId,Long workerId,Integer recordType,String content,
    Integer interruptReasonType,LocalDateTime recordTime,LocalDateTime createTime,List<FileResponse> files){
    public static RepairProcessRecordResponse from(RepairProcessRecord record,List<FileResponse> files){
        return new RepairProcessRecordResponse(record.getId(),record.getOrderId(),record.getWorkerId(),record.getRecordType(),
            record.getContent(),record.getInterruptReasonType(),record.getRecordTime(),record.getCreateTime(),List.copyOf(files));
    }
}
