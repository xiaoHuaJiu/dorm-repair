package com.dormrepair.dispatch.service;
import com.dormrepair.dispatch.model.*; import com.dormrepair.domain.entity.RepairOrder; import com.dormrepair.domain.mapper.*;
import org.springframework.stereotype.Service; import java.time.*; import java.util.*;
@Service public class RepairDispatchService {
 private final RepairOrderMapper orders; private final RepairWorkerMapper workers; private final WorkingTimeCalculator time;
 private final RepairDispatchTransactionService transactions; private final DispatchAlertService alerts;
 public RepairDispatchService(RepairOrderMapper o,RepairWorkerMapper w,WorkingTimeCalculator t,RepairDispatchTransactionService tx,DispatchAlertService a){orders=o;workers=w;time=t;transactions=tx;alerts=a;}
 public DispatchResult dispatch(Long orderId,Collection<Long> excludeWorkerIds,DispatchSourceType source){
  RepairOrder order=orders.selectById(orderId); if(order==null)return DispatchResult.failure(DispatchFailureReason.ORDER_STATE_CHANGED);
  if(!allowed(order.getStatus(),source))return DispatchResult.failure(DispatchFailureReason.ORDER_STATE_CHANGED);
  List<Long> excluded=excludeWorkerIds==null?List.of():excludeWorkerIds.stream().filter(Objects::nonNull).distinct().sorted().toList();
  // 请假转派：工单已不再属于请假人员（被并发处理转走）时无需重复转派，视为已处理
  if(source==DispatchSourceType.LEAVE_REASSIGN&&!needsLeaveReassign(order,excluded))return DispatchResult.success(order.getCurrentAssigneeId());
  if(workers.countSkillWorkers(order.getFaultTypeId(),excluded)==0)return fail(order,source,DispatchFailureReason.NO_SKILL_WORKER,"没有匹配故障类型的维修人员");
  if(workers.countAreaWorkers(order.getFaultTypeId(),order.getCampusId(),order.getAreaId(),order.getBuildingId(),excluded)==0)return fail(order,source,DispatchFailureReason.NO_AREA_WORKER,"没有覆盖报修区域的维修人员");
  List<DispatchCandidate> candidates=workers.selectDispatchCandidates(order.getFaultTypeId(),order.getCampusId(),order.getAreaId(),order.getBuildingId(),LocalDateTime.now(),excluded);
  if(candidates.isEmpty())return fail(order,source,DispatchFailureReason.NO_AVAILABLE_WORKER,"匹配人员均停用、请假或不可用");
  DispatchCandidate selected=candidates.stream().min(Comparator.comparing(DispatchCandidate::workload).thenComparing(DispatchCandidate::workerId)).orElseThrow();
  LocalDateTime dispatchTime=LocalDateTime.now(); LocalDateTime deadline;
  try{deadline=time.calculateDeadline(dispatchTime,Duration.ofMinutes(30));}catch(DispatchException e){return fail(order,source,e.getReason(),e.getMessage());}
  try{
   if(!transactions.assign(order,selected.workerId(),dispatchTime,deadline,source)){
    // CAS 未命中：并发场景下若工单已被其他进程成功转走，视为已处理成功而非失败
    if(source==DispatchSourceType.LEAVE_REASSIGN){RepairOrder current=orders.selectById(orderId);if(!needsLeaveReassign(current,excluded))return DispatchResult.success(current==null?null:current.getCurrentAssigneeId());}
    return DispatchResult.failure(DispatchFailureReason.ORDER_STATE_CHANGED);
   }
   return DispatchResult.success(selected.workerId());
  }
  catch(RuntimeException e){alerts.record(orderId,source,DispatchFailureReason.SYSTEM_ERROR,e.getMessage());return DispatchResult.failure(DispatchFailureReason.SYSTEM_ERROR);}
 }
 private boolean needsLeaveReassign(RepairOrder order,List<Long> excluded){return order!=null&&excluded.contains(order.getCurrentAssigneeId())&&List.of(1,2,4,5).contains(order.getStatus());}
 private DispatchResult fail(RepairOrder order,DispatchSourceType source,DispatchFailureReason reason,String detail){
  if(transactions.markFailed(order,reason,source))alerts.record(order.getId(),source,reason,detail);
  return DispatchResult.failure(reason);
 }
 private boolean allowed(Integer status,DispatchSourceType source){return switch(source){case INITIAL_REPORT->Integer.valueOf(0).equals(status);case ACCEPT_TIMEOUT->Integer.valueOf(1).equals(status);case LEAVE_REASSIGN,TRANSFER_REASSIGN->List.of(1,2,4,5).contains(status);};}
}
