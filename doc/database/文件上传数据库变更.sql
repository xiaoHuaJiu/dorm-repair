CREATE TABLE IF NOT EXISTS sys_file (
 id BIGINT NOT NULL AUTO_INCREMENT, original_name VARCHAR(255) NOT NULL, bucket_name VARCHAR(100) NOT NULL,
 object_name VARCHAR(500) NOT NULL, file_type VARCHAR(20) NOT NULL, content_type VARCHAR(100), file_suffix VARCHAR(20),
 file_size BIGINT NOT NULL DEFAULT 0, biz_type VARCHAR(30) NOT NULL, status TINYINT NOT NULL DEFAULT 1,
 create_by BIGINT, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted TINYINT NOT NULL DEFAULT 0,
 PRIMARY KEY(id), UNIQUE KEY uk_bucket_object(bucket_name,object_name), KEY idx_create_by_time(create_by,create_time),
 KEY idx_file_biz_type(biz_type), KEY idx_file_status_deleted(status,deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公共文件表';

CREATE TABLE IF NOT EXISTS repair_order_file (
 id BIGINT NOT NULL AUTO_INCREMENT, repair_order_id BIGINT NOT NULL, file_id BIGINT NOT NULL, file_type VARCHAR(20) NOT NULL,
 sort_no INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id),
 UNIQUE KEY uk_order_file(repair_order_id,file_id), KEY idx_order_file_order(repair_order_id), KEY idx_order_file_file(file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报修工单附件关联表';

CREATE TABLE IF NOT EXISTS repair_process_file (
 id BIGINT NOT NULL AUTO_INCREMENT, process_id BIGINT NOT NULL, file_id BIGINT NOT NULL, file_type VARCHAR(20) NOT NULL,
 sort_no INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id),
 UNIQUE KEY uk_process_file(process_id,file_id), KEY idx_process_file_process(process_id), KEY idx_process_file_file(file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修过程附件关联表';

CREATE TABLE IF NOT EXISTS repair_rework_file (
 id BIGINT NOT NULL AUTO_INCREMENT, rework_id BIGINT NOT NULL, file_id BIGINT NOT NULL, file_type VARCHAR(20) NOT NULL,
 sort_no INT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(id),
 UNIQUE KEY uk_rework_file(rework_id,file_id), KEY idx_rework_file_rework(rework_id), KEY idx_rework_file_file(file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返工附件关联表';
