package com.dormrepair.dispatch.service;
import com.dormrepair.dispatch.model.*; import com.dormrepair.domain.entity.*; import com.dormrepair.domain.mapper.*;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.time.LocalDateTime;
@Service public class RepairDispatchTransactionService {
 private final RepairOrderMapper orders; private final RepairOrderFlowMapper flows;
 public RepairDispatchTransactionService(RepairOrderMapper orders,RepairOrderFlowMapper flows){this.orders=orders;this.flows=flows;}
 @Transactional public boolean assign(RepairOrder snapshot,Long workerId,LocalDateTime dispatchTime,LocalDateTime deadline,DispatchSourceType source){
  if(orders.casAssign(snapshot.getId(),snapshot.getStatus(),snapshot.getCurrentAssigneeId(),workerId,dispatchTime,deadline)!=1)return false;
  RepairOrderFlow f=new RepairOrderFlow();f.setOrderId(snapshot.getId());f.setOperationType(2);f.setFromStatus(snapshot.getStatus());f.setToStatus(1);f.setOriginalAssigneeId(snapshot.getCurrentAssigneeId());f.setNewAssigneeId(workerId);f.setOperatorRole(4);f.setSourceType(source.getCode());f.setReason("系统自动派单");f.setOperationTime(dispatchTime);flows.insert(f);return true;
 }
 @Transactional public boolean markFailed(RepairOrder snapshot,DispatchFailureReason reason,DispatchSourceType source){return orders.casMarkPending(snapshot.getId(),snapshot.getStatus(),snapshot.getCurrentAssigneeId())==1;}
}
