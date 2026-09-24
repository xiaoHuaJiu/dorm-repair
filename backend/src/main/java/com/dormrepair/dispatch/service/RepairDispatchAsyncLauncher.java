package com.dormrepair.dispatch.service;
import com.dormrepair.dispatch.model.*; import org.slf4j.*; import org.springframework.beans.factory.annotation.Qualifier; import org.springframework.stereotype.Service;
import java.util.Set; import java.util.concurrent.*;
@Service public class RepairDispatchAsyncLauncher {
 private static final Logger log=LoggerFactory.getLogger(RepairDispatchAsyncLauncher.class);
 private final Executor executor; private final RepairDispatchService dispatch; private final DispatchAlertService alerts;
 public RepairDispatchAsyncLauncher(@Qualifier("dispatchTaskExecutor") Executor executor,RepairDispatchService dispatch,DispatchAlertService alerts){this.executor=executor;this.dispatch=dispatch;this.alerts=alerts;}
 public void submit(Long orderId,DispatchSourceType source){
  try{executor.execute(()->run(orderId,source));}catch(RejectedExecutionException e){log.error("派单线程池拒绝任务，orderId={}",orderId,e);alerts.record(orderId,source,DispatchFailureReason.SYSTEM_ERROR,"派单线程池拒绝任务");}
 }
 private void run(Long orderId,DispatchSourceType source){try{dispatch.dispatch(orderId,Set.of(),source);}catch(RuntimeException e){log.error("异步派单异常，orderId={}",orderId,e);alerts.record(orderId,source,DispatchFailureReason.SYSTEM_ERROR,e.getMessage());}}
}
