/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.itman.datastream.common.constant;

public class DataStreamConstant {
    //2mysql、3orale、4postgres、5doris、6men、7h2、8text、9excel、10kafka
    public static final Integer DATA_SOURCE_TYPE_SHARDING = 1;
    public static final Integer DATA_SOURCE_TYPE_MYSQL = 2;
    public static final Integer DATA_SOURCE_TYPE_ORACLE = 3;
    public static final Integer DATA_SOURCE_TYPE_PG = 4;
    public static final Integer DATA_SOURCE_TYPE_DORIS = 5;
    public static final Integer DATA_SOURCE_TYPE_MEM = 6;
    public static final Integer DATA_SOURCE_TYPE_H2 = 7;
    public static final Integer DATA_SOURCE_TYPE_TEXT = 8;
    public static final Integer DATA_SOURCE_TYPE_EXCEL = 9;
    public static final Integer DATA_SOURCE_TYPE_KAFKA = 10;
    public static final Integer DATA_SOURCE_TYPE_ROCKETMQ = 11;
    public static final Integer DATA_SOURCE_TYPE_RABBITMQ = 12;
    public static final Integer DATA_SOURCE_TYPE_ACTIVEMQ = 13;

    //数据源基本分类：1数据库、2文件、3MQ消息队列
    public static final Integer DATA_SOURCE_CATEGORY_DATABASE=1;
    public static final Integer DATA_SOURCE_CATEGORY_FILE=2;
    public static final Integer DATA_SOURCE_CATEGORY_MQ=3;


    public static final int COMMON_STATE_OFFLINE = 1;
    public static final int COMMON_STATE_ONLINE = 2;
    public static final int COMMON_STATE_DELETED = 3;

    /**
     * 任务处理状态：0待处理、1处理中、2处理完成、3处理失败、4处理暂停
     */
    public static final Integer DATA_STREAM_TASK_STATE_INIT = 0;
    public static final Integer DATA_STREAM_TASK_STATE_RUNNING = 1;
    public static final Integer DATA_STREAM_TASK_STATE_FINISH = 2;
    public static final Integer DATA_STREAM_TASK_STATE_ERROR = 3;
    public static final Integer DATA_STREAM_TASK_STATE_STOP = 4;

    /**
     * 任务类型：1数据迁移、2数据删除、3迁移删除、4表结构迁移、5数据稽核、6增量迁移
     */
    public static final Integer DATA_STREAM_TASK_TYPE_DATA_MOVE = 1;
    public static final Integer DATA_STREAM_TASK_TYPE_DATA_DEL = 2;
    public static final Integer DATA_STREAM_TASK_TYPE_DATA_MOVE_DEL = 3;
    public static final Integer DATA_STREAM_TASK_TYPE_TABLE_MOVE = 4;
    public static final Integer DATA_STREAM_TASK_TYPE_DATA_CHECK = 5;
    public static final Integer DATA_STREAM_TASK_TYPE_DATA_CDC = 6;

    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_ID = 1;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_TABLE_NAME = 2;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_STATE = 3;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_TIME = 4;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_BATCH_TASK_ID = 5;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_COPY_TASK_ID = 6;
    public static final int QUERY_DATE_MOVE_TASK_FLAG_BY_TASK_TYPE = 7;

    public static final Integer TABLE_LINK_TASK_STATE_INIT = 0;
    public static final Integer TABLE_LINK_TASK_STATE_ING = 1;
    public static final Integer TABLE_LINK_TASK_STATE_SUCCESS = 2;
    public static final Integer TABLE_LINK_TASK_STATE_ERROR = 3;

    public static final Integer QUERY_DATE_LINK_TASK_FLAG_BY_TASK_ID = 1;
    public static final Integer QUERY_DATE_LINK_TASK_FLAG_BY_TASK_TYPE_AND_ID = 2;
    public static final Integer QUERY_DATE_LINK_TASK_FLAG_BY_STATE = 3;
    public static final Integer QUERY_DATE_LINK_TASK_FLAG_BY_TIME = 4;

