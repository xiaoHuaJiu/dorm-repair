package com.dormrepair.config;
import org.springframework.context.annotation.*; import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
@Configuration public class DispatchAsyncConfig {
 @Bean("dispatchTaskExecutor") public Executor dispatchTaskExecutor(DispatchProperties p){
  ThreadPoolTaskExecutor e=new ThreadPoolTaskExecutor();e.setCorePoolSize(p.getCorePoolSize());e.setMaxPoolSize(p.getMaxPoolSize());e.setQueueCapacity(p.getQueueCapacity());e.setThreadNamePrefix("repair-dispatch-");e.setWaitForTasksToCompleteOnShutdown(true);e.setAwaitTerminationSeconds(30);e.initialize();return e;
 }
}
