--Licensed to the Apache Software Foundation (ASF) under one or more
--contributor license agreements.  See the NOTICE file distributed with
--this work for additional information regarding copyright ownership.
--The ASF licenses this file to You under the Apache License, Version 2.0
--(the "License"); you may not use this file except in compliance with
--the License.  You may obtain a copy of the License at
--
--http://www.apache.org/licenses/LICENSE-2.0
--
--Unless required by applicable law or agreed to in writing, software
--distributed under the License is distributed on an "AS IS" BASIS,
--WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
--         See the License for the specific language governing permissions and
--         limitations under the License.

-- 创建序列（如果需要）
-- 为每个序列生成对应的 H2 序列
CREATE SEQUENCE IF NOT EXISTS SEQ_DATA_BASE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_LINK_TASK_TABLE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_LINK_TASK_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_MOVE_INFO_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_MOVE_TASK_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_SYSTEM_LOG_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_JOB_LOGBACK_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_MOVE_TRACE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_TASK_EXECUTE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_METRICS_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_TASK_EXTEND_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_MOVE_TABLE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_DATA_CHECK_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_COLUMN_TYPE_TEST_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_TABLE_LINK_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_LINK_NODE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_FILE_FORMAT_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_FILE_BODY_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_FILE_FIELD_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_FILE_SPECIAL_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_DEBEZIUM_HISTORY_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_MQ_CONFIG_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_COLUMN_TYPE_DEFINE_ID START WITH 50000 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS SEQ_COLUMN_TYPE_MAP_ID START WITH 50000 INCREMENT BY 1;

drop table data_stream_data_base;
drop table data_stream_move_task;
drop table data_stream_move_table;
drop table data_stream_task_extend;
drop table data_stream_task_execute;
drop table data_stream_data_check;
drop table data_stream_move_info;
drop table data_stream_move_trace;
drop table data_stream_link_task;
drop table data_stream_link_task_table;
drop table data_stream_table_link;
drop table data_stream_link_node;
drop table data_stream_system_log;
drop table data_stream_job_logback;
drop table data_stream_sequence;
drop table data_stream_table_map;
drop table data_stream_metrics;
drop table data_stream_column_type_define;
drop table data_stream_column_type_test;
drop table data_stream_column_type_map;
drop table data_stream_file_format;
drop table data_stream_file_body;
drop table data_stream_file_field;
drop table data_stream_file_filter;
drop table data_stream_file_special;
drop table data_stream_debezium_offsets;
drop table data_stream_debezium_history;
drop table data_stream_mq_config;
drop table data_stream_session;

-- 设置H2为MySQL兼容模式
SET MODE MYSQL;

CREATE TABLE IF NOT EXISTS data_stream_data_base (
    data_base_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_data_base_id',
    data_base_type INT NOT NULL COMMENT '数据库类型',
    data_base_name VARCHAR(128) NOT NULL COMMENT '数据库名称',
    url VARCHAR(512) NOT NULL COMMENT '数据库链接',
    user_name VARCHAR(128) NOT NULL COMMENT '数据库用户名',
    pass_word VARCHAR(128) NOT NULL COMMENT '数据库密码',
    table_key_not_supported INT DEFAULT NULL COMMENT '数据不支持表主键：1:不支持，其他默认支持',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT NOT NULL COMMENT '状态：0(删除)、1(下线)、2(上线)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    PRIMARY KEY (data_base_id)
);

