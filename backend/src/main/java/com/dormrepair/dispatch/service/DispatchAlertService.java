package com.dormrepair.dispatch.service;
import com.dormrepair.dispatch.model.*; import com.dormrepair.domain.entity.RepairDispatchAlert; import com.dormrepair.domain.mapper.RepairDispatchAlertMapper;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Propagation; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime;
@Service public class DispatchAlertService {
 private final RepairDispatchAlertMapper mapper; public DispatchAlertService(RepairDispatchAlertMapper mapper){this.mapper=mapper;}
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void record(Long orderId,DispatchSourceType source,DispatchFailureReason reason,String detail){
  LocalDateTime now=LocalDateTime.now(); RepairDispatchAlert a=new RepairDispatchAlert();a.setOrderId(orderId);a.setSourceType(source.getCode());a.setFailureReason(reason.name());a.setAlertStatus(0);a.setFailureDetail(trim(detail));a.setFirstOccurredTime(now);a.setLastOccurredTime(now);mapper.upsertOpenAlert(a);
 }
 private String trim(String value){return value==null?null:value.substring(0,Math.min(500,value.length()));}
}
