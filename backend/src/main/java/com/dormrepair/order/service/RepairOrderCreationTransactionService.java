package com.dormrepair.order.service;

import com.dormrepair.common.enums.RepairOrderStatusEnum;
import com.dormrepair.domain.entity.*; import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.dto.CreateRepairOrderRequest; import com.dormrepair.order.vo.CreateRepairOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.dormrepair.dispatch.event.RepairOrderCreatedEvent;
import java.time.LocalDateTime; import java.util.List;

@Service public class RepairOrderCreationTransactionService {
 public static final String BIZ_TYPE="REPAIR_ORDER_CREATE";
 private final SysIdempotentRecordMapper idempotency; private final RepairOrderMapper orders; private final RepairOrderFlowMapper flows;
 private final SysOperationLogMapper logs; private final RepairOrderNumberGenerator numbers; private final ObjectMapper json; private final ApplicationEventPublisher events;
 public RepairOrderCreationTransactionService(SysIdempotentRecordMapper i,RepairOrderMapper o,RepairOrderFlowMapper f,SysOperationLogMapper l,RepairOrderNumberGenerator n,ObjectMapper j,ApplicationEventPublisher events){idempotency=i;orders=o;flows=f;logs=l;numbers=n;json=j;this.events=events;}
 @Transactional
 public CreateRepairOrderResponse create(Long studentUid,String username,String hash,CreateRepairOrderRequest r,List<com.dormrepair.order.vo.SuspectedRepairOrder> suspected){
  LocalDateTime now=LocalDateTime.now();
  SysIdempotentRecord idem=new SysIdempotentRecord(); idem.setBizNo(r.getBizNo());idem.setBizType(BIZ_TYPE);idem.setUserId(studentUid);idem.setRequestHash(hash);idem.setStatus(0);idem.setExpireTime(now.plusDays(1));idempotency.insert(idem);
  RepairOrder order=new RepairOrder();order.setOrderNo(numbers.next());order.setStudentUid(studentUid);order.setContactName(r.getContactName());order.setContactPhone(r.getContactPhone());order.setCampusId(r.getCampusId());order.setAreaId(r.getAreaId());order.setBuildingId(r.getBuildingId());order.setRoomId(r.getRoomId());order.setLocationDetail(r.getLocationDetail());order.setFaultTypeId(r.getFaultTypeId());order.setProblemDescription(r.getProblemDescription());order.setImageUrls(toJson(r.getImageUrls()));order.setStatus(RepairOrderStatusEnum.PENDING_DISPATCH.getCode());order.setReworkCount(0);order.setExceptionFlag(0);order.setDuplicateFlag(suspected.isEmpty()?0:1);order.setDuplicateOrderId(suspected.isEmpty()?null:suspected.get(0).getOrderId());order.setReportTime(now);order.setDeleted(0);orders.insert(order);
  RepairOrderFlow flow=new RepairOrderFlow();flow.setOrderId(order.getId());flow.setOperationType(1);flow.setToStatus(0);flow.setOperatorId(studentUid);flow.setOperatorRole(1);flow.setReason("学生提交报修");flow.setOperationTime(now);flows.insert(flow);
  SysOperationLog log=new SysOperationLog();log.setUserId(studentUid);log.setUsername(username);log.setRoleType(1);log.setModuleName("学生报修");log.setOperationName("创建报修工单");log.setBusinessType("REPAIR_ORDER");log.setBusinessId(order.getId());log.setRequestMethod("POST");log.setRequestUri("/api/student/repair-orders");log.setResultStatus(1);log.setOperationTime(now);logs.insert(log);
  CreateRepairOrderResponse result=new CreateRepairOrderResponse(true,!suspected.isEmpty(),order.getId(),order.getOrderNo(),List.copyOf(suspected));
  try{if(idempotency.markSuccess(idem.getId(),json.writeValueAsString(result))!=1)throw new IllegalStateException("幂等记录状态更新失败");}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("创建结果序列化失败",e);} events.publishEvent(new RepairOrderCreatedEvent(order.getId())); return result;
 }
 private String toJson(List<String> urls){if(urls==null||urls.isEmpty())return null;try{return json.writeValueAsString(urls);}catch(Exception e){throw new IllegalArgumentException("附件引用格式错误",e);}}
}
