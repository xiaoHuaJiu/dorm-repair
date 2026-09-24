package com.dormrepair.order;

import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.RepairOrder;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.dto.*;
import com.dormrepair.order.service.*;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.file.service.FileService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WorkerRepairOrderCommandServiceTest {
    private RepairOrderMapper orders; private RepairProcessRecordMapper processes; private RepairMaterialUsageMapper materials;
    private RepairOrderFlowMapper flows; private SysOperationLogMapper logs; private CurrentWorkerResolver resolver;
    private RepairReworkRecordMapper reworks; private FileService fileService;
    private WorkerRepairOrderCommandService service;

    @BeforeEach void setUp() {
        orders=mock(RepairOrderMapper.class); processes=mock(RepairProcessRecordMapper.class); materials=mock(RepairMaterialUsageMapper.class);
        flows=mock(RepairOrderFlowMapper.class); logs=mock(SysOperationLogMapper.class); resolver=mock(CurrentWorkerResolver.class); reworks=mock(RepairReworkRecordMapper.class); fileService=mock(FileService.class);
        doAnswer(inv->{((com.dormrepair.domain.entity.RepairProcessRecord)inv.getArgument(0)).setId(88L);return 1;}).when(processes).insert(any());
        service=new WorkerRepairOrderCommandService(orders,processes,materials,flows,logs,resolver,reworks,fileService);
        LoginUser user=new LoginUser(20L,"worker01","维修员",UserRoleEnum.WORKER);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
        when(resolver.resolveCurrentWorkerId()).thenReturn(10L);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void acceptShouldWriteStatusFlowAndLog() {
        RepairOrder order=order(1,10L); order.setAcceptDeadline(LocalDateTime.now().plusMinutes(10));
        when(orders.selectByIdForUpdate(1L)).thenReturn(order); when(orders.casAccept(eq(1L),eq(10L),any(),isNull(),any())).thenReturn(1);
        service.accept(1L,new AcceptRepairOrderRequest(null));
        verify(orders).casAccept(eq(1L),eq(10L),any(),isNull(),any()); verify(flows).insert(any()); verify(logs).insert(any());
    }
    @Test void nonAssigneeShouldBeForbidden() {
        when(orders.selectByIdForUpdate(1L)).thenReturn(order(2,99L));
        BusinessException ex=assertThrows(BusinessException.class,()->service.addProcess(1L,new AddRepairProcessRequest("检查",null)));
        assertEquals(17002,ex.getCode()); verifyNoInteractions(processes);
    }
    @Test void interruptedOrderCannotAddMaterial() {
        when(orders.selectByIdForUpdate(1L)).thenReturn(order(5,10L));
        BusinessException ex=assertThrows(BusinessException.class,()->service.addMaterial(1L,new AddMaterialUsageRequest("灯泡",null,new BigDecimal("1"),"个",null)));
        assertEquals(10005,ex.getCode()); verifyNoInteractions(materials);
    }
    @Test void submitResultShouldPersistRecordAndTransition() {
        RepairOrder order=order(4,10L); order.setReworkCount(1); when(orders.selectByIdForUpdate(1L)).thenReturn(order); when(orders.casSubmitResult(eq(1L),eq(10L),any())).thenReturn(1); when(reworks.completeCurrent(eq(1L),eq(1),any())).thenReturn(1);
        service.submitResult(1L,new SubmitRepairResultRequest("维修完成",List.of(99L)));
        verify(orders).casSubmitResult(eq(1L),eq(10L),any()); verify(processes).insert(argThat(r->r.getRecordType()==4 && r.getImageUrls()==null)); verify(fileService).bindProcessFiles(88L,List.of(99L),20L); verify(flows).insert(any());
        verify(reworks).completeCurrent(eq(1L),eq(1),any());
    }
    private RepairOrder order(int status,Long workerId) { RepairOrder o=new RepairOrder(); o.setId(1L); o.setStatus(status); o.setCurrentAssigneeId(workerId); o.setDeleted(0); return o; }
}
