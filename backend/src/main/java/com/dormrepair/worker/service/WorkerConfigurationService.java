package com.dormrepair.worker.service;

import com.dormrepair.common.enums.*;import com.dormrepair.common.exception.BusinessException;import com.dormrepair.domain.entity.*;import com.dormrepair.domain.mapper.*;import com.dormrepair.worker.dto.AreaScopeItem;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.util.*;import java.util.function.Function;import java.util.stream.Collectors;

@Service public class WorkerConfigurationService{
 private final RepairWorkerMapper workers;private final RepairFaultTypeMapper faults;private final RepairWorkerFaultTypeMapper workerFaults;private final RepairAreaMapper areas;private final RepairWorkerAreaScopeMapper scopes;
 public WorkerConfigurationService(RepairWorkerMapper w,RepairFaultTypeMapper f,RepairWorkerFaultTypeMapper wf,RepairAreaMapper a,RepairWorkerAreaScopeMapper s){workers=w;faults=f;workerFaults=wf;areas=a;scopes=s;}
 @Transactional public void saveFaultTypes(Long workerId,List<Long> ids){requireConfigurableWorker(workerId);Set<Long> desired=new LinkedHashSet<>(ids);for(Long id:desired){RepairFaultType f=faults.selectById(id);if(f==null||f.getStatus()!=1)throw new BusinessException(ResultCodeEnum.FAULT_UNAVAILABLE);}
  Map<Long,RepairWorkerFaultType> existing=workerFaults.selectByWorkerIdForUpdate(workerId).stream().collect(Collectors.toMap(RepairWorkerFaultType::getFaultTypeId,Function.identity()));
  for(RepairWorkerFaultType old:existing.values()){int status=desired.contains(old.getFaultTypeId())?1:0;if(old.getStatus()!=status)workerFaults.updateStatus(old.getId(),status);}
  for(Long id:desired)if(!existing.containsKey(id)){RepairWorkerFaultType v=new RepairWorkerFaultType();v.setWorkerId(workerId);v.setFaultTypeId(id);v.setStatus(1);workerFaults.insert(v);}
 }
 public List<RepairWorkerFaultType> faultTypes(Long workerId){requireExistingWorker(workerId);return workerFaults.selectByWorkerId(workerId);}
 @Transactional public void saveAreaScopes(Long workerId,List<AreaScopeItem> requested){requireConfigurableWorker(workerId);List<AreaScopeItem> desired=normalize(requested);desired.forEach(this::validateScope);
  List<RepairWorkerAreaScope> old=scopes.selectByWorkerIdForUpdate(workerId);Map<String,RepairWorkerAreaScope> existing=old.stream().collect(Collectors.toMap(this::key,Function.identity(),(a,b)->a));Set<String> keys=desired.stream().map(this::key).collect(Collectors.toSet());
  for(RepairWorkerAreaScope v:old){int status=keys.contains(key(v))?1:0;if(v.getStatus()!=status)scopes.updateStatus(v.getId(),status);}
  for(AreaScopeItem item:desired)if(!existing.containsKey(key(item))){RepairWorkerAreaScope v=new RepairWorkerAreaScope();v.setWorkerId(workerId);v.setCampusId(item.campusId());v.setAreaId(item.areaId());v.setBuildingId(item.buildingId());v.setStatus(1);scopes.insert(v);}
 }
 public List<RepairWorkerAreaScope> areaScopes(Long workerId){requireExistingWorker(workerId);return scopes.selectByWorkerId(workerId);}
 private List<AreaScopeItem> normalize(List<AreaScopeItem> input){LinkedHashMap<String,AreaScopeItem> unique=new LinkedHashMap<>();input.forEach(v->unique.put(key(v),v));List<AreaScopeItem> all=new ArrayList<>(unique.values());
  return all.stream().filter(v->all.stream().noneMatch(parent->covers(parent,v)&&!key(parent).equals(key(v)))).toList();}
 private boolean covers(AreaScopeItem p,AreaScopeItem c){if(!p.campusId().equals(c.campusId()))return false;if(p.areaId()==null)return true;if(!p.areaId().equals(c.areaId()))return false;return p.buildingId()==null;}
 private void validateScope(AreaScopeItem s){RepairArea campus=active(s.campusId(),1);if(campus.getParentId()!=0)invalid();if(s.areaId()==null){if(s.buildingId()!=null)invalid();return;}RepairArea area=active(s.areaId(),2);if(!area.getParentId().equals(campus.getId()))invalid();if(s.buildingId()!=null){RepairArea building=active(s.buildingId(),3);if(!building.getParentId().equals(area.getId()))invalid();}}
 private RepairArea active(Long id,int type){RepairArea v=areas.selectById(id);if(v==null||v.getAreaType()!=type||v.getStatus()!=1||v.getDeleted()!=0){invalid();}return v;}
 private void invalid(){throw new BusinessException(ResultCodeEnum.WORKER_SCOPE_INVALID);}
 private RepairWorker requireExistingWorker(Long id){RepairWorker v=workers.selectById(id);if(v==null||v.getDeleted()!=0)throw new BusinessException(ResultCodeEnum.WORKER_NOT_FOUND);return v;}
 private RepairWorker requireConfigurableWorker(Long id){RepairWorker v=requireExistingWorker(id);if(v.getWorkStatus()==WorkerWorkStatusEnum.DISABLED.getCode())throw new BusinessException(ResultCodeEnum.WORKER_UNAVAILABLE);return v;}
 private String key(AreaScopeItem v){return v.campusId()+":"+v.areaId()+":"+v.buildingId();}private String key(RepairWorkerAreaScope v){return v.getCampusId()+":"+v.getAreaId()+":"+v.getBuildingId();}
}
