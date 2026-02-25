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
package com.itman.datastream.admin.service.impl;

import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.admin.service.IDataSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSearchServiceImpl implements IDataSearchService {
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;

    @Override
    public List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException {
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).getTableColumns(dataSource.getDataBaseType(), dataSource.getSchemaName(), dataSource.getUserName(), tableName);
    }

    @Override
    public List<Map> executeSelectMapListSql(Long dataSourceId, String selectSql) throws DataStreamException {
        return dataStreamDao.executeSelectMapListSql(selectSql);
    }

    @Override
    public Long executeSelectRecordCountSql(Long dataSourceId, String selectSql) throws DataStreamException {
        return dataStreamDao.executeSelectRecordCountSql(selectSql);
    }
}
