CREATE TABLE IF NOT EXISTS repair_order_alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '工单ID',
    rework_no INT NOT NULL COMMENT '返工轮次',
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    alert_level TINYINT NOT NULL COMMENT '1普通 2中等 3严重',
    alert_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
    alert_content VARCHAR(500) NOT NULL COMMENT '告警内容',
    handled_by BIGINT DEFAULT NULL,
    handled_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_alert_rework_type (order_id, rework_no, alert_type),
    KEY idx_order_alert_status_time (alert_status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单业务告警表';
