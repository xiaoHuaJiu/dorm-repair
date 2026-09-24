package com.dormrepair.worker.dto;
import com.dormrepair.common.result.PageQuery;
public class WorkerQuery extends PageQuery{
 private String workerNo,realName,phone;private Integer workStatus;private Long faultTypeId;
 public String getWorkerNo(){return workerNo;}public void setWorkerNo(String v){workerNo=v;}public String getRealName(){return realName;}public void setRealName(String v){realName=v;}
 public String getPhone(){return phone;}public void setPhone(String v){phone=v;}public Integer getWorkStatus(){return workStatus;}public void setWorkStatus(Integer v){workStatus=v;}public Long getFaultTypeId(){return faultTypeId;}public void setFaultTypeId(Long v){faultTypeId=v;}
}
