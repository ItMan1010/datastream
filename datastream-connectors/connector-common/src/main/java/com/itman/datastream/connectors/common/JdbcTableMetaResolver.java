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
package com.itman.datastream.connectors.common;

import com.itman.datastream.common.api.ITableMetaApi;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.ForeignKeyInfo;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.util.CollectionUtils;
import static com.itman.datastream.common.utils.CommUtils.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import static com.itman.datastream.common.errcode.DataStreamErrorCode.DAO_GET_TABLE_META_OTHER_ERROR;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.DAO_GET_TABLE_META_SQL_ERROR;

@Slf4j
public abstract class JdbcTableMetaResolver implements ITableMetaApi {
    private final SqlSessionFactory sqlSessionFactory;
    public final DataStreamConfig dataStreamConfig;

    public JdbcTableMetaResolver(SqlSessionFactory sqlSessionFactory, DataStreamConfig dataStreamConfig) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.dataStreamConfig = dataStreamConfig;
    }

    private Map<String, List<String>> getPrimaryKeys(String schemaName, String tableName, DatabaseMetaData databaseMetaData) throws SQLException {
        // 使用LinkedHashMap保持主键列的顺序
        Map<String, List<String>> primaryKeys = new LinkedHashMap<>();

        try (ResultSet rs = databaseMetaData.getPrimaryKeys(schemaName, null, tableName)) {
            String pkName = null;
            List<String> columns = new ArrayList<>();

            while (rs.next()) {
                String currentPkName = rs.getString("PK_NAME").toLowerCase();
                String columnName = rs.getString("COLUMN_NAME").toLowerCase();
                short keySeq = rs.getShort("KEY_SEQ");  // 列在键中的位置

                // 处理新主键或复合主键
                if (pkName == null || !pkName.equals(currentPkName)) {
                    // 保存前一个主键信息
                    if (pkName != null) {
                        primaryKeys.put(pkName, new ArrayList<>(columns));
                        columns.clear();
                    }
                    pkName = currentPkName;
                }

                // 按顺序添加列（KEY_SEQ从1开始）
                if (columnName != null && !columnName.isEmpty()) {
                    // 确保顺序正确，直接使用KEY_SEQ作为索引
                    while (columns.size() < keySeq) {
                        columns.add(null);  // 占位符
                    }
                    columns.set(keySeq - 1, columnName.toLowerCase());
                }
            }

            // 添加最后一个主键
            if (pkName != null && !columns.isEmpty()) {
                primaryKeys.put(pkName, new ArrayList<>(columns));
            }
        }

        return primaryKeys;
    }


    private Map<String, List<String>> getIndexInfo(String schemaName, String tableName, DatabaseMetaData databaseMetaData) throws SQLException {
        Map<String, List<String>> indexMap = new LinkedHashMap<>();

        try (ResultSet rs = databaseMetaData.getIndexInfo(schemaName, null, tableName, false, false)) {
            while (rs.next()) {
                // 跳过统计信息类型的索引
                if (rs.getShort("TYPE") == DatabaseMetaData.tableIndexStatistic) {
                    continue;
                }

                String indexName = rs.getString("INDEX_NAME").toLowerCase();
                String columnName = rs.getString("COLUMN_NAME").toLowerCase();

                // 处理可能为空的列名
                if (columnName == null || columnName.isEmpty()) {
                    continue;
                }

                // 排除主键索引：通过索引名或列名判断
                if ("primary".equalsIgnoreCase(indexName)) {
                    continue;
                }

                // 将列名转为小写（可选）
                columnName = columnName.toLowerCase();

                // 处理组合索引
                if (!indexMap.containsKey(indexName)) {
                    indexMap.put(indexName, new ArrayList<>());
                }
                indexMap.get(indexName).add(columnName);
            }
        }

        return indexMap;
    }


    private Map<String, ForeignKeyInfo> getForeignKeys(String schemaName, String tableName, DatabaseMetaData databaseMetaData) throws SQLException {
        Map<String, ForeignKeyInfo> foreignKeyMap = new LinkedHashMap<>();

        try (ResultSet rs = databaseMetaData.getImportedKeys(schemaName, null, tableName)) {
            while (rs.next()) {
                String fkName = rs.getString("FK_NAME");
                if (fkName == null || fkName.isEmpty()) {
                    continue;
                }
                fkName = fkName.toLowerCase();

                String pkTable = rs.getString("PKTABLE_NAME");
                if (pkTable == null || pkTable.isEmpty()) {
                    continue;
                }
                String pkTableLower = pkTable.toLowerCase();

                String pkColumn = rs.getString("PKCOLUMN_NAME");
                if (pkColumn == null || pkColumn.isEmpty()) {
                    continue;
                }
                String pkColumnLower = pkColumn.toLowerCase();

                String fkColumn = rs.getString("FKCOLUMN_NAME");
                if (fkColumn == null || fkColumn.isEmpty()) {
                    continue;
                }
                String fkColumnLower = fkColumn.toLowerCase();

                int updateRule = rs.getInt("UPDATE_RULE");
                int deleteRule = rs.getInt("DELETE_RULE");

                ForeignKeyInfo fkInfo = foreignKeyMap.computeIfAbsent(fkName, k -> new ForeignKeyInfo());
                fkInfo.setFkName(fkName);
                fkInfo.setPkTableName(pkTableLower);
                fkInfo.addColumnMapping(pkColumnLower, fkColumnLower);
                fkInfo.setUpdateRule(getRuleName(updateRule));
                fkInfo.setDeleteRule(getRuleName(deleteRule));
            }
        }

        return foreignKeyMap;
    }

    /**
     * 转换规则代码为名称
     */
    private String getRuleName(int rule) {
        switch (rule) {
            case DatabaseMetaData.importedKeyCascade:
                return "CASCADE";
            case DatabaseMetaData.importedKeyRestrict:
                return "RESTRICT";
            case DatabaseMetaData.importedKeySetNull:
                return "SET NULL";
            case DatabaseMetaData.importedKeyNoAction:
                return "NO ACTION";
            case DatabaseMetaData.importedKeySetDefault:
                return "SET DEFAULT";
            default:
                return "NO ACTION";
        }
    }

    private List<TableColumnEntity> getColumns(String schemaName, String tableName, DatabaseMetaData databaseMetaData) throws SQLException {
        List<TableColumnEntity> columnList = new ArrayList<>();
        try (ResultSet rs = databaseMetaData.getColumns(schemaName, null, tableName, null)) {
            while (rs.next()) {
                // 获取列名并检查空值
                String columnName = rs.getString("COLUMN_NAME");
                if (columnName == null || columnName.isEmpty()) {
                    // 跳过列名为空或空字符串的记录（理论上不应出现，但安全起见）
                    continue;
                }

                // 使用构建器模式创建实体，对可能为null的字段进行安全处理
                TableColumnEntity columnBean = TableColumnEntity.builder().columnName(columnName.toLowerCase()) // 确保列名小写
                        .columnSize(rs.getInt("COLUMN_SIZE")).decimalDigits(rs.getInt("DECIMAL_DIGITS")).dataType(rs.getInt("DATA_TYPE")).typeName(rs.getString("TYPE_NAME").toLowerCase()).nullAble(rs.getInt("NULLABLE"))
                        // 处理可能为null的字段：使用rs.wasNull()判断并设置默认值
                        .columnDef(getNullableString(rs, "COLUMN_DEF")).remarks(getNullableString(rs, "REMARKS")).ordinalPosition(rs.getInt("ORDINAL_POSITION")).isAutoIncrement(getNullableString(rs, "IS_AUTOINCREMENT")).isNullable(getNullableString(rs, "IS_NULLABLE")).build();

                columnBean.setTypeName(normalizeTypeName(columnBean.getTypeName()));
                columnList.add(columnBean);
            }
        }
        return columnList;
    }

    private String getNullableString(ResultSet rs, String columnLabel) throws SQLException {
        String value = rs.getString(columnLabel);
        return rs.wasNull() ? null : value;
    }

    public TableInfoEntity fetchTableMetadata(String schemaName, String tableName) throws DataStreamException {
        //不同数据库schemaName和tableName会关注大小写，特别oracle需要大写才能查询出字段
        //为了统一适配不同类型数据库，则先小写查询，如果没有则再试一次大写查询
        TableInfoEntity tableInfo = fetchTableMetadataProcess(schemaName.toLowerCase(), tableName.toLowerCase());
        if (tableInfo == null || CollectionUtils.isEmpty(tableInfo.getColumns())) {
            tableInfo = fetchTableMetadataProcess(schemaName.toUpperCase(), tableName.toUpperCase());
        }
        return tableInfo;
    }

    public TableInfoEntity fetchTableMetadataProcess(String schemaName, String tableName) throws DataStreamException {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            TableInfoEntity tableInfo = new TableInfoEntity();
            DatabaseMetaData databaseMetaData = sqlSession.getConnection().getMetaData();
            //获取主键
            Map<String, List<String>> keyColumnMap = getPrimaryKeys(schemaName, tableName, databaseMetaData);
            tableInfo.setKeyColumnMap(keyColumnMap);
            //获取索引
            Map<String, List<String>> indexColumnMap = getIndexInfo(schemaName, tableName, databaseMetaData);
            tableInfo.setIndexColumnMap(indexColumnMap);
            //获取外键
            Map<String, ForeignKeyInfo> foreignKeyMap = getForeignKeys(schemaName, tableName, databaseMetaData);
            tableInfo.setForeignKeyMap(foreignKeyMap);

            //获取字段
            List<TableColumnEntity> tableColumnList = getColumns(schemaName, tableName, databaseMetaData);
            // 标记主键和索引状态
            for (TableColumnEntity tableColumn : tableColumnList) {
                if (tableColumn.getColumnName() == null) continue;
                String normalizedName = tableColumn.getColumnName().toLowerCase();

                if (!CollectionUtils.isEmpty(keyColumnMap)) {
                    String pkName = keyColumnMap.keySet().iterator().next();
                    List<String> pkColumns = keyColumnMap.get(pkName);
                    tableColumn.setKeyFlag(pkColumns.contains(normalizedName));
                }
            }

            tableInfo.setColumns(tableColumnList);
            return tableInfo;
        } catch (SQLException e) {
            log.error("SQLException=", e);
            throw new DataStreamException(DAO_GET_TABLE_META_SQL_ERROR);
        } catch (Exception e) {
            log.error("Exception=", e);
            throw new DataStreamException(DAO_GET_TABLE_META_OTHER_ERROR);
        }
    }


    public List<TableColumnEntity> getTableColumns(Integer dataSourceType, String schemaName, String userName, String tableName) throws DataStreamException {
        TableInfoEntity TableInfo = fetchTableMetadata(schemaName, tableName.toLowerCase());
        if (TableInfo == null || CollectionUtils.isEmpty(TableInfo.getColumns())) {
            TableInfo = fetchTableMetadata(schemaName, tableName.toUpperCase());
        }
        return TableInfo.getColumns();
    }
}

