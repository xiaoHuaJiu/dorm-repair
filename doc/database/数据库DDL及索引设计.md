统一以 **MySQL 8.x + InnoDB + utf8mb4** 为基准。

在前面的 13 张表基础上补 `sys_user`，另外补两张业务上实际需要的表：

- `repair_evaluation`：学生评价；
- `repair_reminder_record`：用于 10 分钟、5 分钟、0 分钟提醒的**防重复发送**。否则定时任务每扫描一次都可能重复提醒。

全局原则：**暂时不加数据库外键 FK**。业务关联由应用层保证，避免以后逻辑删除、历史数据、用户同步、位置组织结构调整时被数据库外键卡死。

------

# 一、用户表 `sys_user`

统一保存学生、维修人员、管理员账号身份。

```
CREATE TABLE sys_user (
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
```

## 索引业务意义

### `uk_sys_user_username`

登录：

```
WHERE username = ?
```

必须唯一。

### `idx_sys_user_role_status`

管理员查询：

```
所有维修人员
所有正常维修人员
所有学生
```

例如：

```
WHERE role_type = 2
  AND status = 1
  AND deleted = 0
```

------

# 二、维修人员扩展表 `repair_worker`

`sys_user` 负责：

> 他是谁、什么角色。

`repair_worker` 负责：

> 他作为维修人员的业务状态。

```
CREATE TABLE repair_worker (
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
```

自动派单第一步之一：

```
WHERE work_status = 0
AND deleted = 0
```

直接排除：

```
请假中
停用
```

------

# 三、故障类型表 `repair_fault_type`

```
CREATE TABLE repair_fault_type (
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
```

这里不建议逻辑删除故障类型。

历史工单可能还引用旧类型。

停用：

```
status = 0
```

即可。

------

# 四、维修人员故障类型关联表 `repair_worker_fault_type`

```
CREATE TABLE repair_worker_fault_type (
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
```

这里特意**不使用**：

```
UNIQUE(worker_id, fault_type_id, deleted)
```

因为你之前遇到过逻辑删除唯一索引的问题：

```
删除 → 再添加 → 再删除
```

会产生多条：

```
(worker, fault, deleted=1)
```

冲突。

所以这里采用：

> 唯一关系永远只有一条，通过 status 启停。

派单：

```
WHERE fault_type_id = ?
AND status = 1
```

使用：

```
idx_fault_worker
```

------

# 五、区域表`repair_area `

```
CREATE TABLE repair_area (
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
```

查询某节点的直接子级：

```
SELECT *
FROM repair_area
WHERE parent_id = ?
  AND status = 1
  AND deleted = 0
ORDER BY sort_no, id;
```

`idx_area_parent(parent_id, status, sort_no)` 就是专门服务这个高频接口的。工单本身保留了三级位置ID快照。

增加区域树以后，不需要推翻这一结构。

三个字段统一指向 `repair_area.id`：

```
repair_order.campus_id
        ↓
repair_area.id（area_type = 1 单位）

repair_order.area_id
        ↓
repair_area.id（area_type = 2 区域）

repair_order.building_id
        ↓
repair_area.id（area_type = 3 楼栋）

room_id：

repair_area.id（area_type = 4 房间，parent_id 指向对应楼栋）
```

也就是：

```
repair_area

东校区(id=1)
     │
     └── 学生生活一区(id=10)
               │
               └── 3号楼(id=100)
```

学生报修以后：

```
repair_order

campus_id   = 1
area_id     = 10
building_id = 100
room_id     = ...
```

这样做有一个很大的好处：

> **树表负责维护区域关系，工单直接保存三级节点ID，避免每次查工单还要递归查父节点。**

这也正好服务已有的重复报修索引。

# 五、维修人员负责区域表 `repair_worker_area_scope`

```
CREATE TABLE repair_worker_area_scope (
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
```

自动派单第二层过滤：

```
故障所在：
东校区 → A区 → 3号楼
```

寻找：

```
明确负责3号楼
或者负责整个A区
或者负责整个东校区
```

这个查询后续 Mapper 可以按照范围优先级写。

------

# 六、工作时间方案 `repair_work_schedule`

```
CREATE TABLE repair_work_schedule (
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
```

用于：

```
WHERE status = 1
AND start_date <= CURRENT_DATE
AND end_date >= CURRENT_DATE
```

主要计算：

```
accept_deadline
```

------

# 七、工单主表 `repair_order`

这是最重要的一张表。

```
CREATE TABLE repair_order (
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
```

------

# 八、这里的几个核心索引非常重要

## 1. 重复报修识别