    public static final String SEQ_LINK_TASK_TABLE_ID = "SEQ_LINK_TASK_TABLE_ID";
    public static final String SEQ_DATA_BASE_ID = "SEQ_DATA_BASE_ID";
    public static final String SEQ_MOVE_INFO_ID = "SEQ_MOVE_INFO_ID";
    //所有任务ID生成通过这个序列，方便统一任务ID在共享资源的应用不冲突
    public static final String SEQ_MOVE_TASK_ID = "SEQ_MOVE_TASK_ID";
    public static final String SEQ_SYSTEM_LOG_ID = "SEQ_SYSTEM_LOG_ID";
    public static final String SEQ_JOB_LOGBACK_ID = "SEQ_JOB_LOGBACK_ID";
    public static final String SEQ_TABLE_LINK_ID = "SEQ_TABLE_LINK_ID";
    public static final String SEQ_LINK_NODE_ID = "SEQ_LINK_NODE_ID";
    public static final String SEQ_METRICS_ID = "SEQ_METRICS_ID";
    public static final String SEQ_TASK_EXECUTE_ID = "SEQ_TASK_EXECUTE_ID";
    public static final String SEQ_TASK_EXTEND_ID = "SEQ_TASK_EXTEND_ID";
    public static final String SEQ_MOVE_TABLE_ID = "SEQ_MOVE_TABLE_ID";
    public static final String SEQ_DATA_CHECK_ID = "SEQ_DATA_CHECK_ID";
    public static final String SEQ_COLUMN_TYPE_TEST_ID = "SEQ_COLUMN_TYPE_TEST_ID";
    public static final String SEQ_FILE_FORMAT_ID = "SEQ_FILE_FORMAT_ID";
    public static final String SEQ_FILE_BODY_ID = "SEQ_FILE_BODY_ID";
    public static final String SEQ_FILE_SPECIAL_ID = "SEQ_FILE_SPECIAL_ID";
    public static final String SEQ_FILE_FIELD_ID = "SEQ_FILE_FIELD_ID";
    public static final String SEQ_DEBEZIUM_HISTORY_ID = "SEQ_DEBEZIUM_HISTORY_ID";
    public static final String SEQ_MQ_CONFIG_ID = "SEQ_MQ_CONFIG_ID";
    public static final String SEQ_COLUMN_TYPE_DEFINE_ID = "SEQ_COLUMN_TYPE_DEFINE_ID";
    public static final String SEQ_COLUMN_TYPE_MAP_ID = "SEQ_COLUMN_TYPE_MAP_ID";
    public static final String SEQ_SYSTEM_USER_ID = "SEQ_SYSTEM_USER_ID";
    public static final String SEQ_ROLE_ID = "SEQ_ROLE_ID";
    public static final String SEQ_USER_ROLE_ID = "SEQ_USER_ROLE_ID";
    public static final String SEQ_PERMISSION_ID = "SEQ_PERMISSION_ID";
    public static final String SEQ_ROLE_PERMISSION_ID = "SEQ_ROLE_PERMISSION_ID";

    public static final String SQL_FORMAT_HINT_DATANODE = "/* !HINT({\"dn\":[\"%s\"]})*/ ";

    public static final String SQL_FORMAT_HINT_BALANCE_DATANODE = "/* !HINT({\"balance\":\"%s\",\"dn\":[\"%s\"]})*/";

    public static final int UPDATE_DATA_MOVE_TASK_COLUMN_SOURCE_TABLE_COUNT = 1;
    public static final int UPDATE_DATA_MOVE_TASK_COLUMN_TARGET_BEGIN_COUNT = 2;
    public static final int UPDATE_DATA_MOVE_TASK_COLUMN_TARGET_END_COUNT = 3;

    public static final int QUERY_DATE_MOVE_INFO_FLAG_BY_TASK_ID = 1;
    public static final int QUERY_DATE_MOVE_INFO_FLAG_BY_INFO_ID = 2;

    public static final int OPERATE_DATE_MOVE_INFO_FLAG_BY_STOP = 1;
    public static final int OPERATE_DATE_MOVE_INFO_FLAG_BY_REDO = 2;
    public static final int OPERATE_DATE_MOVE_INFO_FLAG_BY_COPY = 3;

    public static final int MOVE_INFO_FLAG_SOURCE = 1;
    public static final int MOVE_INFO_FLAG_TARGET = 2;

    public static final String TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID = "data_stream_extend_01";
    public static final String TARGET_TABLE_ADD_COLUMNS_BACK_COUNT = "data_stream_extend_02";
    public static final String TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID = "data_stream_extend_03";
    public static final String FLOW_ROOT_PARENT_FIELD_BUSINESS_ID = "business_id";

    public static final int DATA_BASE_QUERY_FLAG_ALL = 1;
    public static final int DATA_BASE_QUERY_FLAG_TYPE = 2;
    public static final int DATA_BASE_QUERY_FLAG_ID = 3;

