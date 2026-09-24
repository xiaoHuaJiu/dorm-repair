package com.dormrepair.order.vo;

import com.dormrepair.domain.entity.RepairOrder;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import com.dormrepair.file.vo.FileResponse;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RepairOrderBaseInfoResponse(
    Long id,
    String orderNo,
    Long studentUid,
    String contactName,
    String contactPhone,
    Long campusId,
    Long areaId,
    Long buildingId,
    Long roomId,
    String locationDetail,
    Long faultTypeId,
    String problemDescription,
    String imageUrls,
    List<FileResponse> files,
    Integer status,
    Long currentAssigneeId,
    LocalDateTime dispatchTime,
    LocalDateTime acceptDeadline,
    LocalDateTime acceptTime,
    LocalDateTime expectedCompleteTime,
    LocalDateTime completeDeadline,
    LocalDateTime repairSubmitTime,
    LocalDateTime confirmTime,
    LocalDateTime completeTime,
    Integer reworkCount,
    Integer exceptionFlag,
    Integer duplicateFlag,
    Long duplicateOrderId,
    LocalDateTime reportTime,
    LocalDateTime cancelTime,
    String cancelReason
) {
    public static RepairOrderBaseInfoResponse from(RepairOrder order, boolean includeAdminFields,List<FileResponse> files) {
        return new RepairOrderBaseInfoResponse(
            order.getId(), order.getOrderNo(), order.getStudentUid(), order.getContactName(), order.getContactPhone(),
            order.getCampusId(), order.getAreaId(), order.getBuildingId(), order.getRoomId(), order.getLocationDetail(),
            order.getFaultTypeId(), order.getProblemDescription(), order.getImageUrls(), List.copyOf(files), order.getStatus(),
            order.getCurrentAssigneeId(), order.getDispatchTime(), order.getAcceptDeadline(), order.getAcceptTime(),
            order.getExpectedCompleteTime(), order.getCompleteDeadline(), order.getRepairSubmitTime(), order.getConfirmTime(),
            order.getCompleteTime(), order.getReworkCount(), order.getExceptionFlag(), order.getDuplicateFlag(),
            includeAdminFields ? order.getDuplicateOrderId() : null, order.getReportTime(), order.getCancelTime(),
            order.getCancelReason()
        );
    }
}
