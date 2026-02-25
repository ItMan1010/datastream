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


import cn.hutool.extra.spring.SpringUtil;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.admin.service.IMoveSourceService;
import com.itman.datastream.admin.service.IMoveTargetService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.extend.TaskParamExtend;
import com.itman.datastream.engine.holder.DataStreamHolder;
import com.itman.datastream.engine.route.DataBaseSource;
import com.itman.datastream.engine.route.RegisterDataBase;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.*;


@Slf4j
@Component
public class DataCheckHandler extends AbstractHandler {
    private final IMetaService metaService;
    private final IMoveSourceService moveSourceService;
    private final RegisterDataBase registerDataBase;
    private final DataStreamHolder dataStreamHolder;


    public DataCheckHandler(DataSourceFactory dataSourceFactory, IMetaService metaService, IMoveSourceService moveSourceService, IMoveTargetService moveTargetService, DataStreamConfig dataStreamConfig, DataBaseSource dataBaseSource, ISystemLogEvent systemLogEvent, DataStreamHolder dataStreamHolder, RegisterDataBase registerDataBase, TableInfoHandler tableInfoHandler, TaskParamExtend taskParamExtend) {
        super(dataSourceFactory, dataStreamConfig, metaService, dataStreamHolder, moveTargetService);
        this.metaService = metaService;
        this.moveSourceService = moveSourceService;
        this.dataStreamHolder = dataStreamHolder;
        this.registerDataBase = registerDataBase;
    }

    public String makeSourceSelectSqlColumns(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        Boolean isOrderBy = dataMoveTask.getSourceLoadStrategy().equals(LOAD_STRATEGY_BY_DATA_PART) ? false : true;
        Integer dataSourceType = dataMoveTask.getSourceDataBase().getDataBaseType();
        return super.matchDataBase(dataSourceType).makeSqlSelectColumns(dataMoveTask.getSourceObjectName(), dataMoveTask.getSourceObjectCondition(), dataMoveTask.getSourceTableColumns(), isOrderBy);
    }

    public String makeTargetInsertSqlColumns(DataMoveTaskEntity dataMoveTask) {
        return super.makeSqlInsertColumns(dataMoveTask.getTargetObjectName(), "", dataMoveTask.getTargetTableColumns());
    }

    public List<DataCheckEntity> getDataCheck(Long taskId) throws DataStreamException {
        return metaService.queryDataCheck(taskId);
    }

    public Integer getDataCheckCount(Long taskId) throws DataStreamException {
        return metaService.queryDataCheck(taskId).size();
    }

    public List<DataCheckEntity> queryDataCheckById(Long dataCheckId) throws DataStreamException {
        return metaService.queryDataCheckById(dataCheckId);
    }

    private void registerDataBase(DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = new ArrayList<>();
        if (isDataBaseDataSource(dataMoveTask.getSourceObjectType())) {
            dataBaseList.add(dataMoveTask.getSourceDataBase());
            dataMoveTask.setSourceTableColumnTypeDefineList(SpringUtil.getBean(DataBaseHandler.class).queryColumnTypeDefine(dataMoveTask.getSourceDataBase().getDataBaseType()));
        }

        if (isDataBaseDataSource(dataMoveTask.getTargetObjectType())) {
            dataBaseList.add(dataMoveTask.getTargetDataBase());
            dataMoveTask.setTargetTableColumnTypeDefineList(SpringUtil.getBean(DataBaseHandler.class).queryColumnTypeDefine(dataMoveTask.getTargetDataBase().getDataBaseType()));
        }

        if (!CollectionUtils.isEmpty(dataBaseList)) {
            registerDataBase.registerDataSources(dataBaseList, dataMoveTask.getTaskId());
        }
    }