```
idx_order_duplicate_check
(
    campus_id,
    area_id,
    building_id,
    room_id,
    fault_type_id,
    status,
    report_time
)
```

对应：

```
SELECT id, order_no
FROM repair_order
WHERE campus_id = ?
  AND area_id = ?
  AND building_id = ?
  AND room_id = ?
  AND fault_type_id = ?
  AND status IN (0, 1, 2, 4, 5)
  AND report_time >= NOW() - INTERVAL 24 HOUR
  AND deleted = 0
ORDER BY report_time DESC
LIMIT 1;
```

这就是学生提交报修前每次都会执行的查询。

------

## 2. 自动派单工作量统计

```
idx_order_assignee_status
```

用于：

```
SELECT current_assignee_id, COUNT(*)
FROM repair_order
WHERE current_assignee_id IN (...)
  AND status IN (1, 2, 3, 4, 5)
  AND deleted = 0
GROUP BY current_assignee_id;
```

计算：

> 哪个师傅当前未完成工单最少。

------

## 3. 接单超时扫描

```
idx_order_accept_timeout (
    status,
    accept_deadline
)
```

定时任务：

```
WHERE status = 1
AND accept_deadline <= NOW()
```

------

## 4. 完成超时扫描

```
idx_order_complete_timeout (
    status,
    complete_deadline
)
```

例如：

```
WHERE status IN (2, 4, 5)
AND complete_deadline <= NOW()
```

------

# 九、请假申请 `repair_leave_request`

```
CREATE TABLE repair_leave_request (
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
```

## 重要索引

### 请假冲突检查

```
WHERE worker_id = ?
AND status IN (0,1)
AND start_time < new_end
AND end_time > new_start
```

`idx_leave_worker_time` 支撑。

### 请假开始定时任务

```
WHERE status = 1
AND reassign_status = 0
AND start_time <= NOW()
AND end_time > NOW()
```

使用：

```
idx_leave_start_task
```

------

# 十、转派申请 `repair_transfer_request`

```
CREATE TABLE repair_transfer_request (
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
```

------

# 十一、维修过程记录 `repair_process_record`

```
CREATE TABLE repair_process_record (
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
```

工单详情页：

```
WHERE order_id = ?
ORDER BY record_time ASC
```

非常高频，因此：

```
(order_id, record_time)
```

是核心索引。

------

# 十二、维修材料使用 `repair_material_usage`

```
CREATE TABLE repair_material_usage (
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
```

管理员以后统计：

```
某段时间使用了多少阀芯
某维修人员用了哪些材料
某工单用了哪些材料
```

都能支持。

------

# 十三、返工记录 `repair_rework_record`

```
CREATE TABLE repair_rework_record (
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
```

这里：

```
UNIQUE(order_id, rework_no)
```

非常重要。

即使并发请求重复提交：

```
第一次返工 rework_no=1
```

数据库只能存在一条。

这是业务幂等的最后一道保护。

------

# 十四、工单流转记录 `repair_order_flow`

```
CREATE TABLE repair_order_flow (
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
```

最核心：

```
idx_flow_order_time
```

查看工单完整时间轴：

```
SELECT *
FROM repair_order_flow
WHERE order_id = ?
ORDER BY operation_time ASC;
```

------

# 十五、学生维修评价 `repair_evaluation`

前面业务已经确定学生可以评价，所以应该正式建表。

```
CREATE TABLE repair_evaluation (
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
```

一个工单最终只能评价一次，因此：

```
UNIQUE(order_id)
```

直接提供幂等保证。

------

# 十六、提醒记录 `repair_reminder_record`

这个表非常有必要。

因为定时任务可能：

```
每分钟执行一次
```

而：

```
距离截止还有10分钟
```

这个窗口可能被扫到多次。

必须防止重复提醒。

```
CREATE TABLE repair_reminder_record (
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
```

为什么唯一键里加入：

```
deadline_time
```

因为发生重新派单后：

```
accept_deadline
```

会变化。

同一个工单的新负责人仍然需要重新收到：

```
10分钟
5分钟
0分钟
```

提醒。

------

# 十七、接口幂等记录 `sys_idempotent_record`

该表为正式创建报修接口提供 MySQL 最终幂等兜底。Redis 只承担短时间并发门闩，不能替代此表。

```sql
CREATE TABLE sys_idempotent_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL,
    biz_type VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0处理中 1成功',
    result_snapshot JSON DEFAULT NULL,
    expire_time DATETIME DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotent_biz_user_no (biz_type,user_id,biz_no),
    KEY idx_idempotent_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口幂等记录表';
```

