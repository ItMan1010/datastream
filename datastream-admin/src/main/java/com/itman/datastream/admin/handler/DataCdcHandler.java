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
package com.itman.datastream.admin.handler;

import com.alibaba.fastjson.JSON;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.admin.service.IMoveTargetService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IFileApi;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.AESUtils;
import com.itman.datastream.engine.holder.DataStreamHolder;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.*;
import static com.itman.datastream.common.utils.CommUtils.isFileDataSource;

@Component
@Slf4j
public class DataCdcHandler extends AbstractHandler {
    private final IMetaService metaService;
    private final DataStreamHolder dataStreamHolder;


    public DataCdcHandler(DataSourceFactory dataSourceFactory, DataStreamConfig dataStreamConfig, IMoveTargetService moveTargetService, IMetaService metaService, DataStreamHolder dataStreamHolder, DataStreamHolder dataStreamHolder1) {
        super(dataSourceFactory, dataStreamConfig, metaService, dataStreamHolder, moveTargetService);
        this.metaService = metaService;
        this.dataStreamHolder = dataStreamHolder;
    }

    private final ConcurrentHashMap<Long, DebeziumEngine<ChangeEvent<String, String>>> debeziumEngineMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ExecutorService> debeziumExecutorMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Exception> debeziumErrorResultMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, ConcurrentHashMap<String, DebeziumTableEntity>> debeziumTableMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> debeziumSourceRecordCountMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicLong> debeziumTargetRecordCountMap = new ConcurrentHashMap<>();


    public void startDebeziumEngine(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        String sourceObjectName = dataMoveTask.getSourceObjectName();
        DataBaseEntity sourceDataBase = dataMoveTask.getSourceDataBase();
        String sourceOffsetKafka = dataMoveTask.getSourceOffsetKafka();
        Integer sourceOffsetStorage = dataMoveTask.getSourceOffsetStorage();
        String sourceOffsetStartPos = dataMoveTask.getSourceOffsetStartPos();
        Integer sourceDebeziumSnapshot = dataMoveTask.getSourceDebeziumSnapshot();

        String engineName = genEngineName(dataMoveTask.getTaskId());
        String sourceSchemaName = sourceDataBase.getSchemaName();
        String hostName = parseHostJdbcUrl(sourceDataBase.getUrl());
        Integer port = Integer.parseInt(parsePortJdbcUrl(sourceDataBase.getUrl()));
        String user = sourceDataBase.getUserName();
        String password = AESUtils.decrypt(sourceDataBase.getPassWord());

        Properties properties = debeziumProperties(dataMoveTask.getTaskId(), engineName, sourceSchemaName, sourceObjectName, hostName, port, user, password, sourceOffsetStorage, sourceOffsetKafka, sourceDebeziumSnapshot);
        DebeziumEngine<ChangeEvent<String, String>> initEngine = initDebeziumEngine(dataMoveTask, engineName, properties);
        debeziumEngineMap.put(dataMoveTask.getTaskId(), initEngine);

        DataMoveInfoEntity sourceDataMoveInfo = obtainSourceDataMoveInfo(1, dataMoveTask);
        DataMoveInfoEntity targetDataMoveInfo = obtainTargetDataMoveInfo(1, dataMoveTask);

        if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
            Map<String, Object> additionalProps = new HashMap<>();
            String destination = dataMoveTask.getTargetObjectName();
            String bootstrapServers = dataMoveTask.getTargetMQConfig().getBootstrapServers();
            mqAdapterApi.bindProducerDestination(dataMoveTask.getTaskId(), destination, bootstrapServers, additionalProps);
        }

