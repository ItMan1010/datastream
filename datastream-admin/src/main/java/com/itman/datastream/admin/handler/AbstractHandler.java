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
import com.itman.datastream.admin.service.ITableCount;
import com.itman.datastream.common.api.IFileApi;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.engine.holder.DataStreamHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.constant.DataStreamConstant.MOVE_INFO_FLAG_SOURCE;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.OPER_GET_DATA_BY_FIELD_NAME_ERROR;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.OPER_GET_TABLE_COLUMN_TYPE_DEFINE_ERROR;
import static com.itman.datastream.common.utils.CommUtils.*;


@Slf4j
@Component
@AllArgsConstructor
public abstract class AbstractHandler {
    private final DataSourceFactory dataSourceFactory;
    public final DataStreamConfig dataStreamConfig;
    private final IMetaService metaService;
    private final DataStreamHolder dataStreamHolder;
    protected final IMoveTargetService moveTargetService;


    public IDatabaseAdapter matchDataBase(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchDataBase(dataSourceType);
    }

    public IFileApi matchFileFormat(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchFileFormat(dataSourceType);
    }

    public IMQAdapterApi matchMQ(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchMQ(dataSourceType);
    }

    public String makeSqlInsertColumns(String tableName, String ignore, List<TableColumnEntity> tableColumnList) {
        String tableColumns = tableColumnList.stream().map(TableColumnEntity::getColumnName).collect(Collectors.joining(","));
        StringBuffer insertSql = new StringBuffer();
        return insertSql.append("insert " + ignore + " into ").append(tableName.toLowerCase()).append(" (").append(tableColumns).append(") values ").toString();
    }

    public String makeSqlBatchInsert(String insertSqlColumns, Long taskId, Integer dataSourceType, List<TableColumnEntity> tableColumns, List<Map> dataRecordList) throws DataStreamException {
        return matchDataBase(dataSourceType).makeSqlBatchInsert(insertSqlColumns, taskId, tableColumns, dataRecordList, dataStreamConfig.getDataStreamParallelStreamSize());
    }

    public String makeSqlSelectCountByKey(final Integer dataSourceType, final Map dataRecord, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final String tableName) {
        try {
            return matchDataBase(dataSourceType).makeSqlSelectCountByKey(dataRecord, keyColumns, tableColumns, tableName);
        } catch (DataStreamException e) {
            throw new RuntimeException(e);
        }
    }

    public String makeSqlSourceDataKeyDelete(final Map dataRecord, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
        return matchDataBase(dataSourceType).makeSqlDeleteRow(dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceKeyColumns(), dataMoveTask.getSourceTableColumns(), dataRecord);
    }

    public String makeSqlDeleteRow(final Map dataRecord, final Integer dataSourceType, final String tableName, final List<String> tableKeyColumns, final List<TableColumnEntity> tableColumns) throws DataStreamException {
        return matchDataBase(dataSourceType).makeSqlDeleteRow(tableName, tableKeyColumns, tableColumns, dataRecord);
    }

    public String makeSqlUpdateRow(final Map dataRecordBefore, final Map dataRecordAfter, final Integer dataSourceType, final String tableName, final List<String> tableKeyColumns, final List<TableColumnEntity> tableColumns) throws DataStreamException {
        return matchDataBase(dataSourceType).makeSqlUpdateRow(tableName, tableKeyColumns, tableColumns, dataRecordBefore, dataRecordAfter);
    }