CREATE TABLE IF NOT EXISTS data_stream_move_task (
    task_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_move_task_id',
    task_type INT NOT NULL COMMENT '任务类型：1数据迁移、2数据删除、3数据迁移删除、4表结构迁移、5数据稽核',
    source_object_type BIGINT NOT NULL COMMENT '源数据对象类型：2mysql、3orale、4postgres、5doris、6men、7h2、8text、9excel',
    source_object_id BIGINT NOT NULL COMMENT '源数据对象标识',
    source_object_name VARCHAR(256) NOT NULL COMMENT '源对象名',
    source_object_condition VARCHAR(256) DEFAULT NULL COMMENT '源扩展条件:支持简单数据过滤条件',
    source_object_count BIGINT DEFAULT NULL COMMENT '源对象记录数',
    source_object_keys VARCHAR(128) DEFAULT NULL COMMENT '源对象唯一主键字段',
    source_keys_begin VARCHAR(128) DEFAULT NULL COMMENT '源对象唯一主键开始值',
    source_keys_end VARCHAR(128) DEFAULT NULL COMMENT '源对象唯一主键结束值',
    source_load_strategy INT NOT NULL COMMENT '源对象加载数据策略:1:分页加载、2分段加载',
    source_data_node VARCHAR(256) DEFAULT NULL COMMENT '源对象:分片节点，支持多个节点数据并发处理',
    source_data_set INT DEFAULT NULL,
    target_object_type BIGINT NOT NULL COMMENT '源数据对象类型：2mysql、3orale、4postgres、5doris、6men、7h2、8text、9excel',
    target_object_id BIGINT NOT NULL COMMENT '目标对象标识',
    target_object_name VARCHAR(256) NOT NULL COMMENT '目标对象名',
    target_object_begin_count BIGINT DEFAULT NULL COMMENT '目标对象迁入前记录数',
    target_object_end_count BIGINT DEFAULT NULL COMMENT '目标对象迁入后记录数',
    target_insert_mode INT NOT NULL COMMENT '数据目标端写入方式:1(拼接sql方式)、2(绑定变量方式),如果为空按1方式',
    send_mode INT NOT NULL COMMENT '数据分发模型:1异步、2同步，默认异步方式',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT DEFAULT NULL COMMENT '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    task_disc VARCHAR(1024) DEFAULT NULL COMMENT '任务描述',
    error_code VARCHAR(128) DEFAULT NULL COMMENT '任务错误编码',
    error_msg VARCHAR(128) DEFAULT NULL COMMENT '任务错误信息',
    priority INT NOT NULL COMMENT '数据迁移任务优先级（数字越小优先级越大）',
    system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码',
    batch_task_id BIGINT DEFAULT NULL COMMENT '批量任务标识',
    copy_task_id BIGINT DEFAULT NULL COMMENT '被复制任务标识',
    PRIMARY KEY (task_id)
);

-- 继续创建其他表...
CREATE TABLE IF NOT EXISTS data_stream_move_table (
    move_table_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_move_table_id',
    task_id BIGINT NOT NULL COMMENT '任务标识',
    source_table_name VARCHAR(256) NOT NULL COMMENT '源表名',
    table_sql varchar(10240) default null comment '生成建表脚本',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT DEFAULT NULL COMMENT '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    error_code VARCHAR(128) DEFAULT NULL COMMENT '任务错误编码',
    error_msg VARCHAR(128) DEFAULT NULL COMMENT '任务错误信息',
    PRIMARY KEY (move_table_id)
);

