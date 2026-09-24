package com.dormrepair.worker;

import com.dormrepair.common.enums.ResultCodeEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.*;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.worker.dto.*;
import com.dormrepair.worker.service.WorkerConfigurationService;
import com.dormrepair.worker.service.WorkerService;
import com.dormrepair.worker.vo.WorkerDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class WorkerFoundationServiceTest {
    @Test void returnsCombinedWorkerDetail() {
        SysUserMapper users=mock(SysUserMapper.class);RepairWorkerMapper workers=mock(RepairWorkerMapper.class);
        WorkerDetailResponse detail=new WorkerDetailResponse(5L,7L,"worker","维修甲","13800138000","W001",1,0,"备注");
        when(workers.selectDetail(5L)).thenReturn(detail);
        WorkerService service=new WorkerService(users,workers,mock(PasswordEncoder.class));
        assertThat(service.detail(5L).username()).isEqualTo("worker");
    }
    @Test void rejectsDuplicateWorkerNumberOnUpdate() {
        SysUserMapper users=mock(SysUserMapper.class);RepairWorkerMapper workers=mock(RepairWorkerMapper.class);
        RepairWorker current=new RepairWorker();current.setId(5L);current.setWorkerNo("W001");current.setDeleted(0);when(workers.selectById(5L)).thenReturn(current);
        RepairWorker duplicate=new RepairWorker();duplicate.setId(6L);duplicate.setWorkerNo("W002");when(workers.selectByWorkerNo("W002")).thenReturn(duplicate);
        WorkerService service=new WorkerService(users,workers,mock(PasswordEncoder.class));
        assertThatThrownBy(()->service.update(5L,new WorkerUpdateRequest("W002","新备注")))
            .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo(ResultCodeEnum.WORKER_NO_EXISTS.getCode()));
    }
    @Test void createsWorkerAccountWithWorkerRoleAndEncodedPassword() {
        SysUserMapper users=mock(SysUserMapper.class);RepairWorkerMapper workers=mock(RepairWorkerMapper.class);PasswordEncoder encoder=mock(PasswordEncoder.class);
        when(encoder.encode("StrongPass123")).thenReturn("hash");WorkerService service=new WorkerService(users,workers,encoder);
        service.create(new WorkerCreateRequest(null,"worker-new","StrongPass123","维修甲","13800138000","W001","备注"));
        verify(users).insert(argThat(u->u.getRoleType()==2&&"hash".equals(u.getPassword())));
        verify(workers).insert(argThat(w->"W001".equals(w.getWorkerNo())&&w.getWorkStatus()==0));
    }
    @Test void rejectsBindingNonWorkerUser() {
        SysUserMapper users=mock(SysUserMapper.class);RepairWorkerMapper workers=mock(RepairWorkerMapper.class);SysUser student=new SysUser();
        student.setId(1L);student.setRoleType(1);student.setStatus(1);student.setDeleted(0);when(users.selectById(1L)).thenReturn(student);
        WorkerService service=new WorkerService(users,workers,mock(PasswordEncoder.class));
        assertThatThrownBy(()->service.create(new WorkerCreateRequest(1L,null,null,null,null,"W001",null)))
            .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo(ResultCodeEnum.WORKER_USER_INVALID.getCode()));
    }
    @Test void skillSaveRestoresExistingAndDisablesUnselected() {
        RepairWorkerMapper workers=mock(RepairWorkerMapper.class);RepairFaultTypeMapper faults=mock(RepairFaultTypeMapper.class);
        RepairWorkerFaultTypeMapper relations=mock(RepairWorkerFaultTypeMapper.class);RepairAreaMapper areas=mock(RepairAreaMapper.class);RepairWorkerAreaScopeMapper scopes=mock(RepairWorkerAreaScopeMapper.class);
        RepairWorker worker=new RepairWorker();worker.setId(5L);worker.setWorkStatus(0);worker.setDeleted(0);when(workers.selectById(5L)).thenReturn(worker);
        RepairFaultType fault=new RepairFaultType();fault.setId(9L);fault.setStatus(1);when(faults.selectById(9L)).thenReturn(fault);
        RepairWorkerFaultType old=new RepairWorkerFaultType();old.setId(20L);old.setWorkerId(5L);old.setFaultTypeId(8L);old.setStatus(1);when(relations.selectByWorkerIdForUpdate(5L)).thenReturn(List.of(old));
        WorkerConfigurationService service=new WorkerConfigurationService(workers,faults,relations,areas,scopes);service.saveFaultTypes(5L,List.of(9L));
        verify(relations).updateStatus(20L,0);verify(relations).insert(argThat(v->v.getFaultTypeId()==9L&&v.getStatus()==1));
    }
    @Test void adminCanReadConfigurationOfDisabledWorker() {
        RepairWorkerMapper workers=mock(RepairWorkerMapper.class);RepairFaultTypeMapper faults=mock(RepairFaultTypeMapper.class);
        RepairWorkerFaultTypeMapper relations=mock(RepairWorkerFaultTypeMapper.class);RepairAreaMapper areas=mock(RepairAreaMapper.class);RepairWorkerAreaScopeMapper scopes=mock(RepairWorkerAreaScopeMapper.class);
        RepairWorker worker=new RepairWorker();worker.setId(5L);worker.setWorkStatus(2);worker.setDeleted(0);when(workers.selectById(5L)).thenReturn(worker);
        WorkerConfigurationService service=new WorkerConfigurationService(workers,faults,relations,areas,scopes);
        assertThat(service.faultTypes(5L)).isEmpty();
        verify(relations).selectByWorkerId(5L);
    }
    @Test void scopeRejectsBuildingOutsideArea() {
        RepairWorkerMapper workers=mock(RepairWorkerMapper.class);RepairFaultTypeMapper faults=mock(RepairFaultTypeMapper.class);
        RepairWorkerFaultTypeMapper relations=mock(RepairWorkerFaultTypeMapper.class);RepairAreaMapper areas=mock(RepairAreaMapper.class);RepairWorkerAreaScopeMapper scopes=mock(RepairWorkerAreaScopeMapper.class);
        RepairWorker worker=new RepairWorker();worker.setId(5L);worker.setWorkStatus(0);worker.setDeleted(0);when(workers.selectById(5L)).thenReturn(worker);
        when(areas.selectById(1L)).thenReturn(area(1L,0L,1));when(areas.selectById(2L)).thenReturn(area(2L,1L,2));when(areas.selectById(3L)).thenReturn(area(3L,99L,3));
        WorkerConfigurationService service=new WorkerConfigurationService(workers,faults,relations,areas,scopes);
        assertThatThrownBy(()->service.saveAreaScopes(5L,List.of(new AreaScopeItem(1L,2L,3L))))
            .isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo(ResultCodeEnum.WORKER_SCOPE_INVALID.getCode()));
    }
    private RepairArea area(Long id,Long parent,Integer type){RepairArea v=new RepairArea();v.setId(id);v.setParentId(parent);v.setAreaType(type);v.setStatus(1);v.setDeleted(0);return v;}
}
