package com.dormrepair.leave.job;

import com.dormrepair.leave.service.LeaveEffectiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 请假正式生效定时任务：扫描已通过、已到开始时间且尚未结束的请假，
 * 将维修人员置为请假中并对其未完成工单逐单重新派单。
 */
@Component
public class RepairLeaveEffectiveTask {
    private static final Logger log = LoggerFactory.getLogger(RepairLeaveEffectiveTask.class);
    private final LeaveEffectiveService service;

    public RepairLeaveEffectiveTask(LeaveEffectiveService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${app.leave.effective-delay-ms:60000}",
        initialDelayString = "${app.leave.effective-initial-delay-ms:30000}")
    public void run() {
        try {
            service.processDueLeaves();
        } catch (RuntimeException e) {
            log.error("请假生效定时任务执行异常", e);
        }
    }
}
