package com.dormrepair.order.service;

import com.dormrepair.common.enums.*;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.*;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.dto.*;
import com.dormrepair.security.context.UserContext;
import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service public class StudentRepairOrderTransactionService {
  private final RepairOrderMapper orders; private final RepairEvaluationMapper evaluations;
  private final RepairReworkRecordMapper reworks; private final RepairOrderAlertMapper alerts;
  private final RepairOrderFlowMapper flows; private final SysOperationLogMapper logs; private final ObjectMapper json;
  public StudentRepairOrderTransactionService(RepairOrderMapper orders,RepairEvaluationMapper evaluations,
      RepairReworkRecordMapper reworks,RepairOrderAlertMapper alerts,RepairOrderFlowMapper flows,
      SysOperationLogMapper logs,ObjectMapper json){this.orders=orders;this.evaluations=evaluations;this.reworks=reworks;this.alerts=alerts;this.flows=flows;this.logs=logs;this.json=json;}

  @Transactional public void confirm(Long id){
    LoginUser user=UserContext.getCurrentUser(); LocalDateTime now=LocalDateTime.now(); RepairOrder order=ownedLocked(id,user.getUserId()); requireStatus(order,3);
    if(orders.casStudentConfirm(id,user.getUserId(),now)!=1) conflict("工单状态已变化，请刷新后重试");
    addFlow(order,user,RepairOrderOperationTypeEnum.STUDENT_CONFIRM,3,6,"学生确认维修完成",now);
    addLog(user,id,"确认维修完成","/api/student/repair-orders/"+id+"/confirm",now);
  }

  @Transactional public void evaluate(Long id,CreateRepairEvaluationRequest request){
    LoginUser user=UserContext.getCurrentUser(); LocalDateTime now=LocalDateTime.now(); RepairOrder order=ownedLocked(id,user.getUserId()); requireStatus(order,6);
    if(order.getCurrentAssigneeId()==null) conflict("工单没有维修负责人，暂不能评价");
    if(evaluations.selectByOrderId(id)!=null) throw new BusinessException(ResultCodeEnum.ORDER_EVALUATION_EXISTS);
    RepairEvaluation e=new RepairEvaluation(); e.setOrderId(id);e.setStudentUid(user.getUserId());e.setWorkerId(order.getCurrentAssigneeId());e.setScore(request.score());
    e.setContent(request.content()==null||request.content().isBlank()?null:request.content().trim());
    try{evaluations.insert(e);}catch(DuplicateKeyException ex){throw new BusinessException(ResultCodeEnum.ORDER_EVALUATION_EXISTS);}
    addLog(user,id,"提交维修评价","/api/student/repair-orders/"+id+"/evaluation",now);
  }

  @Transactional public void rework(Long id,CreateReworkRequest request){
    LoginUser user=UserContext.getCurrentUser(); LocalDateTime now=LocalDateTime.now(); RepairOrder original=ownedLocked(id,user.getUserId()); requireStatus(original,3);
    if(orders.casStudentRework(id,user.getUserId())!=1) conflict("工单状态已变化，请刷新后重试");
    RepairOrder updated=orders.selectByIdForUpdate(id); int no=updated.getReworkCount();
    RepairReworkRecord record=new RepairReworkRecord();record.setOrderId(id);record.setReworkNo(no);record.setApplicantUid(user.getUserId());record.setReason(request.reason().trim());
    record.setImageUrls(toJson(request.imageUrls()));record.setOriginalAssigneeId(original.getCurrentAssigneeId());record.setStatus(0);record.setAdminIntervention(0);record.setDeleted(0);reworks.insert(record);
    if(no>=2){RepairOrderAlertTypeEnum type=no==2?RepairOrderAlertTypeEnum.REWORK_WARNING:RepairOrderAlertTypeEnum.REWORK_EXCEPTION;
      RepairOrderAlert alert=new RepairOrderAlert();alert.setOrderId(id);alert.setReworkNo(no);alert.setAlertType(type.name());alert.setAlertLevel(type.getLevel());alert.setAlertStatus(0);
      alert.setAlertContent(no==2?"工单已第二次返工，请关注处理质量":"工单已多次返工，需管理员介入");alerts.insertIgnore(alert);}
    addFlow(original,user,RepairOrderOperationTypeEnum.REWORK,3,4,request.reason().trim(),now);
    addLog(user,id,"申请返工","/api/student/repair-orders/"+id+"/rework",now);
  }
  private RepairOrder ownedLocked(Long id,Long uid){RepairOrder o=orders.selectByIdForUpdate(id);if(o==null||Integer.valueOf(1).equals(o.getDeleted()))throw new BusinessException(ResultCodeEnum.ORDER_NOT_FOUND);if(!uid.equals(o.getStudentUid()))throw new BusinessException(ResultCodeEnum.ORDER_ACCESS_DENIED);return o;}
  private void requireStatus(RepairOrder o,int status){if(!Integer.valueOf(status).equals(o.getStatus()))conflict("当前工单状态不允许执行该操作");}
  private void conflict(String m){throw new BusinessException(ResultCodeEnum.DATA_CONFLICT,m);}
  private String toJson(List<String> values){if(values==null||values.isEmpty())return null;try{return json.writeValueAsString(values);}catch(JsonProcessingException e){throw new BusinessException(ResultCodeEnum.PARAM_ERROR,"图片地址格式错误");}}
  private void addFlow(RepairOrder o,LoginUser u,RepairOrderOperationTypeEnum type,int from,int to,String reason,LocalDateTime now){RepairOrderFlow f=new RepairOrderFlow();f.setOrderId(o.getId());f.setOperationType(type.getCode());f.setFromStatus(from);f.setToStatus(to);f.setOriginalAssigneeId(o.getCurrentAssigneeId());f.setNewAssigneeId(o.getCurrentAssigneeId());f.setOperatorId(u.getUserId());f.setOperatorRole(UserRoleEnum.STUDENT.getCode());f.setReason(reason);f.setOperationTime(now);flows.insert(f);}
  private void addLog(LoginUser u,Long id,String name,String uri,LocalDateTime now){SysOperationLog l=new SysOperationLog();l.setUserId(u.getUserId());l.setUsername(u.getUsername());l.setRoleType(UserRoleEnum.STUDENT.getCode());l.setModuleName("学生工单");l.setOperationName(name);l.setBusinessType("repair_order");l.setBusinessId(id);l.setRequestMethod("POST");l.setRequestUri(uri);l.setResultStatus(1);l.setOperationTime(now);logs.insert(l);}
}
