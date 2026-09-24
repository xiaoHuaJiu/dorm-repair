package com.dormrepair.leave.job;

import com.dormrepair.leave.service.LeaveEndService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 请假结束定时任务：扫描已到结束时间的已通过请假，
 * 在不存在其他生效请假时把维修人员从请假中恢复为正常。
 */
@Component
public class RepairLeaveEndTask {
    private static final Logger log = LoggerFactory.getLogger(RepairLeaveEndTask.class);
    private final LeaveEndService service;

    public RepairLeaveEndTask(LeaveEndService service) { this.service = service; }

    @Scheduled(fixedDelayString = "${app.leave.end-delay-ms:60000}",
        initialDelayString = "${app.leave.end-initial-delay-ms:60000}")
    public void run() {
        try {
            service.processEndedLeaves();
        } catch (RuntimeException e) {
            log.error("请假结束定时任务执行异常", e);
        }
    }
}
