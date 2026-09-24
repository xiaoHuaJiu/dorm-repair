package com.dormrepair.order.service;
import com.dormrepair.common.enums.RepairOrderStatusEnum;
import com.dormrepair.domain.mapper.RepairOrderMapper;
import com.dormrepair.order.dto.RepairOrderDuplicateCheckRequest;
import com.dormrepair.order.vo.*;
import org.springframework.stereotype.Service; import org.springframework.beans.factory.annotation.Autowired;
import java.time.Clock; import java.time.LocalDateTime; import java.util.List;
@Service public class RepairOrderDuplicateService {
 private final RepairOrderMapper orders; private final RepairOrderSubmissionValidator validator; private final Clock clock;
 @Autowired public RepairOrderDuplicateService(RepairOrderMapper o,RepairOrderSubmissionValidator v){this(o,v,Clock.systemDefaultZone());}
 public RepairOrderDuplicateService(RepairOrderMapper o,RepairOrderSubmissionValidator v,Clock c){orders=o;validator=v;clock=c;}
 public RepairOrderDuplicateCheckResponse check(RepairOrderDuplicateCheckRequest r){
  validator.validate(r); LocalDateTime since=LocalDateTime.now(clock).minusHours(24);
  List<SuspectedRepairOrder> list=orders.selectSuspectedDuplicateOrders(r.getCampusId(),r.getAreaId(),r.getBuildingId(),r.getRoomId(),r.getFaultTypeId(),since,RepairOrderStatusEnum.getOpenStatusCodes());
  list.forEach(v->v.setStatusName(RepairOrderStatusEnum.fromCode(v.getStatus()).map(RepairOrderStatusEnum::getDescription).orElse("未知状态")));
  return new RepairOrderDuplicateCheckResponse(!list.isEmpty(),List.copyOf(list));
 }
}
