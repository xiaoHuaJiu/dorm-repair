USE dorm_repair;

CREATE TABLE IF NOT EXISTS repair_holiday_calendar (
 id BIGINT NOT NULL AUTO_INCREMENT, holiday_date DATE NOT NULL, holiday_name VARCHAR(50) NOT NULL,
 day_type TINYINT UNSIGNED NOT NULL, year INT NOT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_holiday_date(holiday_date), KEY idx_holiday_year(year,day_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法定节假日及调休配置表';

CREATE TABLE IF NOT EXISTS repair_dispatch_alert (
 id BIGINT NOT NULL AUTO_INCREMENT, order_id BIGINT NOT NULL, source_type TINYINT NOT NULL,
 failure_reason VARCHAR(50) NOT NULL, alert_status TINYINT NOT NULL DEFAULT 0,
 failure_detail VARCHAR(500) DEFAULT NULL, occurrence_count INT NOT NULL DEFAULT 1,
 first_occurred_time DATETIME NOT NULL, last_occurred_time DATETIME NOT NULL,
 handled_by BIGINT DEFAULT NULL, handled_time DATETIME DEFAULT NULL,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_dispatch_alert_open(order_id,source_type,failure_reason,alert_status),
 KEY idx_dispatch_alert_status_time(alert_status,last_occurred_time), KEY idx_dispatch_alert_order(order_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动派单失败告警表';

-- 2026 年 39 条数据统一维护在 dorm_repair_schema.sql；升级时执行该主结构文件，全部 DDL 和数据写入均可安全重复执行。