`request_hash` 是不包含 `confirmDuplicate` 的标准化报修内容 SHA-256；同一学生同一 `bizNo` 只能对应一份业务内容。成功结果只保存创建结果最小快照。

# 十八、系统操作日志 `sys_operation_log`

```
CREATE TABLE sys_operation_log (
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
```

------

# 十八、完整表关系

当前正式表结构：

```
sys_user
    │
    ├──────────── Student
    │
    ├──────────── Admin
    │
    │
    └──────────── Repair Worker
                     │
                     ▼
                repair_worker
                 │     │
          ┌──────┘     └─────────┐
          ▼                      ▼
worker_fault_type        worker_area_scope
          │                      │
          └──────────┬───────────┘
                     ▼
               自动派单匹配
                     │
                     ▼
               repair_order
                     │
      ┌──────────────┼──────────────────┐
      │              │                  │
      ▼              ▼                  ▼
process_record   material_usage    transfer_request
      │                                 │
      │                           管理员审批
      │                                 │
      └──────────────┐                  ▼
                     │              自动重新派单
                     │                  │
                     ▼                  ▼
                repair_order ◄──────────┘
                     │
             ┌───────┼────────┐
             ▼       ▼        ▼
          rework   evaluation flow
          record              record

repair_worker
      │
      ▼
leave_request
      │
      ▼
请假开始
      │
      ▼
读取 repair_order
当前未完成工单
      │
      ▼
逐个重新派单
      │
      ▼
repair_order_flow

repair_work_schedule
      │
      ▼
计算 accept_deadline

repair_order
      │
      ▼
超时扫描
      │
      ▼
repair_reminder_record

所有用户操作
      │
      ▼
sys_operation_log
```

------

# 十九、从业务流程重新验证索引设计

## 1. 学生提交报修

首先执行：

```
位置 + 故障类型 + 24小时 + 未关闭
```

依赖：

```
idx_order_duplicate_check
```

然后：

```
创建 repair_order
```

------

# 2. 自动派单

第一步：

```
找会处理这种故障的人
```

使用：

```
repair_worker_fault_type.idx_fault_worker
```

第二步：

```
负责这个区域
```

使用：

```
repair_worker_area_scope.idx_scope_location
```

第三步：

```
过滤请假/停用
```

使用：

```
repair_worker.idx_worker_work_status
```

第四步：

```
统计未完成工单
```

使用：

```
repair_order.idx_order_assignee_status
```

第五步：

```
计算工作时间
```

使用：

```
repair_work_schedule.idx_schedule_effective
```

------

# 3. 接单提醒

查询：

```
WHERE status = 1
AND accept_deadline BETWEEN ? AND ?
```

使用：

```
idx_order_accept_timeout
```

然后插：

```
repair_reminder_record
```

利用：

```
uk_reminder_deduplicate
```

防重复。

------

# 4. 接单超时转派

```
WHERE status = 1
AND accept_deadline <= NOW()
```

仍然：

```
idx_order_accept_timeout
```

然后重新跑自动派单。

------

# 5. 维修中

详情页面：

```
工单基本信息
+
过程
+
材料
+
流转
```

分别命中：

```
repair_order PK

repair_process_record
(order_id, record_time)

repair_material_usage
(order_id, use_time)

repair_order_flow
(order_id, operation_time)
```

全部是：

```
1 + N
```

典型高效查询。

------

# 6. 转派申请

维修人员：

```
WHERE applicant_worker_id = ?
ORDER BY create_time DESC
```

使用：

```
idx_transfer_worker
```

管理员待审批：

```
WHERE approval_status = 0
ORDER BY create_time
```

使用：

```
idx_transfer_pending
```

------

# 7. 请假审批

管理员：

```
WHERE status = 0
ORDER BY create_time
```

命中：

```
idx_leave_pending_review
```

------

# 8. 请假正式开始

定时任务：

```
WHERE status = 1
AND reassign_status = 0
AND start_time <= NOW()
```

使用：

```
idx_leave_start_task
```

然后根据：

```
worker_id
```

查询该师傅所有未完成工单：

```
WHERE current_assignee_id = ?
AND status IN (...)
```

使用：

```
idx_order_assignee_status
```

这里正好和自动派单工作量统计共用一个索引。

------

# 9. 多次返工

查：

```
WHERE order_id = ?
ORDER BY rework_no
```

由：

```
uk_rework_order_no
```

支持。

异常列表：

```
WHERE admin_intervention = 1
AND status = 0
```

使用：

```
idx_rework_admin
```

------

