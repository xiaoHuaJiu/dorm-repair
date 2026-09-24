package com.dormrepair.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public class CreateRepairOrderRequest extends RepairOrderDuplicateCheckRequest {
    @NotBlank @Size(max=64) private String bizNo;
    @Size(max=255) private String locationDetail;
    @NotBlank @Size(max=1000) private String problemDescription;
    @NotBlank @Size(max=100) private String contactName;
    @NotBlank @Pattern(regexp="^[0-9+() -]{6,30}$") private String contactPhone;
    @Size(max=9) private List<@NotNull Long> fileIds;
    @NotNull private Boolean confirmDuplicate;
    public String getBizNo(){return bizNo;} public void setBizNo(String v){bizNo=v;}
    public String getLocationDetail(){return locationDetail;} public void setLocationDetail(String v){locationDetail=v;}
    public String getProblemDescription(){return problemDescription;} public void setProblemDescription(String v){problemDescription=v;}
    public String getContactName(){return contactName;} public void setContactName(String v){contactName=v;}
    public String getContactPhone(){return contactPhone;} public void setContactPhone(String v){contactPhone=v;}
    public List<Long> getFileIds(){return fileIds;} public void setFileIds(List<Long> v){fileIds=v;}
    public Boolean getConfirmDuplicate(){return confirmDuplicate;} public void setConfirmDuplicate(Boolean v){confirmDuplicate=v;}
}
