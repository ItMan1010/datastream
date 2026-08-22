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
--See the License for the specific language governing permissions and
--limitations under the License.

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

create table if not exists `data_stream_data_base` (
    `data_base_id` bigint(18) not null comment '主键标识，序列名称：seq_data_base_id',
    `data_base_type` int(2) not null comment '数据库类型',
    `data_base_name` varchar(128) not null comment '数据库名称',
    `url` varchar(512) not null comment '数据库链接',
    `user_name` varchar(128) not null comment '数据库用户名',
    `pass_word` varchar(128) not null comment '数据库密码',
    `table_key_not_supported` int(2) default null comment '数据不支持表主键：1:不支持，其他默认支持',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) not null comment '状态：0(删除)、1(下线)、2(上线)',
    `state_date` datetime not null comment '状态时间',
    primary key (`data_base_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据库配置表';

create table if not exists `data_stream_move_task` (
    `task_id` bigint(18) not null comment '主键标识，序列：seq_move_task_id',
    `task_type` int(2) not null comment '任务类型：1数据迁移、2数据删除、3数据迁移删除、4表结构迁移、5数据稽核',
    `source_object_type` bigint(18) not null comment '源数据对象类型：2mysql、3orale、4postgres、5doris、6men、7h2、8text、9excel',
    `source_object_id` bigint(18) not null comment '源数据对象标识',
    `source_object_name` varchar(256) not null comment '源对象名',
    `source_object_condition` varchar(256) default null comment '源扩展条件:支持简单数据过滤条件',
    `source_object_count` bigint(18) default null comment '源对象记录数',
    `source_object_keys` varchar(128) default null comment '源对象唯一主键字段',
    `source_keys_begin` varchar(128) default null comment '源对象唯一主键开始值',
    `source_keys_end` varchar(128) default null comment '源对象唯一主键结束值',
    `source_load_strategy` int(2) not null comment '源对象加载数据策略:1:分页加载、2分段加载',
    `source_data_node` varchar(256)  default null comment '源对象:分片节点，支持多个节点数据并发处理',
    `source_data_set` int(2) default null ,
    `target_object_type` bigint(18) not null comment '源数据对象类型：2mysql、3orale、4postgres、5doris、6men、7h2、8text、9excel',
    `target_object_id` bigint(18) not null comment '目标对象标识',
    `target_object_name` varchar(256) not null comment '目标对象名',
    `target_object_begin_count` bigint(18) default null comment '目标对象迁入前记录数',
    `target_object_end_count` bigint(18) default null comment '目标对象迁入后记录数',
    `target_insert_mode` int(2) not null comment '数据目标端写入方式:1(拼接sql方式)、2(绑定变量方式),如果为空按1方式',
    `send_mode` int(2) not null comment '数据分发模型:1异步、2同步，默认异步方式',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) default null comment '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    `state_date` datetime not null comment '状态时间',
    `task_disc` varchar(1024) default null comment '任务描述',
    `error_code` varchar(128) default null comment '任务错误编码',
    `error_msg` varchar(128) default null comment '任务错误信息',
    `priority` int(2) not null comment '数据迁移任务优先级（数字越小优先级越大）',
    `system_user_code` varchar(256) default null comment '用户编码',
    `batch_task_id` bigint(18) default null comment '批量任务标识',
    `copy_task_id` bigint(18) default null comment '被复制任务标识',
    primary key (`task_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='任务实例表';

create table if not exists `data_stream_move_table` (
    `move_table_id` bigint(18) not null comment '主键标识，序列：seq_move_table_id',
    `task_id` bigint(18) not null comment '任务标识',
    `source_table_name` varchar(256) not null comment '源表名',
    `table_sql` varchar(10240) default null comment '生成建表脚本',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) default null comment '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    `state_date` datetime not null comment '状态时间',
    `error_code` varchar(128) default null comment '任务错误编码',
    `error_msg` varchar(128) default null comment '任务错误信息',
    primary key (`move_table_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='表结构迁移关联表';

create table if not exists `data_stream_task_extend` (
    `task_extend_id` bigint(18) not null comment '主键标识，序列：seq_task_extend_id',
    `task_id` bigint(18) not null comment '任务标识',
    `create_date` datetime not null comment '记录生成时间',
    `parameter_name` varchar(128) default null comment '参数名称',
    `parameter_value` varchar(32) default null comment '参数值',
    primary key (`task_extend_id`),
    key `idx_data_stream_task_extend_01` (`task_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='任务执行参数表';


create table if not exists `data_stream_task_execute` (
    `task_execute_id` bigint(18) not null comment '主键标识，序列：seq_task_execute_id',
    `task_id` bigint(18) not null comment '任务标识',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) default null comment '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    `state_date` datetime not null comment '状态时间',
    `error_code` varchar(128) default null comment '任务错误编码',
    `error_msg` varchar(128) default null comment '任务错误信息',
    `system_user_code` varchar(256) default null comment '员工编码',
    `host_name` varchar(128) default null comment '处理主机名称',
    `host_ip` varchar(20) default null comment '处理主机ip',
    primary key (`task_execute_id`),
    key `idx_data_stream_task_execute_01` (`task_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='任务执行记录表';

create table if not exists `data_stream_data_check` (
    `data_check_id` bigint(18) not null comment '主键标识，序列：seq_data_check_id',
    `task_id` bigint(18) not null comment '任务标识',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) default null comment '状态：1(稽核生成)、2(修订成功)、2(修订失败)',
    `state_date` datetime not null comment '状态时间',
    `check_result` int(2) default null comment '稽核结果：1(源数据多)、2(数据不一致)、3(目标数据多)',
    `check_keys` varchar(128) default null comment '稽核主键字段',
    `error_code` varchar(128) default null comment '任务错误编码',
    `error_msg` varchar(128) default null comment '任务错误信息',
    primary key (`data_check_id`),
    key `idx_data_stream_task_check_01` (`task_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据稽核结果记录表';

create table if not exists `data_stream_move_info` (
    `info_id` bigint(18) not null comment '主键标识，序列：seq_move_info_id',
    `info_flag` int(2) not null comment '运行标识:1源端线程生成、2目标端线程生成',
    `task_id` bigint(18) not null comment '数据迁移任务标识',
    `table_name` varchar(256) not null comment '数据迁移表名',
    `data_node` varchar(256) default null comment 'teledb分片节点，支持多个节点数据并发处理',
    `create_date` datetime not null comment '记录生成时间',
    `virtual_id` int(2) not null comment '启动线程序列标识',
    `thread_name` varchar(128) not null comment '处理线程名称',
    `data_count` bigint(18) not null comment '记录数',
    `data_actual_count` bigint(18) not null comment '实际记录数',
    `loop_count` int(9) not null comment '循环次数',
    `max_cost` bigint(18) not null comment '最大耗时',
    `min_cost` bigint(18) not null comment '最小耗时',
    `sum_cost` bigint(18) not null comment '总耗时',
    `lately_cost` bigint(18) not null comment '最新耗时',
    `page_row_start` varchar(128) not null comment '数据加载开始,分段记录主键值、分页记录页码',
    `page_row_end` varchar(128) not null comment '数据加载结束,分段记录主键值、分页记录页码',
    `page_loop_count` int(9) not null comment '数据翻页迭代次数',
    `state` int(2) not null comment '状态：0(初始状态)、1(迁移中)、2(迁移成功)、3(迁移失败)、4(迁移暂停)',
    `state_date` datetime not null comment '状态时间',
    `error_code` varchar(128) default null comment '线程错误编码',
    `error_msg` varchar(128) default null comment '线程错误信息',
    primary key (`info_id`),
    key `idx_data_stream_move_info_01` (`task_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据迁移运行记录表,调试分析数据分割情况';

create table if not exists `data_stream_move_trace` (
    `trace_id` bigint(18) not null comment '主键标识，序列：seq_move_trace_id',
    `info_id` bigint(18) not null comment '数据迁移运行标识',
    `task_id` bigint(18) not null comment '数据迁移任务标识',
    `page_row_start` varchar(128) default null comment '记录开始',
    `page_row_end` varchar(128) default null comment '记录结束',
    `data_count` bigint(18) default null comment '记录数',
    `data_actual_count` bigint(18) default null comment '实际记录数',
    `create_date` datetime not null comment '记录生成时间',
    primary key (`trace_id`),
    key `idx_data_stream_move_trace_01` (`info_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据迁移运行轨迹';

create table if not exists `data_stream_link_task` (
    `link_task_id` bigint(18) not null comment '主键标识，序列：seq_move_task_id',
    `table_link_id` bigint(18) default null comment '链接标识',
    `business_id` bigint(18) default null comment '业务流水',
    `source_data_base_id` bigint(18) not null comment '源库标识',
    `target_data_base_id` bigint(18) not null comment '目标库标识',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) not null comment '数据迁移状态：0(等待迁移)、1(回迁中)、2(回迁成功)、3(回迁失败)',
    `state_date` datetime not null comment '状态时间',
    `task_disc` varchar(1024) default null comment '任务描述',
    `error_code` varchar(32) default null comment '回迁失败错误码',
    `error_msg` varchar(256) default null comment '回迁失败信息',
    `system_user_code` varchar(256) default null comment '员工编码',
    `host_name` varchar(128) default null comment '处理主机名称',
    `host_ip` varchar(20) default null comment '处理主机ip',
    primary key (`link_task_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='链接迁移任务表';

create table if not exists `data_stream_link_task_table` (
    `link_task_table_id` bigint(18) not null comment '主键标识，序列：seq_link_task_table_id',
    `link_task_id` bigint(18) not null comment '关联链接任务表示',
    `select_table_name` varchar(256) default null comment '查询模型名称',
    `select_count` int(9) default null comment '查询记录数',
    `insert_table_name` varchar(256) default null comment '插入模型名称',
    `insert_count` int(9) default null comment '插入记录数',
    `create_date` datetime not null comment '记录生成时间',
    primary key (`link_task_table_id`),
    key `idx_data_stream_link_table_01` (`link_task_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='链接迁移任务关联模型';

create table if not exists `data_stream_table_link` (
    `table_link_id` bigint(18) not null comment '链接定义标识，序列名称：seq_table_link_id',
    `table_link_name` varchar(30) not null comment '链接名称',
    `table_link_des` varchar(128) default null comment '链接描述',
    `create_date` datetime not null comment '记录生成时间',
    `state` int(2) not null comment '状态：0(删除)、1(下线)、2(上线)',
    `state_date` datetime not null comment '状态时间',
    primary key (`table_link_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='表链接定义表';

create table if not exists `data_stream_link_node` (
    `link_node_id` bigint(18) not null comment '链接节点标识，序列名称：seq_link_node_id',
    `table_link_id` bigint(18) not null comment '链接定义标识',
    `table_name` varchar(128) not null comment '表名称',
    `field_name` varchar(128) not null comment '数据查询字段名称',
    `parent_field_name` varchar(128) not null comment '父级数据字段名称，如果是顶层节点',
    `parent_link_node_id` bigint(18) not null comment '父级节点标识，-1表示顶层节点',
    `pos_x` int(9) not null comment '节点坐标x',
    `pos_y` int(9) not null comment '节点坐标y',
    `create_date` datetime not null comment '记录生成时间',
    primary key (`link_node_id`),
    key `idx_data_stream_link_node_01` (`table_link_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='链接节点配置表';

create table if not exists `data_stream_system_log` (
    `system_log_id` bigint(18) not null comment '主键标识，序列名称：seq_system_log_id',
    `type` bigint(18) not null comment '1登录日志、2操作日志',
    `username` varchar(30) default null comment '操作工号',
    `ip_address` varchar(30) default null comment '操作端ip地址',
    `module_name` varchar(30) default null comment '操作模块',
    `content` varchar(128) default null comment '操作内容',
    `url_path` varchar(128) default null comment '操作地址',
    `user_agent` varchar(128) default null comment '操作浏览器',
    `request_info` varchar(1024) default null comment '输入信息',
    `response_info` varchar(1024) default null comment '输出信息',
    `elapse` bigint(18) default null comment '耗时(毫秒)',
    `result` varchar(10) default null comment '执行结果(成功/失败)',
    `create_date` datetime not null comment '生成时间',
    primary key (`system_log_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='系统操作日志';

create table if not exists `data_stream_job_logback` (
    `job_logback_id` bigint(18) not null comment '主键标识，序列名称：seq_job_logback_id',
    `job_type` bigint(18) not null comment '1数据运行、2数据回迁',
    `job_id` bigint(18) not null comment '关联id',
    `content` longtext default null comment '操作内容',
    `create_date` datetime not null comment '生成时间',
    primary key (`job_logback_id`),
    key `idx_data_stream_job_logback_01` (`job_id`,job_type)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='任务运行报错日志';

create table if not exists `data_stream_session` (
    `token_key` varchar(2048) character set ascii collate ascii_bin not null comment 'token',
    `username` varchar(30) not null comment '用户名',
    `create_date` datetime not null comment '生成时间',
    `expire_date` datetime not null comment '失效时间',
    `state` int(2) not null comment '状态：1(生成)、2(删除)',
    primary key (`token_key`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='系统session日志';

create table if not exists `data_stream_table_map` (
    `table_map_id` bigint(18) not null comment '主键标识，序列名称：seq_table_map_id',
    `table_name` varchar(128) not null comment '表名称',
    `table_type` int(2) not null comment '表类型,a_split_table_config.table_type',
    primary key (`table_map_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='分表类型映射';

create table if not exists `data_stream_metrics` (
    `metrics_id` bigint(18) not null comment '主键标识，序列名称：seq_metrics_id',
    `task_id` bigint(18) not null comment '任务标识',
    `metrics_time` bigint(18) not null comment '指标时刻',
    `metrics_value` bigint(18) not null comment '指标值',
    primary key (`metrics_id`),
    key `idx_data_stream_metrics_01` (`task_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='指标监控表';

CREATE TABLE `data_stream_column_type_define` (
    `column_type_define_id` bigint(20) NOT NULL COMMENT '主键标识，序列名称：seq_column_type_define_id',
    `database_type` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '数据库类型',
    `column_type_classify` int(2) NOT NULL COMMENT '字段类型分类：0未分类、1数值类型、2字符串类型、3日期时间类型等',
    `column_type_name` varchar(32) COLLATE utf8mb4_bin NOT NULL COMMENT '字段类型名称,全都小写',
    `column_standard_size` bigint(20) DEFAULT NULL COMMENT '字段类型标准长度(如mysql长整型长度19)，如果是null是非标准自定义设置',
    `remark` varchar(1024) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '备注说明',
    `type_category` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '类型分类：整数/小数/浮点/字符串/日期/二进制/布尔/json/数组/uuid/其他',
    `max_precision` int(11) DEFAULT NULL COMMENT '最大精度（整数位数）',
    `max_scale` int(11) DEFAULT NULL COMMENT '最大小数位数',
    `character_max_length` bigint(20) DEFAULT NULL COMMENT '字符串最大长度',
    `min_value` bigint(20) DEFAULT NULL COMMENT '最小值',
    `max_value` bigint(20) DEFAULT NULL COMMENT '最大值',
    `is_national_flag` int(2) DEFAULT '0' COMMENT '是否支持字符集(如 nvarchar)',
    `require_length_param` int(2) DEFAULT '0' COMMENT '是否必须指定长度参数',
    primary key  (`column_type_define_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='数据库字段类型名称定义';

create table if not exists `data_stream_column_type_map` (
    `column_type_map_id` bigint(18) not null comment '主键标识，序列名称：seq_column_type_map_id',
    `column_type_define_id_a` bigint(18) not null comment '数据库类型a',
    `column_type_define_id_b` bigint(18) not null comment '数据库类型b',
    `match_level` int default 1 comment '匹配级别：1-精确匹配，2-兼容匹配，3-降级匹配',
    `precision_conversion_rule` varchar(200) default NULL comment '精度转换规则表达式',
    `length_conversion_rule` varchar(200) default NULL comment '长度转换规则表达式',
    `conversion_warning` varchar(500) default NULL comment '转换警告信息',
    `is_reversible` int default 1 comment '是否可逆转换（双向无损）',
    primary key (`column_type_map_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据库字段类型名称应映射';

create table if not exists `data_stream_column_type_test` (
    `column_type_test_id` bigint not null comment '主键标识，序列名称：seq_column_type_test_id',
    `database_type` varchar(32) not null comment '数据库类型',
    `table_name` varchar(32) not null comment '表名称,全都小写',
    `column_name` varchar(32) not null comment '字段名称,全都小写',
    `column_type_name` varchar(32) not null comment '字段类型名称,全都小写',
    `column_standard_size` bigint default null comment '字段类型标准长度(如mysql长整型长度19)，如果是null是非标准自定义设置',
    `remark` varchar(1024) default null comment '备注说明',
    primary key (`column_type_test_id`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='数据库字段类型名称测试';


create table if not exists `data_stream_file_format`(
    `file_format_id` bigint(18) not null,
    `file_type` int(2) default null comment '文件类型：1文本、2excel',
    `file_name_type` int(2) not null,
    `file_name_format` varchar(128) COLLATE utf8mb4_bin not null,
    `ftp_host` varchar(128) collate utf8mb4_bin default null comment '远程主机IP',
    `ftp_port` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP端口',
    `ftp_user` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP用户名',
    `ftp_passwd` varchar(128) collate utf8mb4_bin default null comment '远程主机FTP密码',
    `ftp_path` varchar(128) collate utf8mb4_bin default null comment '远程主机数据路径',
    `local_path` varchar(128) collate utf8mb4_bin default null,
    `file_bak_action` int(2) not null,
    `file_bak_path` varchar(128) collate utf8mb4_bin default null,
    `create_date` datetime not null,
    `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`file_format_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象基本定义';

create table if not exists `data_stream_file_body`(
    `file_body_id` bigint(18) not null,
    `file_format_id` bigint(18) not null,
    `split_flag` bigint(18) not null,
    `fix_begin_line` int(2) default null,
    `fix_end_line` int(2) default null,
    `create_date` datetime not null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`file_body_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象行体定义';

create table if not exists `data_stream_file_field`(
    `file_field_id` bigint(18) not null,
    `file_format_id` bigint(18) not null,
    `belong_flag` bigint(18) not null comment '1 文件特殊行、2 文件数据正文',
    `belong_id` int(2) not null,
    `field_name` varchar(128) collate utf8mb4_bin not null,
    `fix_width` int(2) default null,
    `position` int(2) default null,
    `sum_line_flag` int(2) default null  comment '用于特殊行定义：1表示该字段记录总行数',
    `sum_field_name` varchar(128) collate utf8mb4_bin default null comment '如果不为空,记录行体用于累加字段名称',
    `create_date` datetime not null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`file_field_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象字段定义';

create table if not exists `data_stream_file_filter`(
    `file_filter_id` bigint(18) not null,
    `file_format_id` bigint(18) not null,
    `file_field_id` bigint(18) not null,
    `symbol_id` int(2) not null comment '1(等于=)、2(大于号>)、3(小于号<)、4(大于等于号>=)、3(小于等于号<=)',
    `symbol_group` int(2) not null comment '同一组条件是与关系，不同组条件是或关系',
    `file_field_value` varchar(128) collate utf8mb4_bin not null,
    `create_date` datetime not null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`file_filter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件过滤规则定义';

create table if not exists `data_stream_file_special`(
    `file_special_id` bigint(18) not null,
    `file_format_id` bigint(18) not null,
    `split_flag` int(2) not null,
    `fix_line_position` int(2) not null,
    `remark` varchar(128) collate utf8mb4_bin default null,
    `create_date` datetime not null,
    `state` int(2) not null,
    `state_date` datetime not null,
    primary key (`file_special_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='文件对象特殊行定义';

CREATE TABLE `data_stream_debezium_offsets` (
    `offset_key` varchar(1024) NOT NULL COMMENT '偏移量键，字符串格式',
    `offset_value` varchar(1024) NOT NULL COMMENT '偏移量值',
    `offset_count` bigint(18) not null  COMMENT '同步次数',
    `create_time` datetime not null,
    `update_time` datetime not null,
    PRIMARY KEY (`offset_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment='增量同步偏移量记录';

CREATE TABLE IF NOT EXISTS data_stream_debezium_history (
    debezium_history_id bigint(18) not null,
    server varchar(128) NOT NULL COMMENT '服务名称',
    history_data longtext NOT NULL COMMENT '历史记录JSON数据',
    create_time datetime not null,
    primary key (`debezium_history_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Debezium数据库历史记录表';

CREATE TABLE IF NOT EXISTS `data_stream_mq_config` (
    `mq_config_id` bigint(20) NOT NULL COMMENT 'MQ配置ID',
    `mq_config_name` varchar(100) NOT NULL COMMENT '实例名称',
    `mq_type` int(2) not null comment 'MQ消息类型：10Kafka',
    `bootstrap_servers` varchar(500) NOT NULL COMMENT 'MQ服务地址，多个地址用逗号分隔',
    `message_format` int(11) DEFAULT 1 COMMENT '报文格式：1-JSON格式，2-分隔符格式',
    `delimiter` varchar(20) DEFAULT '|' COMMENT '分隔符（当message_format为2时使用）',
    `topic_prefix` varchar(100) DEFAULT '' COMMENT 'Topic名称前缀',
    `remark` varchar(500) DEFAULT '' COMMENT '备注',
    `on_line_flag` int(2) not null comment '发布标志：0下线、1在线',
    `state` int(2) not null,
    `state_date` datetime not null,
    `create_date` datetime not null,
    PRIMARY KEY (`mq_config_id`),
    KEY `idx_mq_config_name` (`mq_config_name`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息配置表';


create table if not exists `data_stream_sequence` (
                                                      `seq_name` varchar(50) not null,
    `current_val` int not null,
    `increment_val` int not null default '1',
    primary key (`seq_name`)
    ) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='序列记录表';

/*序列函数创建*/
create function `currseq`(v_seq_name varchar(50)) returns int
    deterministic
begin
 declare value integer;
 set value = 0;
select current_val into value  from data_stream_sequence where seq_name = v_seq_name;
return value;
end

create function `nextseq`(v_seq_name varchar(50)) returns int
    deterministic
begin
update data_stream_sequence set current_val = current_val + increment_val  where seq_name = v_seq_name;
return currseq(v_seq_name);
end

/*序列表数据插入*/
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_DATA_BASE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_LINK_TASK_TABLE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_MOVE_INFO_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_MOVE_TASK_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_SYSTEM_LOG_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_JOB_LOGBACK_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_MOVE_TRACE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_TASK_EXECUTE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_METRICS_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_TASK_EXTEND_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_MOVE_TABLE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_DATA_CHECK_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_COLUMN_TYPE_TEST_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_COLUMN_TYPE_DEFINE_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_COLUMN_TYPE_MAP_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_TABLE_LINK_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_LINK_NODE_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_FILE_FORMAT_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_FILE_BODY_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_FILE_FIELD_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_FILE_SPECIAL_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_DEBEZIUM_HISTORY_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_MQ_CONFIG_ID', 1, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_SYSTEM_USER_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_ROLE_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_USER_ROLE_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_PERMISSION_ID', 50000, 1);
INSERT INTO `data_stream_sequence` (`seq_name`, `current_val`, `increment_val`) VALUES ('SEQ_ROLE_PERMISSION_ID', 50000, 1);

-- ==================== 权限管理（RBAC）====================

create table if not exists `data_stream_system_user` (
    `system_user_id` bigint not null comment '主键标识，序列名称：SEQ_SYSTEM_USER_ID',
    `system_user_code` varchar(64) not null comment '登录账号（唯一）',
    `system_user_name` varchar(128) default null comment '显示名',
    `password` varchar(128) not null comment '登录密码（BCrypt密文）',
    `org_id` bigint default null comment '机构标识',
    `org_name` varchar(128) default null comment '机构名称',
    `username` varchar(128) default null comment '用户名（兼容旧字段）',
    `state` int not null comment '状态：0禁用、1启用',
    `create_date` datetime not null comment '创建时间',
    `update_date` datetime default null comment '更新时间',
    primary key (`system_user_id`),
    unique key `uk_data_stream_system_user_code` (`system_user_code`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='系统用户表';

create table if not exists `data_stream_role` (
    `role_id` bigint not null comment '主键标识，序列名称：SEQ_ROLE_ID',
    `role_code` varchar(64) not null comment '角色编码（唯一）',
    `role_name` varchar(128) not null comment '角色名称（唯一）',
    `description` varchar(256) default null comment '角色描述',
    `built_in` int not null default 0 comment '是否内置：0否、1是',
    `create_date` datetime not null comment '创建时间',
    `update_date` datetime default null comment '更新时间',
    primary key (`role_id`),
    unique key `uk_data_stream_role_code` (`role_code`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='角色表';

create table if not exists `data_stream_user_role` (
    `user_role_id` bigint not null comment '主键标识，序列名称：SEQ_USER_ROLE_ID',
    `system_user_id` bigint not null comment '用户标识',
    `role_id` bigint not null comment '角色标识',
    primary key (`user_role_id`),
    key `idx_data_stream_user_role_user` (`system_user_id`),
    key `idx_data_stream_user_role_role` (`role_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='用户角色关联表';

create table if not exists `data_stream_permission` (
    `permission_id` bigint not null comment '主键标识，序列名称：SEQ_PERMISSION_ID',
    `permission_code` varchar(128) not null comment '权限编码（唯一，如 task:create）',
    `permission_name` varchar(128) not null comment '权限名称',
    `permission_type` int not null comment '权限类型：1菜单、2数据操作',
    `parent_id` bigint default null comment '父权限标识（菜单树）',
    `sort_no` int default 0 comment '排序号',
    `route` varchar(128) default null comment '菜单路由标识',
    `built_in` int not null default 0 comment '是否内置：0否、1是',
    primary key (`permission_id`),
    unique key `uk_data_stream_permission_code` (`permission_code`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='权限资源表';

create table if not exists `data_stream_role_permission` (
    `role_permission_id` bigint not null comment '主键标识，序列名称：SEQ_ROLE_PERMISSION_ID',
    `role_id` bigint not null comment '角色标识',
    `permission_id` bigint not null comment '权限标识',
    primary key (`role_permission_id`),
    key `idx_data_stream_role_permission_role` (`role_id`),
    key `idx_data_stream_role_permission_permission` (`permission_id`)
) engine=innodb default charset=utf8mb4 collate=utf8mb4_bin comment='角色权限关联表';

-- mysql数据库字段类型
CREATE TABLE data_stream_mysql_table_demo (
    -- 数值类型
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    tiny_int_field TINYINT NOT NULL DEFAULT 0 COMMENT '8位整数',
    small_int_field SMALLINT COMMENT '16位整数',
    medium_int_field MEDIUMINT COMMENT '24位整数',
    int_field INT COMMENT '32位整数',
    big_int_field BIGINT COMMENT '64位整数',
    float_field FLOAT(7,4) COMMENT '单精度浮点数',
  double_field DOUBLE(15,8) COMMENT '双精度浮点数',
  decimal_field DECIMAL(10,2) COMMENT '精确小数',
  -- 日期时间类型
  date_field DATE COMMENT '日期',
  time_field TIME COMMENT '时间',
  datetime_field DATETIME COMMENT '日期时间',
  timestamp_field TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '时间戳',
  year_field YEAR COMMENT '年份',
  -- 字符串类型
  char_field CHAR(10) COMMENT '定长字符串',
  varchar_field VARCHAR(255) COMMENT '变长字符串',
  binary_field BINARY(20) COMMENT '定长二进制',
  varbinary_field VARBINARY(100) COMMENT '变长二进制',
  -- 文本类型
  tinytext_field TINYTEXT COMMENT '短文本',
  text_field TEXT COMMENT '普通文本',
  mediumtext_field MEDIUMTEXT COMMENT '中等文本',
  longtext_field LONGTEXT COMMENT '长文本',
  -- BLOB类型
  blob_field BLOB COMMENT '二进制大对象',
  longblob_field LONGBLOB COMMENT '大型二进制对象',
  -- 特殊类型
  enum_field ENUM('A','B','C') DEFAULT 'A' COMMENT '枚举类型',
  set_field SET('Red','Green','Blue') COMMENT '集合类型',
  json_field JSON COMMENT 'JSON数据',
  bit_field BIT(8) COMMENT '位字段',
  -- 空间数据类型 (MySQL 5.7+)
  point_field POINT COMMENT '点坐标',
  geometry_field GEOMETRY COMMENT '几何对象',
  -- 索引示例
  INDEX idx_varchar (varchar_field(20)),
  UNIQUE INDEX uniq_int (int_field)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MySQL全数据类型演示表';

---postgres数据字段类型
CREATE TABLE data_stream_postgres_table_demo (
    -- 数值类型
  id SERIAL PRIMARY KEY,                           -- 自增主键
  small_int_col SMALLINT,                         -- 小整数
  int_col INTEGER,                                -- 普通整数
  big_int_col BIGINT,                             -- 大整数
  decimal_col DECIMAL(10,2),                      -- 精确小数
  numeric_col NUMERIC(8,4),                       -- 任意精度数
  real_col REAL,                                  -- 单精度浮点
  ouble_col DOUBLE PRECISION,                   -- 双精度浮点
    -- 字符类型
  fixed_char_col CHAR(3),                         -- 固定长度字符
  varchar_col VARCHAR(100),                       -- 可变长度字符
  text_col TEXT,                                  -- 大文本数据
    -- 日期时间类型
  date_col DATE,                                  -- 日期
  time_col TIME,                                  -- 时间
  timestamp_col TIMESTAMP,                        -- 时间戳
   timestamptz_col TIMESTAMPTZ,                   -- 带时区时间戳
  interval_col INTERVAL,                          -- 时间间隔
    -- 布尔类型
  boolean_col BOOLEAN,                            -- 布尔值
    -- 二进制类型
  bytea_col BYTEA,                                -- 二进制数据
    -- JSON类型
  json_col JSON,                                  -- JSON数据
  jsonb_col JSONB,                                -- 二进制JSON
    -- 数组类型
   int_array INTEGER[],                            -- 整数数组
  text_array TEXT[],                              -- 文本数组
    -- 网络地址类型
  inet_col INET,                                  -- IP地址
  cidr_col CIDR,                                  -- 网络地址
    -- 特殊类型
  uuid_col UUID,                                  -- 全局唯一标识
  money_col MONEY,                                -- 货币类型
    -- 几何类型（需PostGIS扩展）
  point_col POINT,                                -- 点坐标
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP  -- 创建时间
);




