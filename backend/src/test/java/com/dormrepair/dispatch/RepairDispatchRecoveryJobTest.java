package com.dormrepair.dispatch;
import com.dormrepair.dispatch.job.RepairDispatchRecoveryJob; import com.dormrepair.dispatch.model.DispatchSourceType; import com.dormrepair.dispatch.service.RepairDispatchAsyncLauncher; import com.dormrepair.domain.mapper.RepairOrderMapper;
import org.junit.jupiter.api.Test; import java.time.LocalDateTime; import java.util.List;
import static org.mockito.Mockito.*;
class RepairDispatchRecoveryJobTest {
 @Test void submitsEveryStalePendingOrder(){
  RepairOrderMapper orders=mock(RepairOrderMapper.class); RepairDispatchAsyncLauncher launcher=mock(RepairDispatchAsyncLauncher.class);
  when(orders.selectPendingDispatchIds(any(LocalDateTime.class),eq(100))).thenReturn(List.of(11L,12L));
  new RepairDispatchRecoveryJob(orders,launcher).recover();
  verify(launcher).submit(11L,DispatchSourceType.INITIAL_REPORT); verify(launcher).submit(12L,DispatchSourceType.INITIAL_REPORT);
 }
}
