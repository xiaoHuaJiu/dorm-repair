package com.dormrepair.reminder.task;
import com.dormrepair.reminder.service.RepairReminderService;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.scheduling.annotation.Scheduled;import org.springframework.stereotype.Component;import java.time.LocalDateTime;
@Component @ConditionalOnProperty(prefix="app.task",name="enabled",havingValue="true",matchIfMissing=true)
public class RepairAcceptTimeoutTask {private final RepairReminderService service;public RepairAcceptTimeoutTask(RepairReminderService service){this.service=service;}@Scheduled(cron="${app.task.timeout-scan-cron:0 * * * * ?}")public void scan(){service.processAcceptTimeouts(LocalDateTime.now());}}
