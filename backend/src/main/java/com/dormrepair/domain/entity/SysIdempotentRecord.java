package com.dormrepair.domain.entity;

import java.time.LocalDateTime;

public class SysIdempotentRecord {
    private Long id; private String bizNo; private String bizType; private Long userId; private String requestHash;
    private Integer status; private String resultSnapshot; private LocalDateTime expireTime; private LocalDateTime createTime; private LocalDateTime updateTime;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public String getBizNo(){return bizNo;} public void setBizNo(String v){bizNo=v;}
    public String getBizType(){return bizType;} public void setBizType(String v){bizType=v;}
    public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public String getRequestHash(){return requestHash;} public void setRequestHash(String v){requestHash=v;}
    public Integer getStatus(){return status;} public void setStatus(Integer v){status=v;}
    public String getResultSnapshot(){return resultSnapshot;} public void setResultSnapshot(String v){resultSnapshot=v;}
    public LocalDateTime getExpireTime(){return expireTime;} public void setExpireTime(LocalDateTime v){expireTime=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
}
