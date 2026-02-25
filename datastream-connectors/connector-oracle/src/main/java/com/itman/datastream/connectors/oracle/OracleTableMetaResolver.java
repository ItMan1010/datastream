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
package com.itman.datastream.connectors.oracle;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.common.entity.OracleTableColumnsEntity;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.connectors.common.JdbcTableMetaResolver;
import com.itman.datastream.connectors.oracle.dao.OracleTableMetaDao;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import static com.itman.datastream.common.utils.CommUtils.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class OracleTableMetaResolver extends JdbcTableMetaResolver {
    private OracleTableMetaResolver(SqlSessionFactory sqlSessionFactory, DataStreamConfig dataStreamConfig) {
        super(sqlSessionFactory, dataStreamConfig); // 调用父类的构造器
    }

    @Autowired
    private OracleTableMetaDao oracleTableMetaDao;

    @Override
    public Boolean chooseDS(final Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_ORACLE);
    }


    public List<TableColumnEntity> getTableColumns(Integer dataSourceType, String schemaName, String userName, String tableName) throws DataStreamException {
        return getOracleTableColumns(userName, tableName);
    }

    private List<TableColumnEntity> getOracleTableColumns(String userName, String tableName) throws DataStreamException {
        List<OracleTableColumnsEntity> oracleTableColumnsEntityList = oracleTableMetaDao.getOracleTableColumns(userName.toUpperCase(), tableName.toUpperCase());
        if (CollectionUtils.isEmpty(oracleTableColumnsEntityList)) {
            return new ArrayList<>();
        }

        List<TableColumnEntity> tableColumnEntityList = oracleTableColumnsEntityList.stream().map(iterator -> TableColumnEntity.builder().
                columnName(iterator.getColumnName().toLowerCase()).
                typeName(normalizeTypeName(iterator.getDataType()).toLowerCase()).
                columnSize(iterator.getDataLength()).keyFlag(false).indexFlag(false).build()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(tableColumnEntityList)) {
            List<String> oracleKeyColumnsList = oracleTableMetaDao.getOraclePrimaryKeys(userName.toUpperCase(), tableName.toUpperCase());
            if (!CollectionUtils.isEmpty(oracleKeyColumnsList)) {
                List<String> oracleKeyColumnsListTmp = oracleKeyColumnsList.stream().map(x->x.toLowerCase()).collect(Collectors.toList());
                tableColumnEntityList.stream().filter(column -> oracleKeyColumnsListTmp.contains(column.getColumnName())).forEach(column -> {
                    column.setKeyFlag(true);
                    column.setIndexFlag(true);
                });
            }
        }
        return tableColumnEntityList;
    }

    public List<TableInfoEntity> getTableInfo(Integer dataSourceType, String schemaName) throws DataStreamException {
        return oracleTableMetaDao.getOracleTableInfo(schemaName);
    }
}

