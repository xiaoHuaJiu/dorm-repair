package com.dormrepair.database;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EntitySchemaConsistencyTest {
    private static final Map<String, String> ENTITIES = Map.ofEntries(
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
    void everyEntityExactlyMatchesDatabaseColumnsAndTypes() throws Exception {
        String port = env("DB_PORT", "3307");
        String database = env("DB_NAME", "dorm_repair");
        String url = "jdbc:mysql://localhost:" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (var connection = DriverManager.getConnection(url, required("DB_USER"), required("DB_PASSWORD"))) {
            for (var entry : ENTITIES.entrySet()) {
                Map<String, Class<?>> expected = new LinkedHashMap<>();
                try (var statement = connection.prepareStatement("""
                    SELECT column_name, data_type FROM information_schema.columns
                    WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position
                    """)) {
                    statement.setString(1, database);
                    statement.setString(2, entry.getKey());
                    try (var rows = statement.executeQuery()) {
                        while (rows.next()) expected.put(toCamel(rows.getString(1)), javaType(rows.getString(2)));
                    }
                }
                Class<?> entity = Class.forName("com.dormrepair.domain.entity." + entry.getValue());
                Map<String, Class<?>> actual = new HashMap<>();
                for (Field field : entity.getDeclaredFields()) actual.put(field.getName(), field.getType());
                assertThat(actual).as(entry.getKey()).containsExactlyInAnyOrderEntriesOf(expected);
            }
        }
    }

    private static Class<?> javaType(String dataType) {
        return switch (dataType.toLowerCase()) {
            case "bigint" -> Long.class;
            case "tinyint", "int" -> Integer.class;
            case "varchar", "char", "text", "longtext", "json" -> String.class;
            case "date" -> LocalDate.class;
            case "time" -> LocalTime.class;
            case "datetime", "timestamp" -> LocalDateTime.class;
            case "decimal" -> BigDecimal.class;
            default -> throw new IllegalArgumentException("未登记的数据库类型：" + dataType);
        };
    }

    private static String toCamel(String value) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char ch : value.toCharArray()) {
            if (ch == '_') upper = true;
            else { result.append(upper ? Character.toUpperCase(ch) : ch); upper = false; }
        }
        return result.toString();
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("缺少测试环境变量：" + name);
        return value;
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
