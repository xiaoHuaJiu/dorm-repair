package com.dormrepair.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="app.dispatch") public class DispatchProperties {
 private int corePoolSize=2,maxPoolSize=4,queueCapacity=200,recoveryBatchSize=100; private long recoveryDelayMs=60000,recoveryInitialDelayMs=60000,staleMinutes=1;
 public int getCorePoolSize(){return corePoolSize;} public void setCorePoolSize(int v){corePoolSize=v;}
 public int getMaxPoolSize(){return maxPoolSize;} public void setMaxPoolSize(int v){maxPoolSize=v;}
 public int getQueueCapacity(){return queueCapacity;} public void setQueueCapacity(int v){queueCapacity=v;}
 public int getRecoveryBatchSize(){return recoveryBatchSize;} public void setRecoveryBatchSize(int v){recoveryBatchSize=v;}
 public long getRecoveryDelayMs(){return recoveryDelayMs;} public void setRecoveryDelayMs(long v){recoveryDelayMs=v;}
 public long getRecoveryInitialDelayMs(){return recoveryInitialDelayMs;} public void setRecoveryInitialDelayMs(long v){recoveryInitialDelayMs=v;}
 public long getStaleMinutes(){return staleMinutes;} public void setStaleMinutes(long v){staleMinutes=v;}
}
