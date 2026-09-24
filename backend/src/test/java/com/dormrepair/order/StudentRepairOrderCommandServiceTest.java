package com.dormrepair.order;
import com.dormrepair.common.enums.UserRoleEnum;
import com.dormrepair.common.exception.BusinessException;
import com.dormrepair.domain.entity.*;
import com.dormrepair.domain.mapper.*;
import com.dormrepair.order.dto.*;
import com.dormrepair.order.service.StudentRepairOrderTransactionService;
import com.dormrepair.security.model.LoginUser;
import com.dormrepair.file.service.FileService;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class StudentRepairOrderCommandServiceTest {
  RepairOrderMapper orders; RepairEvaluationMapper evaluations; RepairReworkRecordMapper reworks; RepairOrderAlertMapper alerts; RepairOrderFlowMapper flows; SysOperationLogMapper logs; FileService fileService; StudentRepairOrderTransactionService service;
  @BeforeEach void setup(){orders=mock(RepairOrderMapper.class);evaluations=mock(RepairEvaluationMapper.class);reworks=mock(RepairReworkRecordMapper.class);alerts=mock(RepairOrderAlertMapper.class);flows=mock(RepairOrderFlowMapper.class);logs=mock(SysOperationLogMapper.class);fileService=mock(FileService.class);doAnswer(inv->{((RepairReworkRecord)inv.getArgument(0)).setId(66L);return 1;}).when(reworks).insert(any());service=new StudentRepairOrderTransactionService(orders,evaluations,reworks,alerts,flows,logs,fileService);LoginUser u=new LoginUser(7L,"student01","学生",UserRoleEnum.STUDENT);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u,null,List.of()));}
  @AfterEach void clear(){SecurityContextHolder.clearContext();}
  @Test void confirmOwnPendingOrder(){RepairOrder o=order(3,7L,2L,0);when(orders.selectByIdForUpdate(1L)).thenReturn(o);when(orders.casStudentConfirm(eq(1L),eq(7L),any())).thenReturn(1);service.confirm(1L);verify(flows).insert(argThat(f->f.getFromStatus()==3&&f.getToStatus()==6));verify(logs).insert(any());}
  @Test void denyOtherStudent(){when(orders.selectByIdForUpdate(1L)).thenReturn(order(3,8L,2L,0));BusinessException e=assertThrows(BusinessException.class,()->service.confirm(1L));assertEquals(17002,e.getCode());}
  @Test void evaluateOnceAndNormalizeBlank(){RepairOrder o=order(6,7L,2L,0);when(orders.selectByIdForUpdate(1L)).thenReturn(o);service.evaluate(1L,new CreateRepairEvaluationRequest(5,"  "));verify(evaluations).insert(argThat(e->e.getContent()==null&&e.getWorkerId()==2L));}
  @Test void evaluationRequiresAssignee(){when(orders.selectByIdForUpdate(1L)).thenReturn(order(6,7L,null,0));BusinessException e=assertThrows(BusinessException.class,()->service.evaluate(1L,new CreateRepairEvaluationRequest(5,null)));assertEquals(10005,e.getCode());}
  @Test void secondReworkCreatesWarning(){RepairOrder before=order(3,7L,2L,1),after=order(4,7L,2L,2);when(orders.selectByIdForUpdate(1L)).thenReturn(before,after);when(orders.casStudentRework(1L,7L)).thenReturn(1);service.rework(1L,new CreateReworkRequest("仍未修好",List.of(77L)));verify(reworks).insert(argThat(r->r.getReworkNo()==2&&r.getImageUrls()==null));verify(fileService).bindReworkFiles(66L,List.of(77L),7L);verify(alerts).insertIgnore(argThat(a->a.getAlertLevel()==2));}
  private RepairOrder order(int status,Long uid,Long worker,int count){RepairOrder o=new RepairOrder();o.setId(1L);o.setStatus(status);o.setStudentUid(uid);o.setCurrentAssigneeId(worker);o.setReworkCount(count);o.setDeleted(0);return o;}
}