    public static final int TABLE_LINK_QUERY_FLAG_ALL = 1;
    public static final int TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID = 2;
    public static final int TABLE_LINK_QUERY_FLAG_LINK_NAME = 3;
    public static final int TABLE_LINK_QUERY_FLAG_TABLE_NAME = 4;
    public static final int TABLE_LINK_QUERY_FLAG_STATE = 5;

    public static final String SOURCE_DATA_TEST_FLOW_KEY_NAME = "testFlowKey";
    public static final String SOURCE_DATA_TEST_DATA_KEY_NAME = "testDataKey";
    public static final String SOURCE_DATA_META_DATA_KEY_NAME = "metadb";
    public static final String SOURCE_DATA_MOVE_SOURCE_KEY_NAME = "moveSourceKey";
    public static final String SOURCE_DATA_MOVE_TARGET_KEY_NAME = "moveTargetKey";
    public static final String SOURCE_DATA_LINK_SOURCE_KEY_NAME = "linkSourceKey";
    public static final String SOURCE_DATA_LINK_TARGET_KEY_NAME = "linkTargetKey";
    public static final String DATA_SEARCH_KEY_NAME = "dataSearchKey";

    public static final String SOURCE_WORKS_POOL_EXECUTOR = "SourceWorksPoolExecutor";
    public static final String TARGET_WORKS_POOL_EXECUTOR = "TargetWorksPoolExecutor";
    public static final String EVENT_WORKS_POOL_EXECUTOR = "EventWorksPoolExecutor";
    public static final String TASK_WORKS_POOL_EXECUTOR = "TaskWorksPoolExecutor";

    /**
     * 源端数据加载策略
     * 1:分页加载
     * 2:分段加载
     */
    public static final int LOAD_STRATEGY_BY_LIMIT_PAG = 1;
    public static final int LOAD_STRATEGY_BY_DATA_PART = 2;

    public static final int INSERT_MODE_BY_SPLICING = 1;
    public static final int INSERT_MODE_BY_BIND = 2;
    public static final int INSERT_MODE_BY_AUTO = 3;

    /**
     * 1异步模式：源端和目标端不同线程处理
     * 2同步模式：源端和目标端在同一个线程处理
     */
    public static final int SOURCE_SEND_MODE_ASYNC = 1;
    public static final int SOURCE_SEND_MODE_SYNC = 2;


    public static final int JOB_TYPE_MOVE_INFO = 1;
    public static final int JOB_TYPE_MOVE_LINK = 2;
    public static final int JOB_TYPE_MOVE_TASK = 3;
    public static final int JOB_TYPE_MOVE_TABLE = 4;

    /**
     * 状态：1(差异生成)、2(修订成功)、2(修订失败)
     */
    public static final int DATA_CHECK_STATE_CHECK_GEN = 1;
    public static final int DATA_CHECK_STATE_MODIFY_SUCCESS = 2;
    public static final int DATA_CHECK_STATE_MODIFY_FAIL = 3;

    /**
     * 稽核结果：1(源数据多)、2(数据不一致)、3(目标数据多)
     */
    public static final int DATA_CHECK_RESULT_SOURCE_MORE = 1;
    public static final int DATA_CHECK_RESULT_NOTEQUAL = 2;
    public static final int DATA_CHECK_RESULT_SOURCE_LESS = 3;

    /**
     * 稽核模式：1(正向)、2(双向)
     */
    public static final int DATA_CHECK_MODE_FORWARD = 1;
    public static final int DATA_CHECK_MODE_BIDIRECTIONAL = 2;

    /**
     * 字段类型分类：0未分类、1数值类型、2字符串类型、3日期时间类型等
     */
    public static final int COLUMN_TYPE_CLASSIFY_NONE = 0;
    public static final int COLUMN_TYPE_CLASSIFY_NUMERIC = 1;
    public static final int COLUMN_TYPE_CLASSIFY_STRING = 2;
    public static final int COLUMN_TYPE_CLASSIFY_DATETIME = 3;

    /**
     * 定义不同文件结构体里对应字段
     * 1 文件特殊行
     * 2 文件数据正文
     */
    public static final Integer FILE_LINE_SPECIAL = 1;
    public static final Integer FILE_LINE_BODY = 2;
    /**
     * 行间隔字符标记符：(1)固定长度、(2)竖线|、(3)逗号，、(4)与符号&
     */
    public static final int SPLIT_FLAG_FIX_WIDTH = 1;
    public static final int SPLIT_FLAG_VERTICAL_LINE = 2;
    public static final int SPLIT_FLAG_COMMA = 3;
    public static final int SPLIT_FLAG_AND = 4;

