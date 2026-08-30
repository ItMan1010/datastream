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


import com.itman.datastream.admin.service.ITableInfoService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class TableInfoServiceImpl implements ITableInfoService {
    private final DataSourceFactory dataSourceFactory;


    @Override
    public List<TableInfoEntity> getTableInfo(Long dataSourceId, DataBaseEntity dataSource) throws DataStreamException {
        String schemaParam = resolveSchemaName(dataSource);
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).getTableInfo(dataSource.getDataBaseType(), schemaParam);
    }

    @Override
    public List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException {
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).getTableColumns(dataSource.getDataBaseType(), dataSource.getSchemaName(), dataSource.getUserName(), tableName);
    }

    @Override
    public TableInfoEntity fetchTableMetadata(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException {
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).fetchTableMetadata(resolveSchemaName(dataSource), tableName);
    }

    /**
     * Oracle/达梦的 JDBC URL 不含 database 路径，schema 由连接用户名（owner）确定；
     * 其余数据库使用 URL 中解析出的 schemaName。
     */
    private String resolveSchemaName(DataBaseEntity dataSource) {
        Integer type = dataSource.getDataBaseType();
        if (type != null && (type == DataBaseEnum.ORACLE.getId() || type == DataBaseEnum.DAMENG.getId())) {
            return dataSource.getUserName();
        }
        return dataSource.getSchemaName();
    }
}
