-- AI 法律助手建表脚本
-- 数据库由 JDBC 连接参数 createDatabaseIfNotExist 自动创建，此处只建表

-- 1. 会话表（多轮对话）
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `title`        VARCHAR(128) NOT NULL DEFAULT '新会话' COMMENT '会话标题',
    `scenario`     VARCHAR(64)           DEFAULT NULL COMMENT '所属场景编码',
    `summary`      VARCHAR(512)          DEFAULT NULL COMMENT '会话摘要',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话会话';

-- 2. 消息表（会话内的一问一答）
CREATE TABLE IF NOT EXISTS `chat_message` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `session_id`   BIGINT       NOT NULL COMMENT '会话 ID',
    `role`         VARCHAR(16)  NOT NULL COMMENT 'user / assistant',
    `content`      MEDIUMTEXT   NOT NULL COMMENT '消息内容',
    `citations`    TEXT                  DEFAULT NULL COMMENT '引用的法条 JSON 数组',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '对话消息';

-- 3. 知识库文档表（灌入的法规、案例）
CREATE TABLE IF NOT EXISTS `kb_document` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `title`        VARCHAR(255) NOT NULL COMMENT '文档标题',
    `source`       VARCHAR(255)          DEFAULT NULL COMMENT '来源',
    `doc_type`     VARCHAR(32)  NOT NULL DEFAULT 'law' COMMENT 'law 法规 / case 案例 / contract 合同',
    `chunk_count`  INT          NOT NULL DEFAULT 0 COMMENT '切分后的片段数',
    `content`      LONGTEXT              DEFAULT NULL COMMENT '原始全文',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '知识库文档';

-- 4. 咨询向导会话表（结构化问答流）
CREATE TABLE IF NOT EXISTS `consult_session` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `scenario_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'IN_PROGRESS' COMMENT 'IN_PROGRESS / COMPLETED',
    `current_step` INT          NOT NULL DEFAULT 0 COMMENT '当前步骤序号',
    `answers`      TEXT                  DEFAULT NULL COMMENT '用户回答 JSON',
    `report`       LONGTEXT              DEFAULT NULL COMMENT '生成的结论报告',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '咨询向导会话';

-- 5. 合同审查任务表
CREATE TABLE IF NOT EXISTS `contract_review_task` (
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `file_name`     VARCHAR(255) NOT NULL COMMENT '上传文件名',
    `clause_count`  INT          NOT NULL DEFAULT 0 COMMENT '条款总数',
    `risk_count`    INT          NOT NULL DEFAULT 0 COMMENT '风险条款数',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ANALYZING / DONE / FAILED',
    `result`        LONGTEXT              DEFAULT NULL COMMENT '审查结果 JSON',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '合同审查任务';

-- 6. 文书生成记录表
CREATE TABLE IF NOT EXISTS `doc_generation` (
    `id`           BIGINT       NOT NULL COMMENT '主键',
    `template_code` VARCHAR(64) NOT NULL COMMENT '文书模板编码',
    `title`        VARCHAR(255) NOT NULL COMMENT '文书标题',
    `content`      LONGTEXT              DEFAULT NULL COMMENT '文书正文',
    `file_path`    VARCHAR(512)          DEFAULT NULL COMMENT '生成文件路径',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文书生成记录';

-- 7. 客户分类表
CREATE TABLE IF NOT EXISTS `customer_category` (
                                     `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称',
                                     `sort` INT DEFAULT 0 COMMENT '排序号',
                                     `remark` VARCHAR(255) DEFAULT '' COMMENT '备注',
                                     `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
                                     `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户分类表';

-- 插入测试数据
INSERT INTO `customer_category` (`category_name`, `sort`, `remark`) VALUES
                                                                        ('大客户', 1, '年度采购额100万以上'),
                                                                        ('普通客户', 2, '常规合作客户'),
                                                                        ('潜在客户', 3, '待开发客户'),
                                                                        ('流失客户', 4, '长期无订单');

-- 8. 客户表
CREATE TABLE IF NOT EXISTS `customer` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `category_id` BIGINT NOT NULL COMMENT '客户分类ID，关联customer_category',
                            `customer_name` VARCHAR(128) NOT NULL COMMENT '客户名称',
                            `contact` VARCHAR(32) DEFAULT '' COMMENT '联系人',
                            `phone` VARCHAR(20) DEFAULT '' COMMENT '联系电话',
                            `address` VARCHAR(255) DEFAULT '' COMMENT '客户地址',
                            `credit_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '授信额度',
                            `status` TINYINT DEFAULT 1 COMMENT '客户状态 1正常 2停用',
                            `remark` VARCHAR(255) DEFAULT '' COMMENT '备注',
                            `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除 0未删除 1已删除',
                            `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_category_id` (`category_id`),
                            KEY `idx_customer_name` (`customer_name`),
                            CONSTRAINT `fk_customer_category` FOREIGN KEY (`category_id`) REFERENCES `customer_category`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 插入测试数据
INSERT INTO `customer` (`category_id`, `customer_name`, `contact`, `phone`, `address`, `credit_amount`, `status`, `remark`) VALUES
                                                                                                                                (1, '郑州XX科技有限公司', '王总', '13800138001', '郑州市高新区XX路1号', 500000.00, 1, '长期战略合作'),
                                                                                                                                (2, '河南XX商贸公司', '李经理', '13800138002', '郑州市中原区XX街道', 100000.00, 1, '月度采购'),
                                                                                                                                (3, 'XX贸易有限公司', '张工', '13800138003', '开封市XX园区', 0.00, 1, '正在洽谈'),
                                                                                                                                (4, 'XX实业集团', '刘总', '13800138004', '洛阳市XX大道', 200000.00, 2, '近一年无合作');