package com.dormrepair.worker.service;

import com.dormrepair.common.enums.*;import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.*;import com.dormrepair.domain.mapper.*;import com.dormrepair.worker.dto.WorkerCreateRequest;
import org.springframework.dao.DuplicateKeyException;import org.springframework.security.crypto.password.PasswordEncoder;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import com.dormrepair.worker.dto.WorkerQuery;import com.dormrepair.worker.dto.WorkerUpdateRequest;import com.dormrepair.worker.vo.WorkerDetailResponse;import com.dormrepair.common.result.PageResult;import com.github.pagehelper.PageHelper;import com.github.pagehelper.PageInfo;

@Service public class WorkerService{
 private final SysUserMapper users;private final RepairWorkerMapper workers;private final PasswordEncoder encoder;
 public WorkerService(SysUserMapper users,RepairWorkerMapper workers,PasswordEncoder encoder){this.users=users;this.workers=workers;this.encoder=encoder;}
 @Transactional public Long create(WorkerCreateRequest r){
  if(workers.selectByWorkerNo(r.workerNo())!=null)throw new BusinessException(ResultCodeEnum.WORKER_NO_EXISTS);
  SysUser user=r.existingUserId()==null?createUser(r):requireBindableUser(r.existingUserId());
  if(workers.selectByUserId(user.getId())!=null)throw new BusinessException(ResultCodeEnum.WORKER_USER_EXISTS);
  RepairWorker worker=new RepairWorker();worker.setUserId(user.getId());worker.setWorkerNo(r.workerNo().trim());worker.setWorkStatus(WorkerWorkStatusEnum.NORMAL.getCode());worker.setRemark(r.remark());worker.setDeleted(0);
  try{workers.insert(worker);}catch(DuplicateKeyException ex){throw new BusinessException(ResultCodeEnum.DATA_CONFLICT,"维修人员账号或编号已存在");}return worker.getId();
 }
 @Transactional public void updateWorkStatus(Long id,Integer status){require(id);if(WorkerWorkStatusEnum.fromCode(status).isEmpty())throw new BusinessException(ResultCodeEnum.PARAM_ERROR);workers.updateStatus(id,status);}
 @Transactional public void update(Long id,WorkerUpdateRequest request){RepairWorker worker=require(id);RepairWorker sameNo=workers.selectByWorkerNo(request.workerNo().trim());if(sameNo!=null&&!sameNo.getId().equals(id))throw new BusinessException(ResultCodeEnum.WORKER_NO_EXISTS);worker.setWorkerNo(request.workerNo().trim());worker.setRemark(request.remark());try{workers.update(worker);}catch(DuplicateKeyException ex){throw new BusinessException(ResultCodeEnum.WORKER_NO_EXISTS);}}
 @Transactional public void updateAccountStatus(Long id,Integer status){RepairWorker w=require(id);if(status==null||(status!=0&&status!=1))throw new BusinessException(ResultCodeEnum.PARAM_ERROR);users.updateStatus(w.getUserId(),status);}
 public WorkerDetailResponse detail(Long id){WorkerDetailResponse v=workers.selectDetail(id);if(v==null)throw new BusinessException(ResultCodeEnum.WORKER_NOT_FOUND);return v;}
 public PageResult<WorkerDetailResponse> page(WorkerQuery q){PageHelper.startPage(q.getPageNum(),q.getPageSize());return PageResult.from(new PageInfo<>(workers.selectPage(q.getWorkerNo(),q.getRealName(),q.getPhone(),q.getWorkStatus(),q.getFaultTypeId())));}
 public RepairWorker require(Long id){RepairWorker v=workers.selectById(id);if(v==null||Integer.valueOf(1).equals(v.getDeleted()))throw new BusinessException(ResultCodeEnum.WORKER_NOT_FOUND);return v;}
 private SysUser createUser(WorkerCreateRequest r){if(blank(r.username())||blank(r.password())||r.password().length()<8||blank(r.realName()))throw new BusinessException(ResultCodeEnum.PARAM_ERROR,"新建账号信息不完整");if(users.selectByUsername(r.username())!=null)throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);
  SysUser u=new SysUser();u.setUsername(r.username().trim());u.setPassword(encoder.encode(r.password()));u.setRealName(r.realName().trim());u.setPhone(blank(r.phone())?null:r.phone().trim());u.setRoleType(UserRoleEnum.WORKER.getCode());u.setStatus(1);u.setDeleted(0);
  try{users.insert(u);}catch(DuplicateKeyException ex){throw new BusinessException(ResultCodeEnum.USERNAME_EXISTS);}return u;}
 private SysUser requireBindableUser(Long id){SysUser u=users.selectById(id);if(u==null||u.getDeleted()!=0||u.getStatus()!=1||u.getRoleType()!=UserRoleEnum.WORKER.getCode())throw new BusinessException(ResultCodeEnum.WORKER_USER_INVALID);return u;}
 private boolean blank(String value){return value==null||value.isBlank();}
}
