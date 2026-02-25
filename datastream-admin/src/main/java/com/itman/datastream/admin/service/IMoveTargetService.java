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
package com.itman.datastream.admin.service;

import com.itman.datastream.engine.route.RouteSource;
import com.itman.datastream.common.entity.DataMoveInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableColumnEntity;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.SOURCE_DATA_MOVE_TARGET_KEY_NAME;

public interface IMoveTargetService extends ITableCount {
    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Integer insertDataList(Long dataSourceId, Long infoId, Integer dataCount, String pageRowEnd, String insertSql) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Integer insertDataListBindVar(Long dataSourceId, String insertSqlColumns, List<List<Object>> dataListTarget) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Long executeSelectRecordCountSql(Long dataSourceId, String selectSql) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Integer insertDataMoveInfo(Long dataSourceId, Integer dataSourceType, List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(Long dataSourceId, Long infoId) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Integer executeMetaDbSql(Long dataSourceId, String metaDbSql) throws DataStreamException;

    @RouteSource(SOURCE_DATA_MOVE_TARGET_KEY_NAME)
    Integer deleteDataList(Long dataSourceId, List<String> dataDeleteSqlList) throws DataStreamException;
}