    List<Map> filterDuplicateData(final Long dataSourceId, final Integer dataSourceType, final List<Map> dataList, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final String tableName, final ITableCount ITableCountService) throws DataStreamException {
        if (CollectionUtils.isEmpty(keyColumns)) {
            return new ArrayList<>();
        }

        return (dataList.size() > dataStreamConfig.getDataStreamParallelStreamSize()) ? dataList.parallelStream().filter(dataRecord -> {
            String keySelectSql = makeSqlSelectCountByKey(dataSourceType, dataRecord, keyColumns, tableColumns, tableName);
            try {
                return keySelectSql != null && ITableCountService.getTableRecordCount(dataSourceId, keySelectSql) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()) : dataList.stream().filter(dataRecord -> {
            String keySelectSql = makeSqlSelectCountByKey(dataSourceType, dataRecord, keyColumns, tableColumns, tableName);
            try {
                return keySelectSql != null && ITableCountService.getTableRecordCount(dataSourceId, keySelectSql) == 0L;
            } catch (DataStreamException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    public String formatColumnValueForSql(Integer columnTypeClassify, String columnValue) throws DataStreamException {
        return columnTypeClassify.equals(COLUMN_TYPE_CLASSIFY_NUMERIC) ? columnValue : ("'" + columnValue + "'");
    }

    public DataMoveInfoEntity getRunDataMoveInfo(Long taskId, Integer virtualId, String dataNode, Integer infoFlag) throws DataStreamException {
        List<DataMoveInfoEntity> dataMoveInfoList = metaService.queryDataMoveInfoByTaskIdAndVirtualId(taskId, virtualId, infoFlag, dataNode);
        if (!CollectionUtils.isEmpty(dataMoveInfoList)) {
            return dataMoveInfoList.get(0);
        }
        return null;
    }

    private DataMoveInfoEntity createDefaultDataMoveInfo(Integer infoFlag, Long taskId, String objectName, String dataNodeName, Integer virtualId) throws DataStreamException {
        DataMoveInfoEntity dataMoveInfo = new DataMoveInfoEntity();
        dataMoveInfo.setInfoId(metaService.querySequence(SEQ_MOVE_INFO_ID));
        dataMoveInfo.setTableName(objectName);
        dataMoveInfo.setDataNode(dataNodeName);
        dataMoveInfo.setDataCount(0L);
        dataMoveInfo.setDataActualCount(0L);
        dataMoveInfo.setLoopCount(0);
        dataMoveInfo.setMaxCost(0L);
        dataMoveInfo.setMinCost(0L);
        dataMoveInfo.setSumCost(0L);
        dataMoveInfo.setLatelyCost(0L);
        dataMoveInfo.setVirtualId(virtualId);
        dataMoveInfo.setTaskId(taskId);
        dataMoveInfo.setThreadName("null");
        dataMoveInfo.setState(DATA_STREAM_TASK_STATE_RUNNING);
        dataMoveInfo.setPageRowStart("null");
        dataMoveInfo.setPageRowEnd("null");
        dataMoveInfo.setPageLoopCount(0);
        dataMoveInfo.setInfoFlag(infoFlag);
        return dataMoveInfo;
    }

    DataMoveInfoEntity obtainSourceDataMoveInfo(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        DataMoveInfoEntity dataMoveInfo = getRunDataMoveInfo(dataMoveTask.getTaskId(), virtualId, dataMoveTask.getSourceDataNode(), MOVE_INFO_FLAG_SOURCE);
        if (!Objects.isNull(dataMoveInfo) && dataMoveInfo.getState().equals(DATA_STREAM_TASK_STATE_FINISH)) {
            log.info("linkTaskId={},sourceObjectName={},nodeName={} end2", dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataNode());
        } else if (Objects.isNull(dataMoveInfo)) {
            dataMoveInfo = createDefaultDataMoveInfo(MOVE_INFO_FLAG_SOURCE, dataMoveTask.getTaskId(), dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceDataNode(), virtualId);
            List<DataMoveInfoEntity> dataMoveInfoList = new ArrayList<>();
            dataMoveInfoList.add(dataMoveInfo);
            if (!metaService.insertDataMoveInfo(dataMoveInfoList).equals(1)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_INSERT_MOVE_INFO_DATA_ERROR);
            }
        }
        //缓存处理
        dataStreamHolder.setDataMoveInfo(dataMoveInfo.getInfoId(), dataMoveInfo);
        dataStreamHolder.addSourceMoveInfoId(dataMoveTask.getTaskId(), dataMoveInfo.getInfoId());
        return dataMoveInfo;
    }

    DataMoveInfoEntity obtainTargetDataMoveInfo(final Integer virtualId, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //根据配置决定是否记录目标运行记录
        if (!dataMoveTask.getTargetPropertiesMoveInfoFlag().equals(1)) {
            return null;
        }

        DataMoveInfoEntity dataMoveInfo = getRunDataMoveInfo(dataMoveTask.getTaskId(), virtualId, dataMoveTask.getSourceDataNode(), MOVE_INFO_FLAG_TARGET);
        if (Objects.isNull(dataMoveInfo)) {
            dataMoveInfo = createDefaultDataMoveInfo(MOVE_INFO_FLAG_TARGET, dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataMoveTask.getSourceDataNode(), virtualId);
            List<DataMoveInfoEntity> dataMoveInfoList = new ArrayList<>();
            dataMoveInfoList.add(dataMoveInfo);
            if (!metaService.insertDataMoveInfo(dataMoveInfoList).equals(1)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_INSERT_MOVE_INFO_DATA_ERROR);
            }
        }

        //缓存处理
        dataStreamHolder.setDataMoveInfo(dataMoveInfo.getInfoId(), dataMoveInfo);
        dataStreamHolder.addTargetMoveInfoId(dataMoveTask.getTaskId(), dataMoveInfo.getInfoId());
        return dataMoveInfo;
    }

    void refreshDataMoveInfoPageRowEnd(Long infoId, String pageRowEnd, Integer dataCount, Integer dataActualCount, Long maxCost, Long minCost, Long currentCost) throws DataStreamException {
        //todo 根据配置异步写入和实时写库
        if (!dataStreamConfig.getDataStreamMoveInfoAsyncEnable()) {
            if (!metaService.updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount, maxCost, minCost, currentCost).equals(1)) {
                throw new DataStreamException(DataStreamErrorCode.OPER_UPDATE_TASK_INFO_FAIL_ERROR);
            }
        }
        //写入缓存
        dataStreamHolder.updateDataMoveInfoPageRowEnd(infoId, pageRowEnd, dataCount, dataActualCount, maxCost, minCost, currentCost);
    }

    Integer writeDataToTargetFile(final Long infoId, String pageRowStart, final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<List<String>> dataObjectList = new ArrayList<>();
        Map<String, Long> dataSumFieldMap = new HashMap<>();
        for (Map<String, String> iterator : dataListTarget) {
            List<String> dataRowList = new ArrayList<>();
            if (CollectionUtils.isEmpty(dataMoveTask.getTargetFileFormat().getFileBody().getFileFieldList())) {
                for (Object value : iterator.values()) {
                    dataRowList.add(value.toString());
                }
            } else {
                for (FileFieldEntity iterator2 : dataMoveTask.getTargetFileFormat().getFileBody().getFileFieldList()) {
                    Object fieldvalue = iterator.get(iterator2.getFieldName().toLowerCase());
                    if (fieldvalue == null) {
                        fieldvalue = iterator.get(iterator2.getFieldName().toUpperCase());
                    }
                    if (fieldvalue == null) {
                        throw new DataStreamException(OPER_GET_DATA_BY_FIELD_NAME_ERROR.getCode(), "根据字段名称匹配数据失败,fieldName=" + iterator2.getFieldName());
                    }

                    //判断是否有累加字段
                    if (iterator2.getSumFieldName() != null) {
                        dataSumFieldMap.merge(iterator2.getSumFieldName(), Long.parseLong(fieldvalue.toString()), Long::sum);
                    }
                    dataRowList.add(fieldvalue.toString());
                }
            }
            dataObjectList.add(dataRowList);
        }

        IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
        fileApi.bodyDataWriteIntoFile(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataObjectList, dataSumFieldMap, dataMoveTask.getTargetFileFormat());
        return dataObjectList.size();
    }

    void flushFileResource(final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
        fileApi.flushResource(dataMoveTask.getTaskId(), dataMoveTask.getTargetFileFormat().getFileFormatId());
    }

    Integer writeMapToTargetNoFieldFile(final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<List<String>> dataObjectList = new ArrayList<>();
        for (Map<String, String> iterator : dataListTarget) {
            List<String> dataRowList = new ArrayList<>();
            iterator.forEach((key, value) -> {
                dataRowList.add("{" + key + ":" + value + "}");
            });

            dataObjectList.add(dataRowList);
        }

        IFileApi fileApi = matchFileFormat(dataMoveTask.getTargetObjectType());
        fileApi.bodyDataWriteIntoFile(dataMoveTask.getTaskId(), dataMoveTask.getTargetObjectName(), dataObjectList, null, dataMoveTask.getTargetFileFormat());
        return dataObjectList.size();
    }

    Integer writeDataToTargetMQ(final Long infoId, String pageRowStart, final List<Map> dataListTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
        List<String> dataObjectList = new ArrayList<>();
        MQConfigEntity taskTargetMQConfig = dataMoveTask.getTargetMQConfig();
        for (Map iterator : dataListTarget) {
            dataObjectList.add(convertMapToMessage(iterator, taskTargetMQConfig.getMessageFormat(), taskTargetMQConfig.getDelimiter()));
        }

        for (String iterator : dataObjectList) {
            mqAdapterApi.sendMQMessage(dataMoveTask.getTaskId(), iterator);
        }
        return dataObjectList.size();
    }

    void writeStringObjectToTargetMQ(final Long infoId, String pageRowStart, final String dataTarget, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
        mqAdapterApi.sendMQMessage(dataMoveTask.getTaskId(), dataTarget);
    }

    void writeObjectToTargetMQ(final MQMessageEntity MQMessage, final DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        IMQAdapterApi mqAdapterApi = matchMQ(dataMoveTask.getTargetObjectType());
        String MQMessageString = "dataType<" + MQMessage.getDataType() + ">" + MQMessage.getDataValue();
        mqAdapterApi.sendMQMessage(dataMoveTask.getTaskId(), MQMessageString);
    }

    void MQMessageParser(String MQMessageString, MQMessageEntity MQMessageObject) throws DataStreamException {
        log.debug("MQMessageString="+MQMessageString);
        if (MQMessageString == null || MQMessageObject == null) {
            throw new IllegalArgumentException("输入字符串或目标消息对象不能为空");
        }

        // 1. 查找关键分隔符的位置
        int prefixEndIndex = MQMessageString.indexOf("dataType<");
        if (prefixEndIndex == -1) {
            throw new IllegalArgumentException("无效的消息格式：未找到 'dataType<' 前缀");
        }
        // 将索引移动到 "dataType<" 之后
        prefixEndIndex += "dataType<".length();

        int typeEndIndex = MQMessageString.indexOf(">", prefixEndIndex);
        if (typeEndIndex == -1) {
            throw new IllegalArgumentException("无效的消息格式：未找到匹配的 '>'");
        }

        // 2. 提取 dataType 和 dataJson
        String dataType = MQMessageString.substring(prefixEndIndex, typeEndIndex);
        // '>' 的下一个位置就是 JSON 字符串的开始
        String dataValue = MQMessageString.substring(typeEndIndex + 1);

        // 3. 将解析出的值赋回给目标对象
        MQMessageObject.setDataType(Integer.parseInt(dataType));
        MQMessageObject.setDataValue(dataValue);
    }

    private String convertMapToMessage(Map iterator, Integer messageFormat, String delimiter) {
        StringBuilder lineContent = new StringBuilder();
        if (messageFormat.equals(MESSAGE_FORMAT_JSON)) {
            //把当前map数据行转json
            try {
                lineContent.append(mapToJsonByJacksonStreaming(iterator));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (messageFormat.equals(MESSAGE_FORMAT_DELIMITER)) {
            List<String> dataRowList = new ArrayList<>();
            iterator.forEach((key, value) -> {
                dataRowList.add("{" + key.toString() + ":" + value.toString() + "}");
            });

            lineContent.append(String.join(delimiter, dataRowList));
        }
        return lineContent.toString();
    }

    /**
     * 将消息字符串转换为 Map 对象（convertMapToMessage 的反操作）
     *
     * @param message       消息字符串（JSON 或分隔符格式）
     * @param messageFormat 消息格式：MESSAGE_FORMAT_JSON 或 MESSAGE_FORMAT_DELIMITER
     * @param delimiter     分隔符（仅当 messageFormat 为 MESSAGE_FORMAT_DELIMITER 时使用）
     * @return Map<String, String>
     */
    protected Map<String, String> convertMessageToMap(String message, Integer messageFormat, String delimiter) {
        if (!StringUtils.hasText(message)) {
            return new HashMap<>();
        }

        Map<String, String> resultMap = new HashMap<>();

        if (messageFormat.equals(MESSAGE_FORMAT_JSON)) {
            // JSON 格式解析
            try {
                // 使用 jackson 反序列化
                com.fasterxml.jackson.databind.ObjectMapper jacksonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.core.type.TypeReference<Map<String, String>> typeRef = new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                };
                return jacksonMapper.readValue(message, typeRef);
            } catch (Exception e) {
                log.error("JSON 解析失败: message={}", message, e);
                throw new RuntimeException("JSON 解析失败", e);
            }

        } else if (messageFormat.equals(MESSAGE_FORMAT_DELIMITER)) {
            // 分隔符格式解析: {key1:value1}{key2:value2}
            String[] parts = message.split(java.util.regex.Pattern.quote(delimiter));
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{(.*?):(.+?)\\}");

            for (String part : parts) {
                java.util.regex.Matcher matcher = pattern.matcher(part.trim());
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    resultMap.put(key, value);
                }
            }
        }

        return resultMap;
    }

    /**
     * 将 JSON 字符串解析成 CDCMessageEntity 对象
     *
     * @param jsonString JSON 字符串
     * @return CDCMessageEntity 对象
     */
    protected CDCMessageEntity parseJsonToCDCMessageEntity(String jsonString) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return JSON.parseObject(jsonString, CDCMessageEntity.class);
        } catch (Exception e) {
            log.error("JSON 解析为 CDCMessageEntity 失败: jsonString={}", jsonString, e);
            throw new RuntimeException("JSON 解析为 CDCMessageEntity 失败", e);
        }
    }

    protected void copyTableColumnsTypeDefine(List<TableColumnEntity> tableColumns, List<ColumnTypeDefineEntity> tableColumnTypeDefineList) throws DataStreamException {
        if (CollectionUtils.isEmpty(tableColumns)) {
            return;
        }

        for (TableColumnEntity iterator : tableColumns) {
            List<ColumnTypeDefineEntity> tableColumnTypeDefineListTemp = tableColumnTypeDefineList.stream().filter(x -> x.getColumnTypeName().equalsIgnoreCase(iterator.getTypeName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(tableColumnTypeDefineListTemp)) {
                throw new DataStreamException(OPER_GET_TABLE_COLUMN_TYPE_DEFINE_ERROR);
            }
            BeanUtils.copyProperties(tableColumnTypeDefineListTemp.get(0), iterator);
        }
    }


    private String insertHandler(Long taskId, Long targetDataSourceId, Integer targetDataBaseType, DebeziumTableEntity targetDebeziumTable, CDCMessageEntity messageEntity) throws DataStreamException {
        List<Map> dataRecordList = new ArrayList<>();
        dataRecordList.add(messageEntity.getDataRecordAfter());
        String insertSql = makeSqlBatchInsert(targetDebeziumTable.getSqlInsertColumns(), taskId, targetDataBaseType, targetDebeziumTable.getTableColumnsList(), dataRecordList);
        if (log.isDebugEnabled()) {
            log.debug("insertSql====" + insertSql);
        }
        return insertSql;
    }


    private String deleteHandler(Long targetDataSourceId, Integer targetDataBaseType, DebeziumTableEntity targetDebeziumTable, CDCMessageEntity messageEntity) throws DataStreamException {
        String deleteSql = null;
        if (!CollectionUtils.isEmpty(targetDebeziumTable.getKeyColumnsList())) {
            deleteSql = makeSqlDeleteRow(messageEntity.getDataRecordBefore(), targetDataBaseType, messageEntity.getTable(), targetDebeziumTable.getKeyColumnsList(), targetDebeziumTable.getTableColumnsList());
            if (log.isDebugEnabled()) {
                log.debug("deleteSql1====" + deleteSql);
            }
        } else {
            //没有主键,所有字段参与删除条件
            List<String> columnsList = targetDebeziumTable.getTableColumnsList().stream().map(a -> a.getColumnName()).collect(Collectors.toList());
            deleteSql = makeSqlDeleteRow(messageEntity.getDataRecordBefore(), targetDataBaseType, messageEntity.getTable(), columnsList, targetDebeziumTable.getTableColumnsList());
            if (log.isDebugEnabled()) {
                log.debug("deleteSql2====" + deleteSql);
            }
        }
        return deleteSql;
    }

    private String updateHandler(Long targetDataSourceId, Integer targetDataBaseType, DebeziumTableEntity targetDebeziumTable, CDCMessageEntity messageEntity) throws DataStreamException {
        String updateSql = null;
        if (!CollectionUtils.isEmpty(targetDebeziumTable.getKeyColumnsList())) {
            updateSql = makeSqlUpdateRow(messageEntity.getDataRecordBefore(), messageEntity.getDataRecordAfter(), targetDataBaseType, messageEntity.getTable(), targetDebeziumTable.getKeyColumnsList(), targetDebeziumTable.getTableColumnsList());
            if (log.isDebugEnabled()) {
                log.debug("updateSql1====" + updateSql);
            }
        } else {
            //没有主键,所有字段参与删除条件
            List<String> columnsList = targetDebeziumTable.getTableColumnsList().stream().map(a -> a.getColumnName()).collect(Collectors.toList());
            updateSql = makeSqlUpdateRow(messageEntity.getDataRecordBefore(), messageEntity.getDataRecordAfter(), targetDataBaseType, messageEntity.getTable(), columnsList, targetDebeziumTable.getTableColumnsList());
            if (log.isDebugEnabled()) {
                log.debug("updateSql2====" + updateSql);
            }
        }
        return updateSql;
    }

    protected String generateDmlSQL(DataMoveTaskEntity dataMoveTask, CDCMessageEntity cdcMessage, DebeziumTableEntity targetDebeziumTable) throws DataStreamException {
        Long targetDataSourceId = dataMoveTask.getTargetObjectId();
        Integer targetDataBaseType = dataMoveTask.getTargetDataBase().getDataBaseType();
        if (cdcMessage.getHandleType().equals(HANDLE_TYPE_INSERT) || cdcMessage.getHandleType().equals(HANDLE_TYPE_READ)) {
            return insertHandler(dataMoveTask.getTaskId(), targetDataSourceId, targetDataBaseType, targetDebeziumTable, cdcMessage);
        } else if (cdcMessage.getHandleType().equals(HANDLE_TYPE_DELETE)) {
            return deleteHandler(targetDataSourceId, targetDataBaseType, targetDebeziumTable, cdcMessage);
        } else if (cdcMessage.getHandleType().equals(HANDLE_TYPE_UPDATE)) {
            return updateHandler(targetDataSourceId, targetDataBaseType, targetDebeziumTable, cdcMessage);
        }
        return null;
    }

}
