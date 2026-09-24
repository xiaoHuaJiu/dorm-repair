package com.dormrepair.dispatch.event;
import com.dormrepair.dispatch.model.DispatchSourceType; import com.dormrepair.dispatch.service.RepairDispatchAsyncLauncher;
import org.springframework.stereotype.Component; import org.springframework.transaction.event.*;
@Component public class RepairOrderCreatedListener {
 private final RepairDispatchAsyncLauncher launcher; public RepairOrderCreatedListener(RepairDispatchAsyncLauncher launcher){this.launcher=launcher;}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) public void onCreated(RepairOrderCreatedEvent event){launcher.submit(event.orderId(),DispatchSourceType.INITIAL_REPORT);}
}
