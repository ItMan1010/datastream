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
package com.itman.datastream.connectors.dameng;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.connectors.common.JdbcTableMetaResolver;
import com.itman.datastream.connectors.dameng.dao.DamengTableMetaDao;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.itman.datastream.common.utils.CommUtils.normalizeTypeName;

/**
 * 达梦表元数据解析器，基于达梦 Oracle 兼容视图，保留 NUMBER 精度/小数位、
 * 可空性、默认值与列注释，保证跨库表结构迁移的正确性。
 * <p>达梦 JDBC 驱动的 DatabaseMetaData.getColumns 缺少 IS_AUTOINCREMENT 列，
 * 因此覆写 getTableColumns / getTableInfo / fetchTableMetadata，统一走 DAO。</p>
 */
@Component
public class DamengTableMetaResolver extends JdbcTableMetaResolver {
    private DamengTableMetaResolver(SqlSessionFactory sqlSessionFactory, DataStreamConfig dataStreamConfig) {
        super(sqlSessionFactory, dataStreamConfig);
    }

    @Autowired
    private DamengTableMetaDao damengTableMetaDao;

    @Override
    public Boolean chooseDS(final Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_DAMENG);
    }

    @Override
    public List<TableColumnEntity> getTableColumns(Integer dataSourceType, String schemaName, String userName, String tableName) throws DataStreamException {
        return resolveTableColumns(userName, tableName);
    }

    @Override
    public TableInfoEntity fetchTableMetadata(String schemaName, String tableName) throws DataStreamException {
        TableInfoEntity tableInfo = new TableInfoEntity();
        tableInfo.setColumns(resolveTableColumns(schemaName, tableName));

        Map<String, List<String>> keyColumnMap = new LinkedHashMap<>();
        List<String> primaryKeyColumns = damengTableMetaDao.getDamengPrimaryKeys(schemaName.toUpperCase(), tableName.toUpperCase());
        if (!CollectionUtils.isEmpty(primaryKeyColumns)) {
            List<String> primaryKeyColumnsLower = primaryKeyColumns.stream().map(String::toLowerCase).collect(Collectors.toList());
            keyColumnMap.put("pk_" + tableName.toLowerCase(), primaryKeyColumnsLower);
        }
        tableInfo.setKeyColumnMap(keyColumnMap);

        Map<String, List<String>> indexColumnMap = new LinkedHashMap<>();
        List<DamengIndexEntity> indexes = damengTableMetaDao.getDamengIndexes(schemaName.toUpperCase(), tableName.toUpperCase());
        if (!CollectionUtils.isEmpty(indexes)) {
            for (DamengIndexEntity idx : indexes) {
                indexColumnMap.computeIfAbsent(idx.getIndexName().toLowerCase(), k -> new ArrayList<>())
                        .add(idx.getColumnName().toLowerCase());
            }
            // 达梦/Oracle 主键约束会自动创建同列索引，跳过与主键列完全相同的索引
            if (!CollectionUtils.isEmpty(keyColumnMap)) {
                List<String> primaryKeyColumnsLower = keyColumnMap.values().iterator().next();
                indexColumnMap.entrySet().removeIf(entry -> entry.getValue().equals(primaryKeyColumnsLower));
            }
        }
        tableInfo.setIndexColumnMap(indexColumnMap);
        tableInfo.setForeignKeyMap(new LinkedHashMap<>());
        return tableInfo;
    }

    private List<TableColumnEntity> resolveTableColumns(String userName, String tableName) throws DataStreamException {
        List<DamengTableColumnsEntity> damengColumns = damengTableMetaDao.getDamengTableColumns(userName.toUpperCase(), tableName.toUpperCase());
        if (CollectionUtils.isEmpty(damengColumns)) {
            return new ArrayList<>();
        }

        List<TableColumnEntity> tableColumnList = damengColumns.stream().map(iterator -> {
            // columnSize：NUMBER 取精度（data_precision），其余取长度（data_length），
            // 使 OracleNumberMapper 能按真实精度映射 NUMBER。
            Integer columnSize = iterator.getDataPrecision() != null ? iterator.getDataPrecision() : iterator.getDataLength();
            Integer nullAble = "N".equalsIgnoreCase(iterator.getNullable()) ? 0 : 1;
            return TableColumnEntity.builder()
                    .columnName(iterator.getColumnName().toLowerCase())
                    .typeName(normalizeTypeName(iterator.getDataType()).toLowerCase())
                    .columnSize(columnSize)
                    .decimalDigits(iterator.getDataScale())
                    .nullAble(nullAble)
                    .columnDef(iterator.getDataDefault())
                    .remarks(iterator.getComments())
                    .keyFlag(false)
                    .indexFlag(false)
                    .build();
        }).collect(Collectors.toList());

        List<String> primaryKeyColumns = damengTableMetaDao.getDamengPrimaryKeys(userName.toUpperCase(), tableName.toUpperCase());
        if (!CollectionUtils.isEmpty(primaryKeyColumns)) {
            List<String> primaryKeyColumnsLower = primaryKeyColumns.stream().map(String::toLowerCase).collect(Collectors.toList());
            for (TableColumnEntity column : tableColumnList) {
                if (primaryKeyColumnsLower.contains(column.getColumnName())) {
                    column.setKeyFlag(true);
                    column.setIndexFlag(true);
                }
            }
        }
        return tableColumnList;
    }

    @Override
    public List<TableInfoEntity> getTableInfo(Integer dataSourceType, String schemaName) throws DataStreamException {
        return damengTableMetaDao.getDamengTableInfo(schemaName);
    }
}