# 10. 管理员异常工单

```
WHERE exception_flag = 1
AND status <> 6
```

使用：

```
idx_order_exception
```

------

# 二十、关于逻辑删除和唯一索引，再统一一个规则

这个系统后面很容易再次遇到：

```
deleted参与唯一索引
```

的问题。

所以我建议统一：

### 历史业务数据

例如：

```
工单
返工记录
请假
转派
流转
评价
```

正常情况下都**不应该真的删除**。

------

### 配置关系

例如：

```
维修人员-故障类型
```

不要采用：

```
UNIQUE(worker_id, fault_type_id, deleted)
```

而采用：

```
UNIQUE(worker_id, fault_type_id)
```

删除实际上：

```
status = 0
```

重新配置：

```
status = 1
```

直接恢复。

这样就不会再次出现你之前视频提交表那类：

```
多条 deleted=1 撞唯一键
```

的问题。

------

# 二十一、目前我认为最关键的索引清单

如果只看系统性能核心，真正必须关注的是这些：

| 表                   | 索引                         | 解决问题                         |
| -------------------- | ---------------------------- | -------------------------------- |
| repair_order         | `idx_order_duplicate_check`  | 24小时重复报修检测               |
| repair_order         | `idx_order_assignee_status`  | 当前维修人员工作量、请假工单查询 |
| repair_order         | `idx_order_accept_timeout`   | 接单提醒、接单超时               |
| repair_order         | `idx_order_complete_timeout` | 完成提醒、完成超时               |
| worker_fault_type    | `idx_fault_worker`           | 按故障类型找师傅                 |
| worker_area_scope    | `idx_scope_location`         | 按区域过滤师傅                   |
| repair_leave_request | `idx_leave_start_task`       | 请假开始任务                     |
| transfer_request     | `idx_transfer_pending`       | 管理员转派审批                   |
| process_record       | `idx_process_order_time`     | 工单维修时间线                   |
| order_flow           | `idx_flow_order_time`        | 工单完整生命周期                 |
| reminder_record      | `uk_reminder_deduplicate`    | 防重复提醒                       |
| rework_record        | `uk_rework_order_no`         | 返工幂等                         |

这套索引不是单纯给每个字段都加，而是基本都直接对应现在已经确定的真实 SQL 查询链路。

## PLAN-5 自动派单新增结构

### `repair_holiday_calendar`

按日期保存法定放假日和调休上班日，`day_type=1` 表示放假、`day_type=2` 表示调休上班。`uk_holiday_date` 保证每天只有一条有效解释，`idx_holiday_year(year,day_type)` 支持按年度维护。当前已导入附件提供的 2026 年 39 条配置。

### `repair_dispatch_alert`

保存自动派单失败原因、详情、发生次数和处理状态。`uk_dispatch_alert_open(order_id,source_type,failure_reason,alert_status)` 对待处理告警去重，`idx_dispatch_alert_status_time(alert_status,last_occurred_time)` 支持管理员待办扫描。

自动派单候选查询继续使用 `idx_fault_worker`、`idx_scope_location`、`idx_worker_work_status`、`idx_leave_worker_time` 和 `idx_order_assignee_status`。工单最终写入使用带原状态和原负责人的 CAS UPDATE，不新增全局锁表。

------

# 二十二、最终数据库职责划分

现在数据库可以形成非常清晰的四层：

```
【用户及配置】

sys_user
repair_worker
repair_fault_type
repair_worker_fault_type
repair_worker_area_scope
repair_work_schedule


【核心工单】

repair_order


【工单业务过程】

repair_process_record
repair_material_usage
repair_transfer_request
repair_leave_request
repair_rework_record
repair_evaluation


【审计与任务辅助】

repair_order_flow
repair_reminder_record
sys_operation_log
```

其中整个系统最核心的一条数据设计原则仍然是：

> **`repair_order` 保存“现在”，`repair_order_flow` 保存“过去”；各业务子表保存“为什么发生这次变化”，操作日志保存“谁在系统里执行了什么”。**

开发顺序按依赖关系走：

**基础配置 → 学生报修 → 自动派单 → 维修人员接单 → 维修过程/材料 → 完工确认 → 转派 → 返工 → 请假转派 → 超时提醒。**

这样每完成一阶段，前面的业务链路都是可独立测试和验收的。

## PLAN-7 增量

新增 `repair_order_alert`，正式表总数为 21。`uk_order_alert_rework_type(order_id,rework_no,alert_type)` 防止同一轮重复告警；`idx_order_alert_status_time(alert_status,create_time)` 支持管理员查询待处理告警。