        startDebeziumThread(dataMoveTask, engineName, initEngine, sourceDataMoveInfo.getInfoId(), targetDataMoveInfo.getInfoId());
    }

    private String genEngineName(Long taskId) {
        return "debeziumEngine-" + taskId;
    }

    private Properties debeziumProperties(Long taskId, String engineName, String sourceSchemaName, String sourceObjectName, String hostName, Integer port, String user, String password, Integer sourceOffsetStorage, String sourceOffsetKafka, Integer sourceDebeziumSnapshot) {
        Properties props = new Properties();
        props.setProperty("name", engineName);
        props.setProperty("connector.class", "io.debezium.connector.mysql.MySqlConnector");
        if (sourceOffsetStorage.equals(OFFSET_STORAGE_DATABASE)) {
            //如果按这个方式，data-stream数据必须是独立库，隔离源库和目标库，同一个库不同schema也不行
            //debezium在初始化快照的时候会锁所有库表
            props.setProperty("offset.storage.linkTaskId", taskId.toString());
            props.setProperty("offset.storage", "com.itman.datastream.engine.debezium.DatabaseOffsetBackingStore");
            props.setProperty("database.history", "com.itman.datastream.engine.debezium.DataStreamDatabaseHistory");
        } else if (sourceOffsetStorage.equals(OFFSET_STORAGE_FILE)) {
            String path = Paths.get("").toAbsolutePath() + "/debeziumFile/" + engineName + "/";
            props.setProperty("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore");
            props.setProperty("offset.storage.file.filename", path + "offset.dat");
            props.setProperty("database.history", "io.debezium.relational.history.FileDatabaseHistory");
            props.setProperty("database.history.file.filename", path + "dbhistory.dat");
        } else if (sourceOffsetStorage.equals(OFFSET_STORAGE_KAKFA)) {
            props.setProperty("offset.storage", "org.apache.kafka.connect.storage.KafkaOffsetBackingStore");
            props.setProperty("offset.storage.topic", "debezium-offset-topic-" + taskId);
            props.setProperty("offset.storage.partitions", "1");
            props.setProperty("offset.storage.replication.factor", "1");
            props.setProperty("bootstrap.servers", sourceOffsetKafka);

            props.setProperty("database.history", "io.debezium.relational.history.KafkaDatabaseHistory");
            props.setProperty("database.history.kafka.bootstrap.servers", sourceOffsetKafka);
            props.setProperty("database.history.kafka.topic", "debezium-db-history-topic-" + taskId);
            props.setProperty("database.history.kafka.replication.factor", "1");
            props.setProperty("database.history.kafka.partitions", "1");
            props.setProperty("database.history.kafka.producer.acks", "1");
        }

        props.setProperty("offset.flush.interval.ms", "10000");
        props.setProperty("database.hostname", hostName);
        props.setProperty("database.port", port.toString());
        props.setProperty("database.user", user);
        props.setProperty("database.password", password);
        props.setProperty("database.server.id", taskId.toString());
        props.setProperty("database.server.name", engineName);
        props.setProperty("database.include.list", sourceSchemaName);
        if (!sourceObjectName.equals(sourceSchemaName)) {
            String tableIncludeList = Arrays.stream(sourceObjectName.split(",")) // 将分割后的数组转为 Stream
                    .map(part -> sourceSchemaName + "." + part) // 将每个元素映射为 "ddd.aaa" 的格式
                    .collect(Collectors.joining(",")); // 用逗号将流中的所有元素合并成一个字符串
            props.setProperty("table.include.list", tableIncludeList);
        }
        props.setProperty("tombstones.on.delete", "false");
        props.setProperty("include.query.values", "false"); // 不捕获查询事件，只关注 DML 操作
        // DECIMAL 类型使用字符串格式，避免 Base64 编码
        props.setProperty("decimal.handling.mode", "string");
        // 时间类型处理模式
        props.setProperty("time.precision.mode", "connect");
        // 添加更多配置用于排查 Debezium 捕获问题:fail事件处理失败时停止
        props.setProperty("event.processing.failure.handling.mode", "fail");
//        props.setProperty("max.batch.size", "1000"); // 批量处理大小
//        props.setProperty("max.queue.size", "10000"); // 最大队列大小
//        props.setProperty("poll.interval.ms", "1000"); // 轮询间隔
//        props.setProperty("connect.timeout.ms", "30000"); // 连接超时

        //todo 增加一个参数告知是否需要快照
        if (sourceDebeziumSnapshot != null && sourceDebeziumSnapshot.equals(1)) {
            //initial执行快照会加锁数据库,然后读取快照数据
            props.setProperty("snapshot.mode", "initial");
        } else {
            //schema_only执行快照会加锁数据库,然后获取表结构，释放锁很快，然后在重最位点同步数据
            props.setProperty("snapshot.mode", "schema_only"); // 不执行快照，直接从binlog开始

//            String[] sourceOffsetStartPosS = sourceOffsetStartPos.split(":");
//            String binlogFilename = sourceOffsetStartPosS[0];
//            String binlogPosition = sourceOffsetStartPosS[1];
//            props.setProperty("binlog.filename", binlogFilename); // 指定binlog文件名
//            props.setProperty("binlog.position", binlogPosition); // 指定binlog位置
        }

        log.info("---------debeziumProperties:linkTaskId={},props={}", taskId, props);

        return props;
    }

    //todo 后续研究一下同一事务怎么控制一起执行
    private DebeziumEngine<ChangeEvent<String, String>> initDebeziumEngine(DataMoveTaskEntity dataMoveTask, String engineName, Properties props) {
        return DebeziumEngine.create(Json.class).using(props).notifying(record -> {
            try {
                debeziumSourceRecordCountMap.computeIfAbsent(dataMoveTask.getTaskId(), k -> new AtomicLong(0)).incrementAndGet();
                receiveChangeEvent(dataMoveTask, engineName, record.value());
            } catch (DataStreamException e) {
                log.error("Exception in Debezium engine processing for {}: DataStreamException=", engineName, e);
                debeziumErrorResultMap.put(dataMoveTask.getTaskId(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                log.error("Exception in Debezium engine processing for {}: Exception=", engineName, e);
                debeziumErrorResultMap.put(dataMoveTask.getTaskId(), e);
                throw new RuntimeException(e);
            }
        }).build();
    }

    protected void startDebeziumThread(DataMoveTaskEntity dataMoveTask, String engineName, DebeziumEngine<ChangeEvent<String, String>> initEngine, Long sourceInfoId, Long targetInfoId) throws DataStreamException {
        // 创建自定义ThreadFactory
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                // 在这里创建线程并设置名称
                Thread thread = new Thread(r);
                thread.setName(engineName); // 设置线程名称
                return thread;
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor(threadFactory);
        debeziumExecutorMap.put(dataMoveTask.getTaskId(), executor);

        CompletableFuture<Void> future = CompletableFuture.runAsync(initEngine, executor);

        Integer lastSourceDataCount = 0;
        Integer lastTargetDataCount = 0;
        while (true) {
            try {
                log.info("startDebeziumThread-----------monitor--sleep 3000");

                Thread.sleep(3000);
            } catch (InterruptedException e) {
                log.error("startDebeziumThread-----------monitor--InterruptedException", e);
            }

            AtomicLong currentSourceDataCount = debeziumSourceRecordCountMap.get(dataMoveTask.getTaskId());
            if (currentSourceDataCount != null) {
                Integer dataCount = currentSourceDataCount.intValue() - lastSourceDataCount;
                if (dataCount > 0) {
                    refreshDataMoveInfoPageRowEnd(sourceInfoId, null, dataCount, dataCount, 0L, 0L, 0L);
                    lastSourceDataCount = currentSourceDataCount.intValue();
                }
            }

            AtomicLong currentTargetDataCount = debeziumTargetRecordCountMap.get(dataMoveTask.getTaskId());
            if (currentTargetDataCount != null) {
                Integer dataCount = currentTargetDataCount.intValue() - lastTargetDataCount;
                if (dataCount > 0) {
                    refreshDataMoveInfoPageRowEnd(targetInfoId, null, dataCount, dataCount, 0L, 0L, 0L);
                    lastTargetDataCount = currentTargetDataCount.intValue();
                }
            }

            if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
                super.flushFileResource(dataMoveTask);
            }

            if (debeziumErrorResultMap.containsKey(dataMoveTask.getTaskId())) {
                log.info("startDebeziumThread-----------debeziumErrorResultMap is error ---break");

                break;
            }

            if (!debeziumExecutorMap.containsKey(dataMoveTask.getTaskId())) {
                log.info("startDebeziumThread-----------debeziumExecutorMap is null ---break");
//                debeziumErrorResultMap.put(engineName, new DataStreamException("xxxx", "该任务增量同步线程回退"));

                break;
            }
        }

        debeziumSourceRecordCountMap.remove(dataMoveTask.getTaskId());
        debeziumTargetRecordCountMap.remove(dataMoveTask.getTaskId());

        if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
            fileApi.releaseFileResource(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataMoveTask.getTargetFileFormat());
        }

//        log.info("startDebeziumThread--CompletableFuture-join-begin:engineName={}", engineName);
//        try {
//            CompletableFuture.allOf(future).join(); // 无限期等待所有任务完成
//        } catch (Exception e) {
//            log.error("Exception in future processing for {}: ", engineName, e);
//        } finally {
//            stopSingleEngine(engineName);
//        }

        if (debeziumErrorResultMap.containsKey(dataMoveTask.getTaskId())) {
            String errorCode = null;
            String errorMessage = null;
            Exception debeiziumException = debeziumErrorResultMap.get(dataMoveTask.getTaskId());
            if (debeiziumException instanceof DataStreamException) {
                errorCode = ((DataStreamException) debeiziumException).getErrCode();
                errorMessage = ((DataStreamException) debeiziumException).getErrMsg();
            } else if (debeiziumException instanceof Exception) {
                errorCode = UNKNOWN_ERROR.getCode();
                errorMessage = debeiziumException.getMessage();
            }
            debeziumErrorResultMap.remove(dataMoveTask.getTaskId());
            //修改运行记录状态
            metaService.updateDataMoveInfoErrorInfo(sourceInfoId, DATA_STREAM_TASK_STATE_RUNNING, (Objects.isNull(errorCode) ? DATA_STREAM_TASK_STATE_FINISH : DATA_STREAM_TASK_STATE_ERROR), errorCode, errorMessage);
            metaService.updateDataMoveInfoErrorInfo(targetInfoId, DATA_STREAM_TASK_STATE_RUNNING, (Objects.isNull(errorCode) ? DATA_STREAM_TASK_STATE_FINISH : DATA_STREAM_TASK_STATE_ERROR), errorCode, errorMessage);

            throw new DataStreamException(OPER_DEBEZIUM_MOVE_ERROR);
        }
    }

    public void receiveChangeEvent(DataMoveTaskEntity dataMoveTask, String engineName, String changeValue) throws DataStreamException {
        DataBaseEntity sourceDataBase = dataMoveTask.getSourceDataBase();
        String sourceSchemaName = sourceDataBase.getSchemaName();
        Integer sourceDebeziumObject = dataMoveTask.getSourceDebeziumObject();
        Long targetDataSourceId = dataMoveTask.getTargetObjectId();
        DataBaseEntity targetDataBase = dataMoveTask.getTargetDataBase();
        List<ColumnTypeDefineEntity> targetTableColumnTypeDefineList = dataMoveTask.getTargetTableColumnTypeDefineList();

        if (Objects.nonNull(changeValue)) {
            if (log.isDebugEnabled()) {
                log.debug("~~~~~~~~~~~~~~~~~receiveChangeEvent--changeValue={}", changeValue);
            }
            Map<String, Object> mapObject = parseObjectMap(changeValue);

            Map<String, Object> payload = getMapValueMap("payload", mapObject);
            if (CollectionUtils.isEmpty(payload)) {
                return;
            }

            //如果"snapshot" : "true",在是read类型
            String handleType = getHandleType(payload);
            if (log.isDebugEnabled()) {
                log.debug("&&&&&&&&&&&&&&&&&&&&&&&receiveChangeEvent-dmlMessageConsumer:engineName={}, handleType={}", engineName, handleType);
            }
            if ((sourceDebeziumObject.equals(SOURCE_DEBEZIUM_OBJECT_DATA) || sourceDebeziumObject.equals(SOURCE_DEBEZIUM_OBJECT_SCHEMA_AND_DATA)) && !HANDLE_TYPE_NONE.equals(handleType)) {
                Map<String, Object> schemaMap = getMapValueMap("schema", mapObject);
                if (CollectionUtils.isEmpty(schemaMap)) {
                    throw new DataStreamException(OPER_GET_SCHEMA_NULL_FAIL_ERROR);
                }

                ChangeDataEntity changeData = getChangeData(payload);

                CDCMessageEntity CDCMessage = new CDCMessageEntity();
                CDCMessage.setBeforeData(changeData.getBefore());
                CDCMessage.setAfterData(changeData.getAfter());
                CDCMessage.setDbType("MySQL");
                CDCMessage.setDatabase(String.valueOf(changeData.getSource().get("db")));
                CDCMessage.setTable(String.valueOf(changeData.getSource().get("table")));
                CDCMessage.setHandleType(handleType);

                CDCMessage.setDebeziumFiledMap(changeAfterFields(schemaMap));

                if (log.isDebugEnabled()) {
                    log.debug("******************receiveChangeEvent-dmlMessageConsumer:CDCMessage={}", CDCMessage);
                }

//                if (changeData.getSource().containsKey("snapshot") && changeData.getSource().get("snapshot").equals("true")) {
//                    CDCMessage.setHandleType(HANDLE_TYPE_INSERT);
//                }

                DebeziumTableEntity targetDebeziumTable = null;
                if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
                    String targetTableName = CDCMessage.getTable().toLowerCase();

                    targetDebeziumTable = debeziumTableMap.computeIfAbsent(dataMoveTask.getTaskId(), k -> new ConcurrentHashMap<>()).computeIfAbsent(targetTableName, k -> {
                        DebeziumTableEntity debeziumTable = new DebeziumTableEntity();
                        try {
                            List<TableColumnEntity> targetColumns = moveTargetService.getTableColumns(targetDataSourceId, targetDataBase, targetTableName);
                            super.copyTableColumnsTypeDefine(targetColumns, targetTableColumnTypeDefineList);
                            debeziumTable.setTableColumnsList(targetColumns);
                            debeziumTable.setSqlInsertColumns(makeSqlInsertColumns(targetTableName, "", targetColumns));
                            debeziumTable.setKeyColumnsList(targetColumns.stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
                        } catch (DataStreamException e) {
                            throw new RuntimeException(e);
                        }
                        return debeziumTable;
                    });

                    if (targetDebeziumTable == null) {
                        throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TARGET_TABLE_NULL_ERROR);
                    }
                }

                dmlMessageConsumer(dataMoveTask, engineName, CDCMessage, targetDebeziumTable);
            } else if ((sourceDebeziumObject.equals(SOURCE_DEBEZIUM_OBJECT_SCHEMA) || sourceDebeziumObject.equals(SOURCE_DEBEZIUM_OBJECT_SCHEMA_AND_DATA))) {
                String ddlSql = getDdlSql(payload);
                if (ddlSql != null) {
                    ChangeDataEntity changeDataEntity = getChangeData(payload);
                    if (changeDataEntity != null && changeDataEntity.getSource().get("table") != null) {
                        ddlSql = ddlSql.replace("`" + sourceSchemaName + "`.", "");
                        log.info("----------------receiveChangeEvent-ddlHandler:ddlSql={}", ddlSql);
                        if (log.isDebugEnabled()) {
                            log.debug("----------------receiveChangeEvent-ddlHandler:ddlSql={}", ddlSql);
                        }
                        ddlMessageHandler(dataMoveTask, String.valueOf(changeDataEntity.getSource().get("table")), ddlSql);
                    }
                }
            }
        }
    }

    private void stopSingleEngine(DataMoveTaskEntity dataMoveTask) {
        if (debeziumEngineMap.containsKey(dataMoveTask.getTaskId())) {
            try {
                log.info("stopSingleEngine debeziumEngineMap begin linkTaskId={}", dataMoveTask.getTaskId());
                debeziumEngineMap.get(dataMoveTask.getTaskId()).close();
                debeziumEngineMap.remove(dataMoveTask.getTaskId());
                log.info("stopSingleEngine debeziumEngineMap success linkTaskId={}", dataMoveTask.getTaskId());
            } catch (Exception e) {
                log.error("stopSingleEngine debeziumEngineMap linkTaskId={}, Exception={}", dataMoveTask.getTaskId(), e);
            }
        }

        if (debeziumTableMap.containsKey(dataMoveTask.getTaskId())) {
            debeziumTableMap.remove(dataMoveTask.getTaskId());
        }

        if (debeziumExecutorMap.containsKey(dataMoveTask.getTaskId())) {
            try {
                log.info("stopSingleEngine debeziumExecutorMap begin linkTaskId={}", dataMoveTask.getTaskId());
                debeziumExecutorMap.get(dataMoveTask.getTaskId()).shutdownNow();

                // 第二步：等待一段时间让现有任务（包括引擎关闭流程）完成
                if (!debeziumExecutorMap.get(dataMoveTask.getTaskId()).awaitTermination(60, TimeUnit.SECONDS)) {
                    log.info("线程池未在指定时间内关闭，尝试强制关闭。linkTaskId={}", dataMoveTask.getTaskId());
                    debeziumExecutorMap.get(dataMoveTask.getTaskId()).shutdownNow(); // 尝试强制终止
                } else {
                    log.info("DebeziumEngine 已完全停止，offset 已保存。linkTaskId={}", dataMoveTask.getTaskId());
                }
                log.info("stopSingleEngine debeziumExecutorMap success linkTaskId={}", dataMoveTask.getTaskId());
            } catch (Exception e) {
                log.error("stopSingleEngine debeziumExecutorMap linkTaskId={}, Exception={}", dataMoveTask.getTaskId(), e);
            } finally {
                debeziumExecutorMap.remove(dataMoveTask.getTaskId());
            }
        }

        if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            try {
                IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
                mqAdapterApi.unbindProducerDestination(dataMoveTask.getTaskId());
            } catch (DataStreamException e) {
                log.error("stopSingleEngine unbindProducerDestination linkTaskId={}, Exception={}", dataMoveTask.getTaskId(), e);
            }
        }
    }


    public static Map<String, Object> parseObjectMap(String value) {
        return JSON.parseObject(value, Map.class);
    }

    private static Map<String, Object> getMapValueMap(String keyName, Map<String, Object> mapObject) {
        Object objectValue = mapObject.get(keyName);
        if (objectValue == null) {
            return Collections.emptyMap();
        }

        if (objectValue instanceof Map) {
            @SuppressWarnings("unchecked") Map<String, Object> result = (Map<String, Object>) objectValue;
            return result;
        }
        return Collections.emptyMap();
    }

    private static List<Map<String, Object>> getMapValueList(String keyName, Map<String, Object> mapObject) {
        Object objectValue = mapObject.get(keyName);
        if (objectValue == null) {
            return Collections.emptyList();
        }

        if (objectValue instanceof List) {
            @SuppressWarnings("unchecked") List<Map<String, Object>> result = (List<Map<String, Object>>) objectValue;
            return result;
        }
        return Collections.emptyList();
    }

    private static String getMapValueString(String key, Map<String, Object> mapObject) {
        return mapObject.containsKey(key) ? mapObject.get(key).toString() : null;
    }

    private static Boolean getMapValueBoolean(String key, Map<String, Object> mapObject) {
        return mapObject.containsKey(key) ? (Boolean) mapObject.get(key) : null;
    }

    public static String getHandleType(Map<String, Object> payload) {
        String op = getMapValueString("op", payload);
        if (Objects.nonNull(op)) {
            switch (op) {
                case "r":
                    return HANDLE_TYPE_READ;
                case "c":
                    return HANDLE_TYPE_INSERT;
                case "u":
                    return HANDLE_TYPE_UPDATE;
                case "d":
                    return HANDLE_TYPE_DELETE;
                default:
                    return HANDLE_TYPE_NONE;
            }
        } else {
            return HANDLE_TYPE_NONE;
        }
    }

    public static String getDdlSql(Map<String, Object> payload) {
        return getMapValueString("ddl", payload);
    }

    public static ChangeDataEntity getChangeData(Map<String, Object> payload) {
        return ChangeDataEntity.builder().after((Map<String, Object>) payload.get("after")).source((Map<String, Object>) payload.get("source")).before((Map<String, Object>) payload.get("before")).build();
    }


    public static Map<String, DebeziumFiledEntity> changeAfterFields(Map<String, Object> mapObject) {
        List<Map<String, Object>> fieldsMapList = getMapValueList("fields", mapObject);
        if (fieldsMapList == null) return Collections.emptyMap();

        List<Map<String, Object>> afterFieldsMapList = new ArrayList<>();
        for (Map<String, Object> iterator : fieldsMapList) {
            String afterMap = getMapValueString("field", iterator);
            if (afterMap != null && afterMap.equals("after")) {
                afterFieldsMapList = getMapValueList("fields", iterator);
                break;
            }
        }

        Map<String, DebeziumFiledEntity> debeziumFiledMap = new HashMap<>();
        for (Map<String, Object> iterator : afterFieldsMapList) {
            DebeziumFiledEntity debeziumFiled = new DebeziumFiledEntity();
            debeziumFiled.setField(getMapValueString("field", iterator).toLowerCase());
            debeziumFiled.setOptional(getMapValueBoolean("optional", iterator));
            debeziumFiled.setType(getMapValueString("type", iterator));
            debeziumFiled.setName(getMapValueString("name", iterator));
            debeziumFiledMap.put(debeziumFiled.getField(), debeziumFiled);
        }
        return debeziumFiledMap;
    }


    public void ddlMessageHandler(DataMoveTaskEntity dataMoveTask, String tableName, String ddlSql) throws DataStreamException {
        //通过多个异步线程处理这里只是分发操作
        //只能针对不同主键数据进行并行处理
        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            Long targetDataSourceId = dataMoveTask.getTargetObjectId();
            moveTargetService.executeMetaDbSql(targetDataSourceId, ddlSql);
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            //实时同步写文件没有意义，只能同步不带字段定义的文件，如果同步多个表字段格式不一样
            //写入文件对象
            super.writeMapToTargetNoFieldFile(buildDataMapFromDdlSql(ddlSql), dataMoveTask);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            MQMessageEntity MQMessage= new MQMessageEntity();
            MQMessage.setDataType(MQ_MESSAGE_DATA_TYPE_CDC_DDL);
            MQMessage.setDataValue(ddlSql);
            writeObjectToTargetMQ(MQMessage, dataMoveTask);
        }
        //记录次数
        debeziumTargetRecordCountMap.computeIfAbsent(dataMoveTask.getTaskId(), k -> new AtomicLong(0)).incrementAndGet();
    }

    private List<Map> buildDataMapFromDdlSql(String ddlSql) {
        List<Map> dataListTarget = new ArrayList<>();
        Map<String, String> dataMapTarget = new LinkedHashMap();
        dataMapTarget.put("ddlSql", ddlSql);
        dataListTarget.add(dataMapTarget);
        return dataListTarget;
    }

    public void operateCdcTaskStop(Long taskId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
        if (!CollectionUtils.isEmpty(dataMoveTaskList)) {
            log.info("operateCdcTaskStop-----linkTaskId=" + taskId);
            stopSingleEngine(dataMoveTaskList.get(0));
        }
    }

    public void dmlMessageConsumer(DataMoveTaskEntity dataMoveTask, String engineName, CDCMessageEntity messageEntity, DebeziumTableEntity targetDebeziumTable) throws DataStreamException {
        dmlMessageConsumer(dataMoveTask, messageEntity, targetDebeziumTable);
        //记录次数
        debeziumTargetRecordCountMap.computeIfAbsent(dataMoveTask.getTaskId(), k -> new AtomicLong(0)).incrementAndGet();
    }

    private String changeDebeziumTimeFieldValue(String fieldName, Object fieldValue, Map<String, DebeziumFiledEntity> debeziumFiledMap) {
        if (fieldValue == null) {
            return "null";
        }
        return fieldValue.toString();
    }

    private void formatCDCMessage(CDCMessageEntity messageEntity) throws DataStreamException {
        if (messageEntity.getHandleType().equals(HANDLE_TYPE_INSERT) || messageEntity.getHandleType().equals(HANDLE_TYPE_UPDATE) || messageEntity.getHandleType().equals(HANDLE_TYPE_READ)) {
            Map<String, String> dataMap = new HashMap<>();
            messageEntity.getAfterData().forEach((x, y) -> {
                String fieldName = x.toLowerCase();
                String fieldValue = changeDebeziumTimeFieldValue(fieldName, y, messageEntity.getDebeziumFiledMap());
                dataMap.put(fieldName, fieldValue);
            });
            messageEntity.setDataRecordAfter(dataMap);
        }

        if (messageEntity.getHandleType().equals(HANDLE_TYPE_DELETE) || messageEntity.getHandleType().equals(HANDLE_TYPE_UPDATE)) {
            Map<String, String> dataMap = new HashMap<>();
            messageEntity.getBeforeData().forEach((x, y) -> {
                String fieldName = x.toLowerCase();
                String fieldValue = changeDebeziumTimeFieldValue(fieldName, y, messageEntity.getDebeziumFiledMap());
                dataMap.put(fieldName, fieldValue);
            });
            messageEntity.setDataRecordBefore(dataMap);
        }
    }

    /**
     * 时间戳转换为指定格式的字符串
     *
     * @param timestamp 时间戳（自动判断秒级或毫秒级）
     * @param pattern   格式模式，如 "yyyy-MM-dd HH:mm:ss"
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        // 自动判断时间戳类型：大于10位数认为是毫秒级，否则是秒级
        Instant instant = timestamp > 9999999999L ? Instant.ofEpochMilli(timestamp)  // 毫秒级
                : Instant.ofEpochSecond(timestamp); // 秒级

        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    private List<Map>  buildDataMapFromMessage(CDCMessageEntity messageEntity) {
        List<Map> dataListTarget = new ArrayList<>();
        Map<String, String> dataMapTarget = new LinkedHashMap();
        dataMapTarget.put("handleType", messageEntity.getHandleType());
        if (messageEntity.getHandleType().equals(HANDLE_TYPE_INSERT) || messageEntity.getHandleType().equals(HANDLE_TYPE_UPDATE) || messageEntity.getHandleType().equals(HANDLE_TYPE_READ)) {
            dataMapTarget.putAll(messageEntity.getDataRecordAfter());
        } else if (messageEntity.getHandleType().equals(HANDLE_TYPE_DELETE)) {
            dataMapTarget.putAll(messageEntity.getDataRecordBefore());
        }

        dataListTarget.add(dataMapTarget);
        return dataListTarget;
    }

    private void dmlMessageConsumer(DataMoveTaskEntity dataMoveTask, CDCMessageEntity messageEntity, DebeziumTableEntity targetDebeziumTable) throws DataStreamException {
        formatCDCMessage(messageEntity);

        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            String dmlSql = generateDmlSQL(dataMoveTask, messageEntity, targetDebeziumTable);
            Long targetDataSourceId = dataMoveTask.getTargetObjectId();
            if (moveTargetService.executeMetaDbSql(targetDataSourceId, dmlSql) != 1) {
                throw new DataStreamException("executeMetaDbSql", " executeMetaDbSql fail !");
            }
        } else if (isFileDataSource(dataMoveTask.getTargetObjectType())) {
            //实时同步写文件没有意义，只能同步不带字段定义的文件，如果同步多个表字段格式不一样
            //写入文件对象
            writeMapToTargetNoFieldFile(buildDataMapFromMessage(messageEntity), dataMoveTask);
        } else if (isMQDataSource(dataMoveTask.getTargetObjectType())) {
            //写入消息队列
            String dmlSql = JSON.toJSONString(messageEntity);
            MQMessageEntity MQMessage= new MQMessageEntity();
            MQMessage.setDataType(MQ_MESSAGE_DATA_TYPE_CDC_DML);
            MQMessage.setDataValue(dmlSql);
            writeObjectToTargetMQ(MQMessage, dataMoveTask);
        }
    }
}
