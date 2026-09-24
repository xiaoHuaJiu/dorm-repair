package com.dormrepair.database;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaSqlContractTest {
    private static final Path SCHEMA = Path.of("..", "doc", "database", "dorm_repair_schema.sql");
    private static final Set<String> TABLES = Set.of(
        "sys_user", "repair_area", "repair_worker", "repair_fault_type",
        "repair_worker_fault_type", "repair_worker_area_scope", "repair_work_schedule",
        "repair_order", "repair_process_record", "repair_material_usage",
        "repair_transfer_request", "repair_leave_request", "repair_rework_record",
        "repair_evaluation", "repair_order_flow", "repair_reminder_record", "repair_holiday_calendar",
        "repair_dispatch_alert", "repair_order_alert", "sys_idempotent_record", "sys_operation_log"
    );

    @Test
    void schemaContainsExactlyTheTwentyOneSafeFormalTables() throws IOException {
        String sql = Files.readString(SCHEMA);
        Matcher matcher = Pattern.compile("(?i)CREATE\\s+TABLE\\s+IF\\s+NOT\\s+EXISTS\\s+`?([a-z_]+)`?").matcher(sql);
        java.util.Set<String> actual = new java.util.HashSet<>();
        while (matcher.find()) actual.add(matcher.group(1));

        assertThat(actual).containsExactlyInAnyOrderElementsOf(TABLES);
        assertThat(sql).doesNotContainIgnoringCase("DROP TABLE", "DROP DATABASE", "FOREIGN KEY");
        assertThat(Pattern.compile("(?i)ENGINE\\s*=\\s*InnoDB").matcher(sql).results()).hasSize(21);
        assertThat(Pattern.compile("(?i)(DEFAULT\\s+)?CHARSET\\s*=\\s*utf8mb4").matcher(sql).results()).hasSize(21);
        assertThat(Pattern.compile("(?i)INSERT\\s+INTO\\s+repair_holiday_calendar").matcher(sql).results()).hasSize(39);
    }

    @Test
    void schemaContainsRequiredBusinessIndexes() throws IOException {
        String sql = Files.readString(SCHEMA);
        assertThat(sql).contains(
            "idx_order_duplicate_check", "idx_order_assignee_status", "idx_order_status_report",
            "idx_order_accept_timeout", "idx_order_complete_timeout", "idx_order_exception",
            "idx_order_duplicate_relation", "uk_worker_fault", "idx_fault_worker",
            "idx_scope_worker", "idx_scope_location", "idx_schedule_effective",
            "idx_leave_worker_time", "idx_leave_pending_review", "idx_leave_start_task",
            "idx_leave_end_task", "idx_process_order_time", "idx_process_worker_time",
            "idx_process_order_type", "idx_material_order_time", "idx_material_worker_time",
            "uk_rework_order_no", "idx_rework_order_time", "idx_rework_admin",
            "idx_flow_order_time", "uk_evaluation_order", "uk_reminder_deduplicate", "uk_idempotent_biz_user_no",
            "uk_holiday_date", "idx_holiday_year", "uk_dispatch_alert_open", "idx_dispatch_alert_status_time",
            "uk_order_alert_rework_type", "idx_order_alert_status_time"
        );
    }
}