CREATE TABLE IF NOT EXISTS data_stream_task_extend (
    task_extend_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_task_extend_id',
    task_id BIGINT NOT NULL COMMENT '任务标识',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
     parameter_name VARCHAR(128) DEFAULT NULL COMMENT '参数名称',
    parameter_value VARCHAR(32) DEFAULT NULL COMMENT '参数值',
    PRIMARY KEY (task_extend_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_task_extend_01 ON data_stream_task_extend(task_id);

CREATE TABLE IF NOT EXISTS data_stream_task_execute (
    task_execute_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_task_execute_id',
    task_id BIGINT NOT NULL COMMENT '任务标识',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT DEFAULT NULL COMMENT '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    error_code VARCHAR(128) DEFAULT NULL COMMENT '任务错误编码',
    error_msg VARCHAR(128) DEFAULT NULL COMMENT '任务错误信息',
    system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码',
    host_name VARCHAR(128) DEFAULT NULL COMMENT '处理主机名称',
    host_ip VARCHAR(20) DEFAULT NULL COMMENT '处理主机ip',
    PRIMARY KEY (task_execute_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_task_execute_01 ON data_stream_task_execute(task_id);

-- 剩余表的转换遵循相同模式...
CREATE TABLE IF NOT EXISTS data_stream_data_check (
    data_check_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_data_check_id',
    task_id BIGINT NOT NULL COMMENT '任务标识',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT DEFAULT NULL COMMENT '状态：1(稽核生成)、2(修订成功)、2(修订失败)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    check_result INT DEFAULT NULL COMMENT '稽核结果：1(源数据多)、2(数据不一致)、3(目标数据多)',
    check_keys VARCHAR(128) DEFAULT NULL COMMENT '稽核主键字段',
    error_code VARCHAR(128) DEFAULT NULL COMMENT '任务错误编码',
    error_msg VARCHAR(128) DEFAULT NULL COMMENT '任务错误信息',
    PRIMARY KEY (data_check_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_task_check_01 ON data_stream_data_check(task_id);

CREATE TABLE IF NOT EXISTS data_stream_move_info (
    info_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_move_info_id',
    info_flag INT NOT NULL COMMENT '运行标识:1源端线程生成、2目标端线程生成',
    task_id BIGINT NOT NULL COMMENT '数据迁移任务标识',
    table_name VARCHAR(256) NOT NULL COMMENT '数据迁移表名',
    data_node VARCHAR(256) DEFAULT NULL COMMENT 'teledb分片节点，支持多个节点数据并发处理',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    virtual_id INT NOT NULL COMMENT '启动线程序列标识',
    thread_name VARCHAR(128) NOT NULL COMMENT '处理线程名称',
    data_count BIGINT NOT NULL COMMENT '记录数',
    data_actual_count BIGINT NOT NULL COMMENT '实际记录数',
    loop_count INT NOT NULL COMMENT '循环次数',
    max_cost BIGINT NOT NULL COMMENT '最大耗时',
    min_cost BIGINT NOT NULL COMMENT '最小耗时',
    sum_cost BIGINT NOT NULL COMMENT '总耗时',
    lately_cost BIGINT NOT NULL COMMENT '最新耗时',
    `page_row_start` varchar(128) not null comment '数据加载开始,分段记录主键值、分页记录页码',
    `page_row_end` varchar(128) not null comment '数据加载结束,分段记录主键值、分页记录页码',
    `page_loop_count` int(9) not null comment '数据翻页迭代次数',
    state INT NOT NULL COMMENT '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    error_code VARCHAR(128) DEFAULT NULL COMMENT '线程错误编码',
    error_msg VARCHAR(128) DEFAULT NULL COMMENT '线程错误信息',
    PRIMARY KEY (info_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_move_info_01 ON data_stream_move_info(task_id);

CREATE TABLE IF NOT EXISTS data_stream_move_trace (
    trace_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_move_trace_id',
    info_id BIGINT NOT NULL COMMENT '数据迁移运行标识',
    task_id BIGINT NOT NULL COMMENT '数据迁移任务标识',
    page_row_start VARCHAR(128) DEFAULT NULL COMMENT '记录开始',
    page_row_end VARCHAR(128) DEFAULT NULL COMMENT '记录结束',
    data_count BIGINT DEFAULT NULL COMMENT '记录数',
    data_actual_count BIGINT DEFAULT NULL COMMENT '实际记录数',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    PRIMARY KEY (trace_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_move_trace_01 ON data_stream_move_trace(info_id);

CREATE TABLE IF NOT EXISTS data_stream_link_task (
    link_task_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_link_task_id',
    table_link_id BIGINT DEFAULT NULL COMMENT '链接标识',
    business_id BIGINT DEFAULT NULL COMMENT '业务流水',
    source_data_base_id BIGINT NOT NULL COMMENT '源库标识',
    target_data_base_id BIGINT NOT NULL COMMENT '目标库标识',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
     state INT NOT NULL COMMENT '数据迁移状态：0(等待迁移)、1(回迁中)、2(回迁成功)、3(回迁失败)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    task_disc VARCHAR(1024) DEFAULT NULL COMMENT '任务描述',
    error_code VARCHAR(32) DEFAULT NULL COMMENT '回迁失败错误码',
    error_msg VARCHAR(256) DEFAULT NULL COMMENT '回迁失败信息',
    system_user_code VARCHAR(256) DEFAULT NULL COMMENT '员工编码',
    host_name VARCHAR(128) DEFAULT NULL COMMENT '处理主机名称',
    host_ip VARCHAR(20) DEFAULT NULL COMMENT '处理主机ip',
    PRIMARY KEY (link_task_id)
);

CREATE TABLE IF NOT EXISTS data_stream_link_task_table (
    link_task_table_id BIGINT NOT NULL COMMENT '主键标识，序列：seq_link_task_table_id',
    link_task_id BIGINT NOT NULL COMMENT '关联链接任务表示',
    select_table_name VARCHAR(256) DEFAULT NULL COMMENT '查询模型名称',
    select_count INT DEFAULT NULL COMMENT '查询记录数',
    insert_table_name VARCHAR(256) DEFAULT NULL COMMENT '插入模型名称',
    insert_count INT DEFAULT NULL COMMENT '插入记录数',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    PRIMARY KEY (link_task_table_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_link_table_01 ON data_stream_link_task_table(link_task_id);

CREATE TABLE IF NOT EXISTS data_stream_table_link (
    table_link_id BIGINT NOT NULL COMMENT '链接定义标识，序列名称：seq_table_link_id',
     table_link_name VARCHAR(30) NOT NULL COMMENT '链接名称',
    table_link_des VARCHAR(128) DEFAULT NULL COMMENT '链接描述',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    state INT NOT NULL COMMENT '状态：0(删除)、1(下线)、2(上线)',
    state_date DATETIME NOT NULL COMMENT '状态时间',
    PRIMARY KEY (table_link_id)
);

CREATE TABLE IF NOT EXISTS data_stream_link_node (
    link_node_id BIGINT NOT NULL COMMENT '链接节点标识，序列名称：seq_link_node_id',
    table_link_id BIGINT NOT NULL COMMENT '链接定义标识',
     table_name VARCHAR(128) NOT NULL COMMENT '表名称',
    field_name VARCHAR(128) NOT NULL COMMENT '数据查询字段名称',
    parent_field_name VARCHAR(128) NOT NULL COMMENT '父级数据字段名称，如果是顶层节点',
    parent_link_node_id BIGINT NOT NULL COMMENT '父级节点标识，-1表示顶层节点',
    pos_x INT NOT NULL COMMENT '节点坐标x',
    pos_y INT NOT NULL COMMENT '节点坐标y',
    create_date DATETIME NOT NULL COMMENT '记录生成时间',
    PRIMARY KEY (link_node_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_link_node_01 ON data_stream_link_node(table_link_id);

CREATE TABLE IF NOT EXISTS data_stream_system_log (
    system_log_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_system_log_id',
    type BIGINT NOT NULL COMMENT '1登录日志、2操作日志',
    username VARCHAR(30) DEFAULT NULL COMMENT '操作工号',
    ip_address VARCHAR(30) DEFAULT NULL COMMENT '操作端ip地址',
    module_name VARCHAR(30) DEFAULT NULL COMMENT '操作模块',
    content VARCHAR(128) DEFAULT NULL COMMENT '操作内容',
    url_path VARCHAR(128) DEFAULT NULL COMMENT '操作地址',
    user_agent VARCHAR(128) DEFAULT NULL COMMENT '操作浏览器',
    request_info VARCHAR(1024) DEFAULT NULL COMMENT '输入信息',
    response_info VARCHAR(1024) DEFAULT NULL COMMENT '输出信息',
    create_date DATETIME NOT NULL COMMENT '生成时间',
    PRIMARY KEY (system_log_id)
 );

CREATE TABLE IF NOT EXISTS data_stream_job_logback (
    job_logback_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_job_logback_id',
    job_type BIGINT NOT NULL COMMENT '1数据运行、2数据回迁',
    job_id BIGINT NOT NULL COMMENT '关联id',
    content LONGVARCHAR DEFAULT NULL COMMENT '操作内容',
    create_date DATETIME NOT NULL COMMENT '生成时间',
     PRIMARY KEY (job_logback_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_job_logback_01 ON data_stream_job_logback(job_id, job_type);

CREATE TABLE IF NOT EXISTS data_stream_session (
    token_key VARCHAR(256) NOT NULL COMMENT 'token',
    username VARCHAR(30) NOT NULL COMMENT '用户名',
    create_date DATETIME NOT NULL COMMENT '生成时间',
    expire_date DATETIME NOT NULL COMMENT '失效时间',
    state INT NOT NULL COMMENT '状态：1(生成)、2(删除)',
    PRIMARY KEY (token_key)
);

CREATE TABLE IF NOT EXISTS data_stream_sequence (
    seq_name VARCHAR(50) NOT NULL,
    current_val INT NOT NULL,
    increment_val INT NOT NULL DEFAULT 1,
    PRIMARY KEY (seq_name)
);

CREATE TABLE IF NOT EXISTS data_stream_table_map (
    table_map_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_table_map_id',
    table_name VARCHAR(128) NOT NULL COMMENT '表名称',
    table_type INT NOT NULL COMMENT '表类型,a_split_table_config.table_type',
    PRIMARY KEY (table_map_id)
);

CREATE TABLE IF NOT EXISTS data_stream_metrics (
    metrics_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_metrics_id',
    task_id BIGINT NOT NULL COMMENT '任务标识',
     metrics_time BIGINT NOT NULL COMMENT '指标时刻',
    metrics_value BIGINT NOT NULL COMMENT '指标值',
    PRIMARY KEY (metrics_id)
);

CREATE INDEX IF NOT EXISTS idx_data_stream_metrics_01 ON data_stream_metrics(task_id);

CREATE TABLE IF NOT EXISTS data_stream_column_type_define (
    column_type_define_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_column_type_define_id',
    database_type VARCHAR(32) NOT NULL COMMENT '数据库类型',
    column_type_classify INT NOT NULL COMMENT '字段类型分类：0未分类、1数值类型、2字符串类型、3日期时间类型等',
    column_type_name VARCHAR(32) NOT NULL COMMENT '字段类型名称,全都小写',
    column_standard_size BIGINT DEFAULT NULL COMMENT '字段类型标准长度(如mysql长整型长度19)，如果是null是非标准自定义设置',
    remark VARCHAR(1024) DEFAULT NULL COMMENT '备注说明',
    type_category varchar(50)  DEFAULT NULL COMMENT '类型分类：整数/小数/浮点/字符串/日期/二进制/布尔/json/数组/uuid/其他',
    max_precision int(11) DEFAULT NULL COMMENT '最大精度（整数位数）',
    max_scale int(11) DEFAULT NULL COMMENT '最大小数位数',
    character_max_length bigint(20) DEFAULT NULL COMMENT '字符串最大长度',
    min_value bigint(20) DEFAULT NULL COMMENT '最小值',
    max_value bigint(20) DEFAULT NULL COMMENT '最大值',
    is_national_flag int(2) DEFAULT 0 COMMENT '是否支持字符集(如 nvarchar)',
    require_length_param int(2) DEFAULT 0 COMMENT '是否必须指定长度参数',
    PRIMARY KEY (column_type_define_id)
);

CREATE TABLE IF NOT EXISTS data_stream_column_type_map (
    column_type_map_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_column_type_map_id',
    column_type_define_id_a BIGINT NOT NULL COMMENT '数据库类型a',
    column_type_define_id_b BIGINT NOT NULL COMMENT '数据库类型b',
    match_level int default 1 comment '匹配级别：1-精确匹配，2-兼容匹配，3-降级匹配',
    precision_conversion_rule varchar(200) default NULL comment '精度转换规则表达式',
    length_conversion_rule varchar(200) default NULL comment '长度转换规则表达式',
    conversion_warning varchar(500) default NULL comment '转换警告信息',
    is_reversible int default 1 comment '是否可逆转换（双向无损）',
    PRIMARY KEY (column_type_map_id)
);

CREATE TABLE IF NOT EXISTS data_stream_column_type_test (
    column_type_test_id BIGINT NOT NULL COMMENT '主键标识，序列名称：seq_column_type_test_id',
    database_type VARCHAR(32) NOT NULL COMMENT '数据库类型',
    table_name VARCHAR(32) NOT NULL COMMENT '表名称,全都小写',
    column_name VARCHAR(32) NOT NULL COMMENT '字段名称,全都小写',
    column_type_name VARCHAR(32) NOT NULL COMMENT '字段类型名称,全都小写',
    column_standard_size BIGINT DEFAULT NULL COMMENT '字段类型标准长度(如mysql长整型长度19)，如果是null是非标准自定义设置',
    remark VARCHAR(1024) DEFAULT NULL COMMENT '备注说明',
    PRIMARY KEY (column_type_test_id)
);

CREATE TABLE IF NOT EXISTS data_stream_file_format (
    file_format_id BIGINT NOT NULL,
    file_type INT DEFAULT NULL COMMENT '文件类型：1文本、2excel',
    file_name_type INT NOT NULL,
    file_name_format VARCHAR(128) NOT NULL,
    ftp_host VARCHAR(128) DEFAULT NULL COMMENT '远程主机IP',
    ftp_port VARCHAR(128) DEFAULT NULL COMMENT '远程主机FTP端口',
    ftp_user VARCHAR(128) DEFAULT NULL COMMENT '远程主机FTP用户名',
    ftp_passwd VARCHAR(128) DEFAULT NULL COMMENT '远程主机FTP密码',
    ftp_path VARCHAR(128) DEFAULT NULL COMMENT '远程主机数据路径',
    local_path VARCHAR(128) DEFAULT NULL,
    file_bak_action INT NOT NULL,
    file_bak_path VARCHAR(128) DEFAULT NULL,
    create_date DATETIME NOT NULL,
    on_line_flag INT NOT NULL COMMENT '发布标志：0下线、1在线',
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    PRIMARY KEY (file_format_id)
);

CREATE TABLE IF NOT EXISTS data_stream_file_body (
    file_body_id BIGINT NOT NULL,
    file_format_id BIGINT NOT NULL,
    split_flag BIGINT NOT NULL,
    fix_begin_line INT DEFAULT NULL,
    fix_end_line INT DEFAULT NULL,
    create_date DATETIME NOT NULL,
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    PRIMARY KEY (file_body_id)
);

CREATE TABLE IF NOT EXISTS data_stream_file_field (
    file_field_id BIGINT NOT NULL,
    file_format_id BIGINT NOT NULL,
    belong_flag BIGINT NOT NULL COMMENT '1 文件特殊行、2 文件数据正文',
    belong_id INT NOT NULL,
    field_name VARCHAR(128) NOT NULL,
    fix_width INT DEFAULT NULL,
    position INT DEFAULT NULL,
    sum_line_flag INT DEFAULT NULL COMMENT '用于特殊行定义：1表示该字段记录总行数',
    sum_field_name VARCHAR(128) DEFAULT NULL COMMENT '如果不为空,记录行体用于累加字段名称',
    create_date DATETIME NOT NULL,
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    PRIMARY KEY (file_field_id)
);

CREATE TABLE IF NOT EXISTS data_stream_file_filter (
    file_filter_id BIGINT NOT NULL,
    file_format_id BIGINT NOT NULL,
    file_field_id BIGINT NOT NULL,
    symbol_id INT NOT NULL COMMENT '1(等于=)、2(大于号>)、3(小于号<)、4(大于等于号>=)、3(小于等于号<=)',
    symbol_group INT NOT NULL COMMENT '同一组条件是与关系，不同组条件是或关系',
    file_field_value VARCHAR(128) NOT NULL,
    create_date DATETIME NOT NULL,
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    PRIMARY KEY (file_filter_id)
);

CREATE TABLE IF NOT EXISTS data_stream_file_special (
    file_special_id BIGINT NOT NULL,
    file_format_id BIGINT NOT NULL,
    split_flag INT NOT NULL,
    fix_line_position INT NOT NULL,
    remark VARCHAR(128) DEFAULT NULL,
    create_date DATETIME NOT NULL,
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    PRIMARY KEY (file_special_id)
);

CREATE TABLE IF NOT EXISTS data_stream_debezium_offsets (
    offset_key VARCHAR(1024) NOT NULL COMMENT '偏移量键，字符串格式',
    offset_value VARCHAR(1024) NOT NULL COMMENT '偏移量值',
    offset_count BIGINT NOT NULL COMMENT '同步次数',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (offset_key)
);

CREATE TABLE IF NOT EXISTS data_stream_debezium_history (
    debezium_history_id BIGINT NOT NULL,
    server VARCHAR(128) NOT NULL COMMENT '服务名称',
    history_data LONGVARCHAR NOT NULL COMMENT '历史记录JSON数据',
    create_time DATETIME NOT NULL,
    PRIMARY KEY (debezium_history_id)
);

CREATE TABLE IF NOT EXISTS data_stream_mq_config (
    mq_config_id BIGINT NOT NULL COMMENT 'MQ配置ID',
    mq_config_name VARCHAR(100) NOT NULL COMMENT '实例名称',
    mq_type INT NOT NULL COMMENT 'MQ消息类型：10Kafka',
    bootstrap_servers VARCHAR(500) NOT NULL COMMENT 'MQ服务地址，多个地址用逗号分隔',
    message_format INT DEFAULT 1 COMMENT '报文格式：1-JSON格式，2-分隔符格式',
    delimiter VARCHAR(20) DEFAULT '|' COMMENT '分隔符（当message_format为2时使用）',
    topic_prefix VARCHAR(100) DEFAULT '' COMMENT 'Topic名称前缀',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    on_line_flag INT NOT NULL COMMENT '发布标志：0下线、1在线',
    state INT NOT NULL,
    state_date DATETIME NOT NULL,
    create_date DATETIME NOT NULL,
    PRIMARY KEY (mq_config_id)
);

CREATE INDEX IF NOT EXISTS idx_mq_config_name ON data_stream_mq_config(mq_config_name);