    public void repairDataCheck(Integer checkType, Long taskId, Long dataCheckId) throws DataStreamException {
        List<DataMoveTaskEntity> dataMoveTaskList = new ArrayList<>();
        List<DataCheckEntity> dataCheckList = new ArrayList<>();
        if (checkType.equals(CHECK_TYPE_BY_TASK_ID)) {
            dataMoveTaskList = metaService.queryTaskByTaskId(taskId);
            if (CollectionUtils.isEmpty(dataMoveTaskList)) {
                throw new DataStreamException(OPER_TASK_DATA_FETCH_ERROR);
            }

            dataCheckList = metaService.queryDataCheck(taskId);
            if (CollectionUtils.isEmpty(dataCheckList)) {
                throw new DataStreamException(OPER_TASK_NO_DIFF_DATA_FOUND);
            }
        } else if (checkType.equals(CHECK_TYPE_BY_CHECK_ID)) {
            dataCheckList = metaService.queryDataCheckById(dataCheckId);
            if (CollectionUtils.isEmpty(dataCheckList)) {
                throw new DataStreamException(OPER_TASK_NO_DIFF_DATA_FOUND);
            }

            dataMoveTaskList = metaService.queryTaskByTaskId(dataCheckList.get(0).getTaskId());
            if (CollectionUtils.isEmpty(dataMoveTaskList)) {
                throw new DataStreamException(OPER_TASK_DATA_FETCH_ERROR);
            }
        }

        //这里是注册，后面需要释放
        DataMoveTaskEntity dataMoveTask = dataMoveTaskList.get(0);

        registerDataBase(dataMoveTask);

        List<TableColumnEntity> sourceTableColumnAllList = moveSourceService.getTableColumns(dataMoveTask.getSourceDataBase().getDataBaseId(), dataMoveTask.getSourceDataBase(), dataMoveTask.getSourceObjectName());

        List<TableColumnEntity> sourceTableColumList = sourceTableColumnAllList.stream().filter(column -> !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_COUNT) && !column.getColumnName().equalsIgnoreCase(TARGET_TABLE_ADD_COLUMNS_BACK_TASK_ID)).collect(Collectors.toList());
        dataMoveTask.setSourceTableColumns(sourceTableColumList);
        if (CollectionUtils.isEmpty(dataMoveTask.getSourceTableColumns())) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_SOURCE_TABLE_NULL_ERROR);
        }

        dataMoveTask.setSourceKeyColumns(dataMoveTask.getSourceTableColumns().stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
        dataMoveTask.setSourceTableKeysList(dataMoveTask.getSourceTableColumns().stream().filter(x -> x.isKeyFlag()).collect(Collectors.toList()));

        dataMoveTask.setTargetTableColumns(moveTargetService.getTableColumns(dataMoveTask.getTargetDataBase().getDataBaseId(), dataMoveTask.getTargetDataBase(), dataMoveTask.getTargetObjectName()));
        if (CollectionUtils.isEmpty(dataMoveTask.getTargetTableColumns())) {
            throw new DataStreamException(DataStreamErrorCode.OPER_TASK_TARGET_TABLE_NULL_ERROR);
        }

        dataMoveTask.setTargetKeyColumns(dataMoveTask.getTargetTableColumns().stream().filter(x -> x.isKeyFlag()).map(a -> a.getColumnName()).collect(Collectors.toList()));
        dataMoveTask.setTargetTableKeysList(dataMoveTask.getTargetTableColumns().stream().filter(x -> x.isKeyFlag()).collect(Collectors.toList()));

        SpringUtil.getBean(DataBaseHandler.class).copyTableColumnsTypeDefine(dataMoveTask.getSourceTableColumns(), dataMoveTask.getSourceTableColumnTypeDefineList());
        dataMoveTask.setSourceSelectSqlColumns(makeSourceSelectSqlColumns(dataMoveTask));

        SpringUtil.getBean(DataBaseHandler.class).copyTableColumnsTypeDefine(dataMoveTask.getTargetTableColumns(), dataMoveTask.getTargetTableColumnTypeDefineList());
        dataMoveTask.setTargetInsertSqlColumns(makeTargetInsertSqlColumns(dataMoveTask));

        for (DataCheckEntity iterator : dataCheckList) {
            if (iterator.getState().equals(DATA_CHECK_STATE_MODIFY_SUCCESS)) {
                continue;
            }

            String errorCode = null;
            String errorMsg = null;
            try {
                repairDataCheck(iterator, dataMoveTask);
            } catch (DataStreamException aie) {
                errorCode = aie.getErrCode();
                errorMsg = aie.getErrMsg();
            } catch (Exception e) {
                errorCode = UNKNOWN_ERROR.getCode();
                errorMsg = UNKNOWN_ERROR.getMessage();
            }

            updateDataCheck(iterator.getDataCheckId(), iterator.getState(), errorCode, errorMsg);
        }

        registerDataBase.releaseTaskDataSources(dataMoveTask.getTaskId());
    }

    public void repairDataCheck(DataCheckEntity dataCheck, DataMoveTaskEntity dataMoveTask) throws DataStreamException {
        //去目标执行数据插入、删除、更新操作
        Integer recordResult = 0;
        if (dataCheck.getCheckResult().equals(DATA_CHECK_RESULT_SOURCE_MORE)) {
            //组装根据主键查询源数据语句
            String selectSql = dataMoveTask.getSourceSelectSqlColumns() + " where " + dataCheck.getCheckKeys();
            //查询源数据数据
            List<Map> dataRowList = moveSourceService.executeSelectMapListSql(dataMoveTask.getSourceObjectId(), selectSql);
            if (CollectionUtils.isEmpty(dataRowList)) {
                throw new DataStreamException(OPER_SOURCE_DATA_QUERY_BY_PK_ERROR);
            }

            recordResult = SpringUtil.getBean(DataMoveHandler.class).handleWriteByDataSource(dataRowList, dataMoveTask);
        } else if (dataCheck.getCheckResult().equals(DATA_CHECK_RESULT_NOTEQUAL)) {
            //todo update
        } else if (dataCheck.getCheckResult().equals(DATA_CHECK_RESULT_SOURCE_LESS)) {
            String deleteSql = "delete from " + dataMoveTask.getTargetObjectName() + " where " + dataCheck.getCheckKeys();
            List<String> dataDeleteSqlList = new ArrayList<>();
            dataDeleteSqlList.add(deleteSql);
            recordResult = moveTargetService.deleteDataList(dataMoveTask.getTargetObjectId(), dataDeleteSqlList);
        }

        if (recordResult.equals(0)) {
            throw new DataStreamException(OPER_REPAIR_TARGET_DATA_CHECK_ERROR);
        }
    }

    public void updateDataCheck(Long dataCheckId, Integer oldState, String errorCode, String errorMsg) {
        try {
            if (errorCode == null) {
                metaService.updateDataCheck(dataCheckId, oldState, DATA_CHECK_STATE_MODIFY_SUCCESS, null, null);
            } else {
                metaService.updateDataCheck(dataCheckId, oldState, DATA_CHECK_STATE_MODIFY_FAIL, errorCode, errorMsg);
            }
        } catch (DataStreamException e) {
            log.error("Exception=", e);
        }
    }
}
