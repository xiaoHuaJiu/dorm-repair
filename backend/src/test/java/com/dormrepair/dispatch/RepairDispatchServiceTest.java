package com.dormrepair.dispatch;

import com.dormrepair.dispatch.model.*; import com.dormrepair.dispatch.service.*;
import com.dormrepair.domain.entity.RepairOrder; import com.dormrepair.domain.mapper.*;
import org.junit.jupiter.api.Test; import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;

class RepairDispatchServiceTest {
 @Test void selectsLowestWorkloadThenLowestWorkerId(){
  RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairWorkerMapper workers=mock(RepairWorkerMapper.class);
  WorkingTimeCalculator time=mock(WorkingTimeCalculator.class); RepairDispatchTransactionService tx=mock(RepairDispatchTransactionService.class); DispatchAlertService alerts=mock(DispatchAlertService.class);
  RepairOrder order=order(); when(orders.selectById(10L)).thenReturn(order); when(workers.countSkillWorkers(9L,List.of())).thenReturn(2);
  when(workers.countAreaWorkers(9L,1L,2L,3L,List.of())).thenReturn(2);
  when(workers.selectDispatchCandidates(eq(9L),eq(1L),eq(2L),eq(3L),any(),eq(List.of()))).thenReturn(List.of(new DispatchCandidate(8L,1),new DispatchCandidate(7L,1)));
  LocalDateTime deadline=LocalDateTime.of(2026,9,24,10,30); when(time.calculateDeadline(any(),eq(Duration.ofMinutes(30)))).thenReturn(deadline); when(tx.assign(any(),eq(7L),any(),eq(deadline),eq(DispatchSourceType.INITIAL_REPORT))).thenReturn(true);
  DispatchResult result=new RepairDispatchService(orders,workers,time,tx,alerts).dispatch(10L,Set.of(),DispatchSourceType.INITIAL_REPORT);
  assertThat(result.success()).isTrue(); assertThat(result.workerId()).isEqualTo(7L);
 }
 @Test void recordsSpecificFailureWhenNoSkillWorker(){
  RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairWorkerMapper workers=mock(RepairWorkerMapper.class);
  WorkingTimeCalculator time=mock(WorkingTimeCalculator.class); RepairDispatchTransactionService tx=mock(RepairDispatchTransactionService.class); DispatchAlertService alerts=mock(DispatchAlertService.class);
  when(orders.selectById(10L)).thenReturn(order()); when(workers.countSkillWorkers(9L,List.of())).thenReturn(0); when(tx.markFailed(any(),eq(DispatchFailureReason.NO_SKILL_WORKER),eq(DispatchSourceType.INITIAL_REPORT))).thenReturn(true);
  DispatchResult result=new RepairDispatchService(orders,workers,time,tx,alerts).dispatch(10L,Set.of(),DispatchSourceType.INITIAL_REPORT);
  assertThat(result.failureReason()).isEqualTo(DispatchFailureReason.NO_SKILL_WORKER); verify(alerts).record(10L,DispatchSourceType.INITIAL_REPORT,DispatchFailureReason.NO_SKILL_WORKER,"没有匹配故障类型的维修人员");
 }
 private RepairOrder order(){RepairOrder o=new RepairOrder();o.setId(10L);o.setStatus(0);o.setFaultTypeId(9L);o.setCampusId(1L);o.setAreaId(2L);o.setBuildingId(3L);return o;}
}
