package com.dormrepair.config;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 请假生效与结束定时任务配置。
 */
@ConfigurationProperties(prefix = "app.leave")
public class LeaveProperties {
    private long effectiveDelayMs = 60000;
    private long effectiveInitialDelayMs = 30000;
    private long endDelayMs = 60000;
    private long endInitialDelayMs = 60000;
    private int effectiveBatchSize = 100;
    private int endBatchSize = 100;
    /** 处理中任务超时阈值（分钟），超过则视为异常中断，允许恢复执行。 */
    private long staleMinutes = 5;
    public long getEffectiveDelayMs() { return effectiveDelayMs; }
    public void setEffectiveDelayMs(long v) { effectiveDelayMs = v; }
    public long getEffectiveInitialDelayMs() { return effectiveInitialDelayMs; }
    public void setEffectiveInitialDelayMs(long v) { effectiveInitialDelayMs = v; }
    public long getEndDelayMs() { return endDelayMs; }
    public void setEndDelayMs(long v) { endDelayMs = v; }
    public long getEndInitialDelayMs() { return endInitialDelayMs; }
    public void setEndInitialDelayMs(long v) { endInitialDelayMs = v; }
    public int getEffectiveBatchSize() { return effectiveBatchSize; }
    public void setEffectiveBatchSize(int v) { effectiveBatchSize = v; }
    public int getEndBatchSize() { return endBatchSize; }
    public void setEndBatchSize(int v) { endBatchSize = v; }
    public long getStaleMinutes() { return staleMinutes; }
    public void setStaleMinutes(long v) { staleMinutes = v; }
}
