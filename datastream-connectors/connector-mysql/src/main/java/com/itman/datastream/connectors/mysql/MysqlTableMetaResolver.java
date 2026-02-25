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
package com.itman.datastream.connectors.mysql;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.connectors.common.JdbcTableMetaResolver;
import com.itman.datastream.connectors.mysql.dao.MysqlTableMetaDao;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.constant.DataStreamConstant.DATA_SOURCE_TYPE_MEM;


@Component
public class MysqlTableMetaResolver extends JdbcTableMetaResolver {
    private MysqlTableMetaResolver(SqlSessionFactory sqlSessionFactory, DataStreamConfig dataStreamConfig) {
        super(sqlSessionFactory, dataStreamConfig); // 调用父类的构造器
    }

    @Autowired
    private MysqlTableMetaDao mysqlTableMetaDao;


    @Override
    public Boolean chooseDS(final Integer dataSourceType) {
        return Arrays.asList(DATA_SOURCE_TYPE_MYSQL, DATA_SOURCE_TYPE_SHARDING, DATA_SOURCE_TYPE_DORIS, DATA_SOURCE_TYPE_MEM).contains(dataSourceType);
    }

    public List<TableColumnEntity> getTableColumns(Integer dataSourceType, String schemaName, String userName, String tableName) throws DataStreamException {
        //使用父类jdbc的
        List<TableColumnEntity> tableMeta = super.getTableColumns(dataSourceType, schemaName, userName, tableName);

        if (dataSourceType.equals(DATA_SOURCE_TYPE_DORIS) && !CollectionUtils.isEmpty(tableMeta)) {
            List<String> keyColumnNameList = mysqlTableMetaDao.getDorisPrimaryKeys(schemaName, tableName.toLowerCase());
            if (CollectionUtils.isEmpty(keyColumnNameList)) {
                keyColumnNameList = mysqlTableMetaDao.getDorisPrimaryKeys(schemaName, tableName.toUpperCase());
            }
            if (!CollectionUtils.isEmpty(keyColumnNameList)) {
                for (TableColumnEntity iterator : tableMeta) {
                    for (String iterator2 : keyColumnNameList) {
                        if (iterator2.equalsIgnoreCase(iterator.getColumnName())) {
                            iterator.setKeyFlag(true);
                            break;
                        }
                    }
                }
            }
        }

        return tableMeta;
    }

    public List<TableInfoEntity> getTableInfo(Integer dataSourceType, String schemaName) throws DataStreamException {
        return mysqlTableMetaDao.getMySQLTableInfo(schemaName);
    }
}

