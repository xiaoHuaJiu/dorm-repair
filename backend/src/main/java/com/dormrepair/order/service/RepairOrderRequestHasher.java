package com.dormrepair.order.service;
import com.dormrepair.order.dto.CreateRepairOrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest; import java.util.LinkedHashMap; import java.util.List; import java.util.Objects;
import org.springframework.stereotype.Component;
@Component public class RepairOrderRequestHasher {
 private final ObjectMapper json=new ObjectMapper();
 public String hash(Long studentUid,CreateRepairOrderRequest r){
  try{
   var m=new LinkedHashMap<String,Object>();m.put("studentUid",studentUid);m.put("campusId",r.getCampusId());m.put("areaId",r.getAreaId());m.put("buildingId",r.getBuildingId());m.put("roomId",r.getRoomId());m.put("faultTypeId",r.getFaultTypeId());m.put("locationDetail",r.getLocationDetail());m.put("problemDescription",r.getProblemDescription());m.put("contactName",r.getContactName());m.put("contactPhone",r.getContactPhone());m.put("fileIds",r.getFileIds()==null?List.of():r.getFileIds().stream().filter(Objects::nonNull).distinct().sorted().toList());
   byte[] bytes=MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(m)); return java.util.HexFormat.of().formatHex(bytes);
  }catch(Exception e){throw new IllegalStateException("无法计算报修请求摘要",e);}
 }
}
