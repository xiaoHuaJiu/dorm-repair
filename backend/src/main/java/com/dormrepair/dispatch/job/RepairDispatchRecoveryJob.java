package com.dormrepair.dispatch.job;
import com.dormrepair.dispatch.model.DispatchSourceType; import com.dormrepair.dispatch.service.RepairDispatchAsyncLauncher; import com.dormrepair.domain.mapper.RepairOrderMapper;
import org.springframework.scheduling.annotation.Scheduled; import org.springframework.stereotype.Component; import java.time.LocalDateTime; import com.dormrepair.config.DispatchProperties; import org.springframework.beans.factory.annotation.Autowired;
@Component public class RepairDispatchRecoveryJob {
 private final RepairOrderMapper orders; private final RepairDispatchAsyncLauncher launcher; private final DispatchProperties properties;
 public RepairDispatchRecoveryJob(RepairOrderMapper orders,RepairDispatchAsyncLauncher launcher){this(orders,launcher,new DispatchProperties());}
 @Autowired public RepairDispatchRecoveryJob(RepairOrderMapper orders,RepairDispatchAsyncLauncher launcher,DispatchProperties properties){this.orders=orders;this.launcher=launcher;this.properties=properties;}
 @Scheduled(fixedDelayString="${app.dispatch.recovery-delay-ms:60000}",initialDelayString="${app.dispatch.recovery-initial-delay-ms:60000}")
 public void recover(){for(Long id:orders.selectPendingDispatchIds(LocalDateTime.now().minusMinutes(properties.getStaleMinutes()),properties.getRecoveryBatchSize()))launcher.submit(id,DispatchSourceType.INITIAL_REPORT);}
}