    /**
     * 符号标识：
     * 1(等于=)、
     * 2(大于号>)、
     * 3(小于号<)、
     * 4(大于等于号>=)、
     * 5(小于等于号<=)
     */
    public static final int SYMBOL_ID_EQUAL = 1;
    public static final int SYMBOL_ID_MORE = 2;
    public static final int SYMBOL_ID_LESS = 3;
    public static final int SYMBOL_ID_MORE_EQUAL = 4;
    public static final int SYMBOL_ID_LESS_EQUAL = 5;


    //可比较标志:1可参与比较、0或空不参与比较
    public static final int COMPARE_ABLE_FLAG_NO = 0;
    public static final int COMPARE_ABLE_FLAG_YES = 1;

    public static final String MAP_KEY_PREFIX = "mapKey=";

    /**
     * 比对标志，0未比较、1已比较
     */
    public static final int COMPARED_FLAG_NO = 0;
    public static final int COMPARED_FLAG_YES = 1;

    //固定名称
    public static final int FILE_NAME_TYPE_FIX = 1;
    //正则表达式名称,可能匹配多个文件
    public static final int FILE_NAME_TYPE_PATTERN = 2;
    //可扩展文件名称
    public static final int FILE_NAME_TYPE_EXTEND = 3;

    /**
     * 文件记录动作：1删除、2上线、3下线、4复制、5校验、6ftp测试
     */
    public static final int DATA_STREAM_ACTION_DELETE = 1;
    public static final int DATA_STREAM_ACTION_ONLINE = 2;
    public static final int DATA_STREAM_ACTION_OFFLINE = 3;
    public static final int DATA_STREAM_ACTION_COPY = 4;
    public static final int DATA_STREAM_ACTION_CHECK = 5;
    public static final int DATA_STREAM_ACTION_FTP = 6;

    //前端页面处理方式：1展示、2编辑
    public static final int VIEW_FLAG_SHOW = 1;
    public static final int VIEW_FLAG_EDIT = 2;

    public static final int FILE_FORMAT_QUERY_FLAG_ALL = 1;
    public static final int FILE_FORMAT_QUERY_FLAG_FILE_FORMAT_ID = 2;
    public static final int FILE_FORMAT_QUERY_FLAG_FILE_NAME = 3;

    public static final String HANDLE_TYPE_NONE = "none";
    public static final String HANDLE_TYPE_READ = "read";
    public static final String HANDLE_TYPE_INSERT = "insert";
    public static final String HANDLE_TYPE_UPDATE = "update";
    public static final String HANDLE_TYPE_DELETE = "delete";

    /**
     * offset存储：1数据库、2文件、3kakfa
     */
    public static final int OFFSET_STORAGE_DATABASE = 1;
    public static final int OFFSET_STORAGE_FILE = 2;
    public static final int OFFSET_STORAGE_KAKFA = 3;

    /**
     * 增量同步对象：1表数据、2表结构、3表结构和数据
     */
    public static final int SOURCE_DEBEZIUM_OBJECT_DATA = 1;
    public static final int SOURCE_DEBEZIUM_OBJECT_SCHEMA = 2;
    public static final int SOURCE_DEBEZIUM_OBJECT_SCHEMA_AND_DATA = 3;

    /**
     * 1按dataCheckId、2按taskId
     */

    public static final int CHECK_TYPE_BY_CHECK_ID = 1;
    public static final int CHECK_TYPE_BY_TASK_ID = 2;

    public static final int MQ_CONFIG_QUERY_FLAG_ALL = 1;
    public static final int MQ_CONFIG_QUERY_FLAG_MQ_CONFIG_ID = 2;
    public static final int MQ_CONFIG_QUERY_FLAG_FILE_NAME = 3;


    /**
     * 报文格式：JSON格式
     */
    public static final int MESSAGE_FORMAT_JSON = 1;

    /**
     * 报文格式：分隔符格式
     */
    public static final int MESSAGE_FORMAT_DELIMITER = 2;

    /**
     * 数据库处理对象类型：表对象1、schema对象2
     */
    public static final int MOVE_TABLE_OBJECT_TYPE_TABLE=1;
    public static final int MOVE_TABLE_OBJECT_TYPE_SCHEMA=2;

    /**
     * 消息数据对象类型：1(CDC消息数据)、2(CDC消息结构)
     */
    public static final int MQ_MESSAGE_DATA_TYPE_CDC_DML = 1;
    public static final int MQ_MESSAGE_DATA_TYPE_CDC_DDL = 2;
    public static final int MQ_MESSAGE_DATA_TYPE_MAP = 3;

}
