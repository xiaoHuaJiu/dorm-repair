package com.dormrepair.database;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MapperXmlContractTest {
    static final Map<String, String> TABLES = Map.ofEntries(
        Map.entry("sys_user", "SysUser"), Map.entry("repair_area", "RepairArea"),
        Map.entry("repair_worker", "RepairWorker"), Map.entry("repair_fault_type", "RepairFaultType"),
        Map.entry("repair_worker_fault_type", "RepairWorkerFaultType"),
        Map.entry("repair_worker_area_scope", "RepairWorkerAreaScope"),
        Map.entry("repair_work_schedule", "RepairWorkSchedule"), Map.entry("repair_order", "RepairOrder"),
        Map.entry("repair_process_record", "RepairProcessRecord"),
        Map.entry("repair_material_usage", "RepairMaterialUsage"),
        Map.entry("repair_transfer_request", "RepairTransferRequest"),
        Map.entry("repair_leave_request", "RepairLeaveRequest"),
        Map.entry("repair_rework_record", "RepairReworkRecord"),
        Map.entry("repair_evaluation", "RepairEvaluation"), Map.entry("repair_order_flow", "RepairOrderFlow"),
        Map.entry("repair_reminder_record", "RepairReminderRecord"),
        Map.entry("repair_holiday_calendar", "RepairHolidayCalendar"),
        Map.entry("repair_dispatch_alert", "RepairDispatchAlert"),
        Map.entry("sys_idempotent_record", "SysIdempotentRecord"),
        Map.entry("sys_operation_log", "SysOperationLog")
    );

    @Test
    void everyTableHasMatchingMapperAndCompleteXmlContract() throws Exception {
        for (var entry : TABLES.entrySet()) {
            String mapperName = entry.getValue() + "Mapper";
            Class<?> mapper = Class.forName("com.dormrepair.domain.mapper." + mapperName);
            assertThat(mapper.getMethod("selectById", Long.class).getReturnType().getSimpleName())
                .isEqualTo(entry.getValue());

            Path xmlPath = Path.of("src", "main", "resources", "mapper", mapperName + ".xml");
            String xml = Files.readString(xmlPath);
            assertThat(xml)
                .contains("namespace=\"com.dormrepair.domain.mapper." + mapperName + "\"")
                .contains("id=\"BaseResultMap\"")
                .contains("id=\"Base_Column_List\"")
                .contains("id=\"selectById\"")
                .contains("FROM " + entry.getKey())
                .contains("WHERE id = #{id}");
        }
    }
}
