package com.dormrepair.order.service;

import com.dormrepair.common.constant.RedisConstant;
import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.SysIdempotentRecord;
import com.dormrepair.domain.mapper.SysIdempotentRecordMapper;
import com.dormrepair.order.dto.*; import com.dormrepair.order.vo.*;
import com.dormrepair.security.context.UserContext; import com.dormrepair.security.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger; import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException; import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration; import java.util.List;

@Service public class StudentRepairOrderCreationService {
 private static final Logger log=LoggerFactory.getLogger(StudentRepairOrderCreationService.class);
 private final RepairOrderDuplicateService duplicates; private final RepairOrderRequestHasher hasher; private final SysIdempotentRecordMapper records;
 private final StringRedisTemplate redis; private final RepairOrderCreationTransactionService transactions; private final ObjectMapper json;
 public StudentRepairOrderCreationService(RepairOrderDuplicateService d,RepairOrderRequestHasher h,SysIdempotentRecordMapper r,StringRedisTemplate redis,RepairOrderCreationTransactionService t,ObjectMapper j){duplicates=d;hasher=h;records=r;this.redis=redis;transactions=t;json=j;}
 public RepairOrderDuplicateCheckResponse checkDuplicate(RepairOrderDuplicateCheckRequest request){return duplicates.check(request);}
 public CreateRepairOrderResponse create(CreateRepairOrderRequest request){
  LoginUser user=UserContext.getCurrentUser(); Long uid=user.getUserId(); String hash=hasher.hash(uid,request);
  CreateRepairOrderResponse previous=replayIfPresent(uid,request.getBizNo(),hash); if(previous!=null)return previous;
  String key=RedisConstant.repairOrderCreateKey(uid,request.getBizNo()); boolean acquired=tryAcquire(key);
  if(!acquired){previous=replayIfPresent(uid,request.getBizNo(),hash);if(previous!=null)return previous;throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);}
  try{
   RepairOrderDuplicateCheckResponse duplicate=duplicates.check(request);
   if(duplicate.duplicate()&&!Boolean.TRUE.equals(request.getConfirmDuplicate()))return new CreateRepairOrderResponse(false,true,null,null,duplicate.suspectedOrders());
   CreateRepairOrderResponse result;
   try{result=transactions.create(uid,user.getUsername(),hash,request,duplicate.suspectedOrders());}
   catch(DuplicateKeyException e){result=replayIfPresent(uid,request.getBizNo(),hash);if(result==null)throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);}
   markSuccess(key,result.orderId());
   return result;
  }finally{if(!isSuccess(key))release(key);}
 }
 private CreateRepairOrderResponse replayIfPresent(Long uid,String bizNo,String hash){
  SysIdempotentRecord r=records.selectByBusinessKey(RepairOrderCreationTransactionService.BIZ_TYPE,uid,bizNo);if(r==null)return null;
  if(!hash.equals(r.getRequestHash()))throw new BusinessException(ResultCodeEnum.ORDER_BIZ_NO_CONFLICT);
  if(!Integer.valueOf(1).equals(r.getStatus())||r.getResultSnapshot()==null)throw new BusinessException(ResultCodeEnum.ORDER_IDEMPOTENT_PROCESSING);
  try{return json.readValue(r.getResultSnapshot(),CreateRepairOrderResponse.class);}catch(Exception e){throw new IllegalStateException("幂等结果快照无法读取",e);}
 }
 private boolean tryAcquire(String key){try{return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key,"PROCESSING",Duration.ofMinutes(RedisConstant.REPAIR_ORDER_CREATE_TTL_MINUTES)));}catch(RuntimeException e){log.warn("Redis 幂等门闩不可用，降级为 MySQL 唯一约束",e);return true;}}
 private void markSuccess(String key,Long orderId){try{redis.opsForValue().set(key,"SUCCESS:"+orderId,Duration.ofMinutes(RedisConstant.REPAIR_ORDER_CREATE_TTL_MINUTES));}catch(RuntimeException e){log.warn("Redis 幂等成功标记写入失败，MySQL 记录仍可重放",e);}}
 private boolean isSuccess(String key){try{String v=redis.opsForValue().get(key);return v!=null&&v.startsWith("SUCCESS:");}catch(RuntimeException e){return true;}}
 private void release(String key){try{redis.delete(key);}catch(RuntimeException e){log.warn("Redis 幂等处理中标记清理失败，等待 TTL 到期",e);}}
}
