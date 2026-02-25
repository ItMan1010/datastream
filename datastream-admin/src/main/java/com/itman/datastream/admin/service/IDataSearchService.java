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
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableColumnEntity;

import java.util.List;
import java.util.Map;

import static com.itman.datastream.common.constant.DataStreamConstant.DATA_SEARCH_KEY_NAME;

public interface IDataSearchService {
    @RouteSource(DATA_SEARCH_KEY_NAME)
    List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException;

    @RouteSource(DATA_SEARCH_KEY_NAME)
    List<Map> executeSelectMapListSql(Long dataSourceId, String selectSql) throws DataStreamException;

    @RouteSource(DATA_SEARCH_KEY_NAME)
    Long executeSelectRecordCountSql(Long dataSourceId, String selectSql) throws DataStreamException;
}