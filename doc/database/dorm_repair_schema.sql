-- 从《数据库DDL及索引设计.md》机械提取，字段和索引以该文档为准。
USE dorm_repair;

CREATE TABLE IF NOT EXISTS sys_user (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username            VARCHAR(100)    NOT NULL COMMENT '登录账号',
    password            VARCHAR(255)    NOT NULL COMMENT '加密密码',
    real_name           VARCHAR(100)    DEFAULT NULL COMMENT '真实姓名',
    phone               VARCHAR(30)     DEFAULT NULL COMMENT '联系电话',

    role_type           TINYINT         NOT NULL COMMENT '角色：1学生 2维修人员 3管理员',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '账号状态：0停用 1正常',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',

    PRIMARY KEY (id),

    UNIQUE KEY uk_sys_user_username (username),

    KEY idx_sys_user_role_status (
        role_type,
        status,
        deleted
    ),

    KEY idx_sys_user_phone (phone)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS repair_worker (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '维修人员ID',
    user_id             BIGINT          NOT NULL COMMENT 'sys_user.id',
    worker_no           VARCHAR(50)     DEFAULT NULL COMMENT '维修人员编号',

    work_status         TINYINT         NOT NULL DEFAULT 0
                        COMMENT '工作状态：0正常 1请假中 2停用',

    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_worker_user_id (user_id),

    UNIQUE KEY uk_worker_no (worker_no),

    KEY idx_worker_work_status (
        work_status,
        deleted
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='维修人员业务扩展表';

CREATE TABLE IF NOT EXISTS repair_fault_type (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    type_code           VARCHAR(50)     NOT NULL COMMENT '故障类型编码',
    type_name           VARCHAR(100)    NOT NULL COMMENT '故障类型名称',

    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0停用 1启用',
    sort_no             INT             NOT NULL DEFAULT 0 COMMENT '排序',

    remark              VARCHAR(500)    DEFAULT NULL,

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_fault_type_code (type_code),

    KEY idx_fault_type_status_sort (
        status,
        sort_no
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='故障类型表';

CREATE TABLE IF NOT EXISTS repair_worker_fault_type (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    worker_id           BIGINT          NOT NULL COMMENT '维修人员ID',
    fault_type_id       BIGINT          NOT NULL COMMENT '故障类型ID',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0停用 1启用',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_worker_fault (
        worker_id,
        fault_type_id
    ),

    KEY idx_fault_worker (
        fault_type_id,
        status,
        worker_id
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修人员可维修故障类型关联表';

CREATE TABLE IF NOT EXISTS repair_area (
    id                  BIGINT          NOT NULL AUTO_INCREMENT COMMENT '区域节点ID',

    parent_id           BIGINT          NOT NULL DEFAULT 0
                        COMMENT '父节点ID，根节点为0',

    area_code           VARCHAR(50)     NOT NULL
                        COMMENT '区域编码',

    area_name           VARCHAR(100)    NOT NULL
                        COMMENT '节点名称',

    area_type           TINYINT         NOT NULL
                        COMMENT '节点类型：1单位/校区 2区域 3楼栋 4房间',

    sort_no             INT             NOT NULL DEFAULT 0
                        COMMENT '同级排序',

    status              TINYINT         NOT NULL DEFAULT 1
                        COMMENT '状态：0停用 1启用',

    remark              VARCHAR(500)    DEFAULT NULL COMMENT '备注',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    deleted             TINYINT         NOT NULL DEFAULT 0
                        COMMENT '逻辑删除：0否 1是',

    PRIMARY KEY (id),

    UNIQUE KEY uk_area_code (
        area_code
    ),

    KEY idx_area_parent (
        parent_id,
        status,
        sort_no
    ),

    KEY idx_area_type (
        area_type,
        status,
        deleted
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='维修区域树表：单位-区域-楼栋';

CREATE TABLE IF NOT EXISTS repair_worker_area_scope (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    worker_id           BIGINT          NOT NULL COMMENT '维修人员ID',

    campus_id           BIGINT          NOT NULL COMMENT '校区ID',
    area_id             BIGINT          DEFAULT NULL COMMENT '区域ID，为空代表整个校区',
    building_id         BIGINT          DEFAULT NULL COMMENT '楼栋ID，为空代表整个区域',

    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0停用 1启用',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_scope_worker (
        worker_id,
        status
    ),

    KEY idx_scope_location (
        campus_id,
        area_id,
        building_id,
        status,
        worker_id
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修人员负责区域表';

CREATE TABLE IF NOT EXISTS repair_work_schedule (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    schedule_name       VARCHAR(100)    NOT NULL COMMENT '方案名称',

    start_date          DATE            NOT NULL COMMENT '生效开始日期',
    end_date            DATE            NOT NULL COMMENT '生效结束日期',

    work_start_time     TIME            NOT NULL COMMENT '上班时间',
    work_end_time       TIME            NOT NULL COMMENT '下班时间',

    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '0停用 1启用',

    remark              VARCHAR(500)    DEFAULT NULL,

    create_by           BIGINT          DEFAULT NULL,
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_schedule_effective (
        status,
        start_date,
        end_date
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修工作时间方案表';

CREATE TABLE IF NOT EXISTS repair_order (
    id                          BIGINT          NOT NULL AUTO_INCREMENT,
    order_no                    VARCHAR(32)     NOT NULL COMMENT '工单编号',

    student_uid                 BIGINT          NOT NULL COMMENT '报修学生ID',

    contact_name                VARCHAR(100)    NOT NULL COMMENT '联系人',
    contact_phone               VARCHAR(30)     NOT NULL COMMENT '联系电话',

    campus_id                   BIGINT          NOT NULL,
    area_id                     BIGINT          NOT NULL,
    building_id                 BIGINT          NOT NULL,
    room_id                     BIGINT          NOT NULL,

    location_detail             VARCHAR(255)    DEFAULT NULL COMMENT '具体位置描述',

    fault_type_id               BIGINT          NOT NULL,
    problem_description         VARCHAR(1000)   NOT NULL,

    image_urls                  JSON            DEFAULT NULL COMMENT '报修现场图片',

    status                      TINYINT         NOT NULL DEFAULT 0
                                COMMENT '0待派单 1待接单 2维修中 3待确认 4返工中 5已中断 6已完成 7已取消',

    current_assignee_id         BIGINT          DEFAULT NULL COMMENT '当前维修人员ID',

    dispatch_time               DATETIME        DEFAULT NULL COMMENT '当前轮次派单时间',
    accept_deadline             DATETIME        DEFAULT NULL COMMENT '接单截止时间',
    accept_time                 DATETIME        DEFAULT NULL COMMENT '当前负责人接单时间',

    expected_complete_time      DATETIME        DEFAULT NULL COMMENT '预计完成时间',
    complete_deadline           DATETIME        DEFAULT NULL COMMENT '维修截止时间',

    repair_submit_time          DATETIME        DEFAULT NULL COMMENT '最近一次提交维修结果时间',

    confirm_time                DATETIME        DEFAULT NULL COMMENT '学生确认时间',
    complete_time               DATETIME        DEFAULT NULL COMMENT '最终完成时间',

    rework_count                INT             NOT NULL DEFAULT 0 COMMENT '累计返工次数',

    exception_flag              TINYINT         NOT NULL DEFAULT 0 COMMENT '异常工单：0否 1是',

    duplicate_flag              TINYINT         NOT NULL DEFAULT 0 COMMENT '疑似重复：0否 1是',
    duplicate_order_id          BIGINT          DEFAULT NULL COMMENT '关联疑似重复工单ID',

    report_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',

    cancel_time                 DATETIME        DEFAULT NULL,
    cancel_reason               VARCHAR(500)    DEFAULT NULL,

    create_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time                 DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                                ON UPDATE CURRENT_TIMESTAMP,
    deleted                     TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_repair_order_no (order_no),

    KEY idx_order_student (
        student_uid,
        deleted,
        report_time
    ),

    KEY idx_order_duplicate_check (
        campus_id,
        area_id,
        building_id,
        room_id,
        fault_type_id,
        status,
        report_time
    ),

    KEY idx_order_assignee_status (
        current_assignee_id,
        status,
        deleted
    ),

    KEY idx_order_status_report (
        status,
        report_time
    ),

    KEY idx_order_accept_timeout (
        status,
        accept_deadline
    ),

    KEY idx_order_complete_timeout (
        status,
        complete_deadline
    ),

    KEY idx_order_exception (
        exception_flag,
        status,
        update_time
    ),

    KEY idx_order_duplicate_relation (
        duplicate_order_id
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='维修工单主表';

CREATE TABLE IF NOT EXISTS repair_leave_request (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    worker_id           BIGINT          NOT NULL,

    start_time          DATETIME        NOT NULL,
    end_time            DATETIME        NOT NULL,

    reason              VARCHAR(500)    NOT NULL,

    status              TINYINT         NOT NULL DEFAULT 0
                        COMMENT '0待审批 1已通过 2已驳回 3已撤回',

    review_admin_id     BIGINT          DEFAULT NULL,
    review_remark       VARCHAR(500)    DEFAULT NULL,
    review_time         DATETIME        DEFAULT NULL,

    reassign_status     TINYINT         NOT NULL DEFAULT 0
                        COMMENT '工单转派状态：0未执行 1执行中 2完成 3部分失败',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_leave_worker_time (
        worker_id,
        start_time,
        end_time,
        status
    ),

    KEY idx_leave_pending_review (
        status,
        create_time
    ),

    KEY idx_leave_start_task (
        status,
        reassign_status,
        start_time
    ),

    KEY idx_leave_end_task (
        status,
        end_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修人员请假申请表';

CREATE TABLE IF NOT EXISTS repair_transfer_request (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    order_id                BIGINT          NOT NULL,

    applicant_worker_id     BIGINT          NOT NULL COMMENT '申请转派维修人员',
    original_assignee_id    BIGINT          NOT NULL COMMENT '申请时原负责人',

    reason_type             TINYINT         NOT NULL
                            COMMENT '1范围不符 2技能不足 3当前无法处理 4需要其他工种 5工作量过大 6其他',

    reason_description      VARCHAR(500)    DEFAULT NULL,

    approval_status         TINYINT         NOT NULL DEFAULT 0
                            COMMENT '0待审批 1通过 2驳回 3撤回',

    review_admin_id         BIGINT          DEFAULT NULL,
    review_remark           VARCHAR(500)    DEFAULT NULL,
    review_time             DATETIME        DEFAULT NULL,

    execute_status          TINYINT         NOT NULL DEFAULT 0
                            COMMENT '0未执行 1执行中 2成功 3失败',

    new_assignee_id         BIGINT          DEFAULT NULL,

    execute_time            DATETIME        DEFAULT NULL,

    failure_reason          VARCHAR(500)    DEFAULT NULL,

    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                            ON UPDATE CURRENT_TIMESTAMP,
    deleted                 TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_transfer_order (
        order_id,
        create_time
    ),

    KEY idx_transfer_worker (
        applicant_worker_id,
        approval_status,
        create_time
    ),

    KEY idx_transfer_pending (
        approval_status,
        create_time
    ),

    KEY idx_transfer_execute (
        approval_status,
        execute_status,
        review_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修工单转派申请表';

CREATE TABLE IF NOT EXISTS repair_process_record (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    order_id                BIGINT          NOT NULL,
    worker_id               BIGINT          NOT NULL,

    record_type             TINYINT         NOT NULL
                            COMMENT '1普通维修记录 2维修中断 3恢复维修 4提交维修结果',

    content                 VARCHAR(1000)   NOT NULL,
    image_urls              JSON            DEFAULT NULL,

    interrupt_reason_type   TINYINT         DEFAULT NULL
                            COMMENT '1等待材料 2第三方介入 3现场条件不允许 4其他',

    record_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                 TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_process_order_time (
        order_id,
        record_time
    ),

    KEY idx_process_worker_time (
        worker_id,
        record_time
    ),

    KEY idx_process_order_type (
        order_id,
        record_type,
        record_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修过程记录表';

CREATE TABLE IF NOT EXISTS repair_material_usage (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    order_id            BIGINT          NOT NULL,
    worker_id           BIGINT          NOT NULL,

    material_name       VARCHAR(100)    NOT NULL,
    specification       VARCHAR(100)    DEFAULT NULL,

    quantity            DECIMAL(10,2)   NOT NULL,
    unit                VARCHAR(20)     NOT NULL,

    remark              VARCHAR(500)    DEFAULT NULL,

    use_time            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    KEY idx_material_order_time (
        order_id,
        use_time
    ),

    KEY idx_material_worker_time (
        worker_id,
        use_time
    ),

    KEY idx_material_name (
        material_name
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修材料使用记录表';

CREATE TABLE IF NOT EXISTS repair_rework_record (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    order_id                BIGINT          NOT NULL,

    rework_no               INT             NOT NULL COMMENT '第几次返工',

    applicant_uid           BIGINT          NOT NULL COMMENT '申请学生',

    reason                  VARCHAR(1000)   NOT NULL,
    image_urls              JSON            DEFAULT NULL,

    original_assignee_id    BIGINT          DEFAULT NULL,

    status                  TINYINT         NOT NULL DEFAULT 0
                            COMMENT '0处理中 1已完成',

    admin_intervention      TINYINT         NOT NULL DEFAULT 0
                            COMMENT '0否 1是',

    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finish_time             DATETIME        DEFAULT NULL,

    deleted                 TINYINT         NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    UNIQUE KEY uk_rework_order_no (
        order_id,
        rework_no
    ),

    KEY idx_rework_order_time (
        order_id,
        create_time
    ),

    KEY idx_rework_admin (
        admin_intervention,
        status,
        create_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='工单返工记录表';

CREATE TABLE IF NOT EXISTS repair_order_flow (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,

    order_id                BIGINT          NOT NULL,

    operation_type          TINYINT         NOT NULL COMMENT '工单操作类型',

    from_status             TINYINT         DEFAULT NULL,
    to_status               TINYINT         DEFAULT NULL,

    original_assignee_id    BIGINT          DEFAULT NULL,
    new_assignee_id         BIGINT          DEFAULT NULL,

    operator_id             BIGINT          DEFAULT NULL,
    operator_role           TINYINT         DEFAULT NULL
                            COMMENT '1学生 2维修人员 3管理员 4系统',

    source_type             TINYINT         DEFAULT NULL
                            COMMENT '1正常派单 2接单超时 3转派申请 4人员请假 5管理员操作 6返工 7系统',

    related_business_id     BIGINT          DEFAULT NULL,

    reason                  VARCHAR(500)    DEFAULT NULL,

    operation_time          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_flow_order_time (
        order_id,
        operation_time
    ),

    KEY idx_flow_operator (
        operator_id,
        operation_time
    ),

    KEY idx_flow_assignee_original (
        original_assignee_id,
        operation_time
    ),

    KEY idx_flow_assignee_new (
        new_assignee_id,
        operation_time
    ),

    KEY idx_flow_source_business (
        source_type,
        related_business_id
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='工单生命周期流转记录表';

CREATE TABLE IF NOT EXISTS repair_evaluation (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    order_id            BIGINT          NOT NULL,
    student_uid         BIGINT          NOT NULL,
    worker_id           BIGINT          NOT NULL,

    score               TINYINT         NOT NULL COMMENT '评分，例如1-5',

    content             VARCHAR(1000)   DEFAULT NULL COMMENT '评价内容',

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_evaluation_order (
        order_id
    ),

    KEY idx_evaluation_worker_time (
        worker_id,
        create_time
    ),

    KEY idx_evaluation_student_time (
        student_uid,
        create_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='维修服务评价表';

CREATE TABLE IF NOT EXISTS repair_reminder_record (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    order_id            BIGINT          NOT NULL,

    reminder_type       TINYINT         NOT NULL
                        COMMENT '1接单提醒 2完成提醒',

    reminder_level      TINYINT         NOT NULL
                        COMMENT '1剩余10分钟 2剩余5分钟 3已到期',

    deadline_time       DATETIME        NOT NULL COMMENT '本次提醒对应的截止时间',

    receiver_id         BIGINT          NOT NULL COMMENT '接收人用户ID',

    send_status         TINYINT         NOT NULL DEFAULT 0
                        COMMENT '0待发送 1已发送 2发送失败',

    send_time           DATETIME        DEFAULT NULL,

    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    UNIQUE KEY uk_reminder_deduplicate (
        order_id,
        reminder_type,
        reminder_level,
        deadline_time,
        receiver_id
    ),

    KEY idx_reminder_send (
        send_status,
        create_time
    ),

    KEY idx_reminder_order (
        order_id,
        create_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='工单超时提醒记录表';

CREATE TABLE IF NOT EXISTS sys_idempotent_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    biz_no          VARCHAR(64)  NOT NULL COMMENT '客户端业务提交标识',
    biz_type        VARCHAR(50)  NOT NULL COMMENT '业务类型',
    user_id         BIGINT       NOT NULL COMMENT '当前登录用户ID',
    request_hash    CHAR(64)     NOT NULL COMMENT '标准化请求SHA-256',
    status          TINYINT      NOT NULL DEFAULT 0 COMMENT '0处理中 1成功',
    result_snapshot JSON         DEFAULT NULL COMMENT '成功结果最小快照',
    expire_time     DATETIME     DEFAULT NULL,
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotent_biz_user_no (biz_type,user_id,biz_no),
    KEY idx_idempotent_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口幂等记录表';

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,

    user_id             BIGINT          DEFAULT NULL,
    username            VARCHAR(100)    DEFAULT NULL,
    role_type           TINYINT         DEFAULT NULL,

    module_name         VARCHAR(100)    DEFAULT NULL,
    operation_name      VARCHAR(100)    DEFAULT NULL,

    business_type       VARCHAR(50)     DEFAULT NULL,
    business_id         BIGINT          DEFAULT NULL,

    request_method      VARCHAR(10)     DEFAULT NULL,
    request_uri         VARCHAR(255)    DEFAULT NULL,
    request_params      TEXT            DEFAULT NULL,

    result_status       TINYINT         DEFAULT NULL COMMENT '0失败 1成功',
    error_message       VARCHAR(1000)   DEFAULT NULL,

    ip_address          VARCHAR(64)     DEFAULT NULL,

    operation_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),

    KEY idx_log_user_time (
        user_id,
        operation_time
    ),

    KEY idx_log_business (
        business_type,
        business_id,
        operation_time
    ),

    KEY idx_log_module_time (
        module_name,
        operation_time
    ),

    KEY idx_log_time (
        operation_time
    )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='系统操作日志表';

CREATE TABLE IF NOT EXISTS repair_holiday_calendar (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_date DATE NOT NULL COMMENT '节假日或调休日期',
    holiday_name VARCHAR(50) NOT NULL COMMENT '日期名称',
    day_type TINYINT UNSIGNED NOT NULL COMMENT '1放假日 2调休上班日',
    year INT NOT NULL COMMENT '年份',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (holiday_date),
    KEY idx_holiday_year (year, day_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='法定节假日及调休配置表';

CREATE TABLE IF NOT EXISTS repair_dispatch_alert (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '工单ID',
    source_type TINYINT NOT NULL COMMENT '1初次报修 2接单超时 3申请转派 4请假转派',
    failure_reason VARCHAR(50) NOT NULL COMMENT '失败原因编码',
    alert_status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
    failure_detail VARCHAR(500) DEFAULT NULL COMMENT '失败详情',
    occurrence_count INT NOT NULL DEFAULT 1 COMMENT '累计发生次数',
    first_occurred_time DATETIME NOT NULL,
    last_occurred_time DATETIME NOT NULL,
    handled_by BIGINT DEFAULT NULL,
    handled_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispatch_alert_open (order_id, source_type, failure_reason, alert_status),
    KEY idx_dispatch_alert_status_time (alert_status, last_occurred_time),
    KEY idx_dispatch_alert_order (order_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动派单失败告警表';

INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-01-01','元旦',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-01-02','元旦',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-01-03','元旦',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-01-04','元旦后补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-14','春节前补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-15','春节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-16','除夕',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-17','初一',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-18','初二',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-19','初三',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-20','初四',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-21','初五',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-22','初六',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-23','初七',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-02-28','春节后补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-04-04','清明节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-04-05','清明节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-04-06','清明节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-01','劳动节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-02','劳动节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-03','劳动节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-04','劳动节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-05','劳动节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-05-09','劳动节后补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-06-19','端午节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-06-20','端午节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-06-21','端午节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-09-20','中秋节前补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-09-25','中秋节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-09-26','中秋节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-09-27','中秋节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-01','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-02','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-03','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-04','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-05','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-06','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-07','国庆节',1,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
INSERT INTO repair_holiday_calendar(holiday_date,holiday_name,day_type,year) VALUES('2026-10-10','国庆节后补班',2,2026) ON DUPLICATE KEY UPDATE holiday_name=VALUES(holiday_name),day_type=VALUES(day_type),year=VALUES(year);
