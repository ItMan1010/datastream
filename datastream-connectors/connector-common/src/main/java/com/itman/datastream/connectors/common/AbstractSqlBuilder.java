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

import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.TableColumnEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;

@Slf4j
@AllArgsConstructor
public abstract class AbstractSqlBuilder implements IDatabaseAdapter {
    /**
     * 获取当前数据库类型，由子类实现
     */
    public abstract DataBaseEnum getDataBaseType();

    // 抽象方法，由子类提供具体的数据库适配器
    public abstract String stringToDate(String columnValue);

    public abstract String timeToString(String columnType, String columnValue);

    /**
     * 生成分页取数sql
     *
     * @param currentKeysValue
     * @param tableKeyColumns
     * @param tableName
     * @param selectCondition
     * @param tableMinKeyValue
     * @param pageRowSize
     * @return
     */
    public abstract String makeSqlCurrentPageMaxKeyValue(String currentKeysValue, String tableKeyColumns, String tableName, String selectCondition, String tableMinKeyValue, Integer pageRowSize);

    public abstract String makeSqlSelectByPage(final Integer loadStrategy, String selectColumns, String selectCondition, String selectBeginKeyValue, String tableMinKeyValue, String tableKeyColumns, Integer selectCount);

    public abstract String makeSqlBatchInsert(final String insertSqlColumns, Long taskId, List<TableColumnEntity> tableColumns, List<Map> dataRecordList, Integer dataStreamParallelStreamSize);

    public String makeSqlSelectColumns(String tableName, String tableCondition, List<TableColumnEntity> tableColumns, Boolean isOrderBy) {
        // 直接使用列名，不做时间格式转换
        // JDBC 会自动将数据库时间类型转换为 java.sql.Timestamp/java.sql.Date 等标准类型
        // 在 INSERT/UPDATE 时由 formatColumnValueForInsert 统一处理格式化
        List<String> sourceTableColumnList = tableColumns.stream()
                .map(TableColumnEntity::getColumnName)
                .collect(Collectors.toList());

        String keyColumn = tableColumns.stream()
                .filter(column -> column.isKeyFlag())
                .map(TableColumnEntity::getColumnName)
                .findFirst()
                .orElse(null);

        StringBuffer selectSql = new StringBuffer();
        selectSql.append("select ").append(sourceTableColumnList.stream().collect(Collectors.joining(",")))
                .append(" from ").append(tableName.toLowerCase());

        if (!StringUtils.isEmpty(tableCondition)) {
            selectSql.append(" where ").append(tableCondition);
        }

        if (keyColumn != null && isOrderBy) {
            selectSql.append(" order by ").append(keyColumn);
        }
        return selectSql.toString();
    }

    public String makeSqlSelectCountByKey(final Map dataRecord, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final String tableName) {
        List<String> dataKeyValueList = makeSqlKeyColumnSelect(keyColumns, tableColumns, dataRecord);
        if (CollectionUtils.isEmpty(dataKeyValueList)) {
            return null;
        }

        StringBuffer selectSql = new StringBuffer();
        selectSql.append("select count(1) from ");
        selectSql.append(tableName.toLowerCase());
        selectSql.append(" where ");
        selectSql.append(dataKeyValueList.stream().collect(Collectors.joining(" and ")));
        return selectSql.toString();
    }

    public List<String> makeSqlKeyColumnSelect(List<String> keyColumns, List<TableColumnEntity> tableColumns, Map dataRecord) {
        List<String> dataRowList = new ArrayList<>();
        for (String columnName : keyColumns) {
            Object columnValue = dataRecord.get(columnName);
            if (Objects.isNull(columnValue)) {
                columnValue = dataRecord.get(columnName.toLowerCase());
            }

            if (columnValue != null) {
                boolean isVarcharType = tableColumns.stream().filter(column -> columnName.equalsIgnoreCase(column.getColumnName())).findFirst().map(column -> column.getColumnTypeClassify().equals(COLUMN_TYPE_CLASSIFY_STRING)).orElse(false);

                String temp = isVarcharType ? "'" : "";
                dataRowList.add(columnName + "=" + temp + columnValue + temp);
            }
        }
        return dataRowList;
    }

    public String makeSqlInsertRow(Long taskId, List<TableColumnEntity> tableColumns, Map dataRow) {
        List<String> dataColumnList = new ArrayList<>();
        for (TableColumnEntity iterator : tableColumns) {
            Object columnValue = dataRow.get(iterator.getColumnName());
            if (Objects.isNull(columnValue)) {
                columnValue = dataRow.get(iterator.getColumnName().toLowerCase());
                if (Objects.isNull(columnValue) && TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID.equalsIgnoreCase(iterator.getColumnName())) {
                    columnValue = taskId;
                }
            }

            if (columnValue != null) {
                String valueStr = formatColumnValueForInsert(iterator, columnValue);
                dataColumnList.add(valueStr);
            } else {
                dataColumnList.add("null");
            }
        }
        return "( " + dataColumnList.stream().collect(Collectors.joining(",")) + ")";
    }

    /**
     * 通用列值格式化方法（支持所有主流数据库）
     * 处理：Clob、Blob、日期时间、数组、BigDecimal、PGobject 等各种类型
     *
     * @param column 列定义信息
     * @param columnValue 列值
     * @return 格式化后的 SQL 值字符串
     */
    protected String formatColumnValueForInsert(TableColumnEntity column, Object columnValue) {
        if (columnValue == null) {
            return "null";
        }

        Class<?> clazz = columnValue.getClass();

        // 1. 标准 JDBC 日期时间类型（所有数据库通用）
        if (java.util.Date.class.isAssignableFrom(clazz)) {
            return formatDateTimeValue(columnValue);
        }

        // 2. Java 8+ 日期时间类型（LocalDateTime、LocalDate、LocalTime 等）
        // 现代 JDBC 驱动可能返回这些类型而非 java.util.Date
        if (isJavaDateTimeType(clazz)) {
            return formatJavaDateTimeValue(columnValue);
        }

        // 3. 标准 JDBC Clob 接口（Oracle CLOB、MySQL TEXT 等）
        if (columnValue instanceof Clob) {
            return formatClobValue((Clob) columnValue);
        }

        // 4. 标准 JDBC Blob 接口（Oracle BLOB、MySQL BLOB 等）
        if (columnValue instanceof Blob) {
            return formatBlobValue((Blob) columnValue);
        }

        // 5. 字节数组
        if (columnValue instanceof byte[]) {
            return formatBytesValue((byte[]) columnValue);
        }

        // 6. JDBC 数组（PostgreSQL 数组等）
        if (columnValue instanceof Array) {
            return formatArrayValue((Array) columnValue);
        }

        // 7. 字符串类型（需要转义单引号）
        if (columnValue instanceof String) {
            // 特殊处理：如果列类型是日期时间，但 JDBC 返回的是字符串
            // 需要将字符串格式转换为标准日期时间格式
            // 某些 JDBC 驱动（如 MySQL）可能将日期返回为字符串
            if (column.getColumnTypeClassify() != null
                    && column.getColumnTypeClassify().equals(COLUMN_TYPE_CLASSIFY_DATETIME)) {
                return formatDateTimeString((String) columnValue);
            }
            return formatStringValue((String) columnValue);
        }

        // 8. BigDecimal（避免科学计数法）
        if (columnValue instanceof BigDecimal) {
            return formatBigDecimal((BigDecimal) columnValue);
        }

        // 9. 其他特殊对象（PGobject、STRUCT 等）- 尝试反射或 JSON 序列化
        return formatObjectValue(columnValue, clazz);
    }

    /**
     * 判断是否为 Java 8+ 日期时间类型
     */
    private boolean isJavaDateTimeType(Class<?> clazz) {
        String className = clazz.getName();
        return className.equals("java.time.LocalDateTime")
                || className.equals("java.time.LocalDate")
                || className.equals("java.time.LocalTime")
                || className.equals("java.time.ZonedDateTime")
                || className.equals("java.time.OffsetDateTime")
                || className.equals("java.time.Instant");
    }

    /**
     * 格式化日期时间值
     * 将日期时间转换为所有主流数据库都能识别的格式：YYYY-MM-DD HH24:MI:SS
     * 支持 java.util.Date 及其子类（java.sql.Date、java.sql.Timestamp、java.sql.Time）
     */
    private String formatDateTimeValue(Object dateValue) {
        // 使用 SimpleDateFormat 确保输出格式为 YYYY-MM-DD HH:MM:SS
        // Oracle、MySQL、PostgreSQL、SQL Server 等都能识别这种格式
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "'" + sdf.format(dateValue) + "'";
    }

    /**
     * 格式化 Java 8+ 日期时间值
     * 处理 LocalDateTime、LocalDate、LocalTime 等 java.time 包中的类型
     * 这些类型的 toString() 返回 ISO 8601 格式，需要转换为数据库可识别的格式
     */
    private String formatJavaDateTimeValue(Object dateTimeValue) {
        String dateTimeStr = dateTimeValue.toString();
        return formatDateTimeString(dateTimeStr);
    }

    /**
     * 格式化日期时间字符串
     * 处理 JDBC 驱动返回的日期时间字符串（如 ISO 8601 格式、时间戳数字）
     * 转换为所有主流数据库都能识别的格式：YYYY-MM-DD HH:MI:SS
     */
    private String formatDateTimeString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "null";
        }

        try {
            // 1. 判断是否为时间戳数字（毫秒级：13位，秒级：10位）
            if (dateStr.matches("^\\d{13}$")) {
                long timestamp = Long.parseLong(dateStr);
                String formatted = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
                return formatAsDateTimeLiteral(formatted);
            } else if (dateStr.matches("^\\d{10}$")) {
                long timestamp = Long.parseLong(dateStr) * 1000;
                String formatted = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
                return formatAsDateTimeLiteral(formatted);
            }

            // 2. 判断是否为 ISO 8601 格式（包含 T）
            if (dateStr.contains("T")) {
                // 替换 T 为空格，去掉可能的毫秒部分和 Z 后缀
                String normalized = dateStr.replace("T", " ");
                if (normalized.contains(".")) {
                    normalized = normalized.substring(0, normalized.indexOf("."));
                }
                if (normalized.endsWith("Z")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                return formatAsDateTimeLiteral(normalized);
            }

            // 3. 如果已经是标准格式，直接返回
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return formatAsDateTimeLiteral(dateStr);
            }

            // 其他情况，尝试作为字符串直接返回（带转义）
            return "'" + escapeSqlString(dateStr) + "'";
        } catch (Exception e) {
            log.warn("格式化日期字符串失败: {}, 错误: {}", dateStr, e.getMessage());
            return "'" + escapeSqlString(dateStr) + "'";
        }
    }

    /**
     * 将标准日期时间格式化为 SQL 字面量
     * Oracle 使用 TO_DATE 函数，其他数据库使用字符串字面量
     */
    private String formatAsDateTimeLiteral(String dateStr) {
        // Oracle 需要使用 TO_DATE 函数显式转换
        if (getDataBaseType() == DataBaseEnum.ORACLE || getDataBaseType() == DataBaseEnum.DAMENG) {
            return "TO_DATE('" + dateStr + "', 'YYYY-MM-DD HH24:MI:SS')";
        }
        // 其他数据库使用字符串字面量
        return "'" + dateStr + "'";
    }

    /**
     * 格式化 Clob 值（使用标准 JDBC 接口，不依赖具体数据库实现）
     * 支持：Oracle CLOB、PostgreSQL TEXT 等
     */
    private String formatClobValue(Clob clob) {
        try {
            long length = clob.length();
            if (length == 0) {
                return "''";
            }
            // 使用标准 JDBC 接口读取内容
            int len = (int) Math.min(length, Integer.MAX_VALUE);
            String content = clob.getSubString(1, len);
            return "'" + escapeSqlString(content) + "'";
        } catch (SQLException e) {
            log.error("读取 CLOB 失败: {}", e.getMessage());
            return "null";
        }
    }

    /**
     * 格式化 Blob 值（转换为十六进制字符串）
     * 支持：Oracle BLOB、MySQL BLOB 等
     */
    private String formatBlobValue(Blob blob) {
        try {
            long length = blob.length();
            if (length == 0) {
                return "''";
            }
            byte[] bytes = blob.getBytes(1, (int) Math.min(length, Integer.MAX_VALUE));
            return formatBytesValue(bytes);
        } catch (SQLException e) {
            log.error("读取 BLOB 失败: {}", e.getMessage());
            return "null";
        }
    }

    /**
     * 格式化字节数组
     * 转换为十六进制字符串，适用于所有数据库
     */
    private String formatBytesValue(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "''";
        }
        // 转换为十六进制字符串，大部分数据库都支持
        // MySQL: 0x... 或 UNHEX()
        // PostgreSQL: decode('...', 'hex')
        // Oracle: HEXTORAW('...')
        return "0x" + bytesToHex(bytes);
    }

    /**
     * 格式化数组类型
     * 支持：PostgreSQL 数组
     */
    private String formatArrayValue(Array array) {
        try {
            Object[] elements = (Object[]) array.getArray();
            if (elements == null || elements.length == 0) {
                return "''";
            }
            // 转换为 JSON 数组格式字符串
            StringBuilder sb = new StringBuilder("'");
            for (int i = 0; i < elements.length; i++) {
                if (i > 0) sb.append(",");
                if (elements[i] instanceof String) {
                    sb.append("'").append(escapeSqlString(elements[i].toString())).append("'");
                } else {
                    sb.append(elements[i] != null ? elements[i].toString() : "null");
                }
            }
            sb.append("'");
            return sb.toString();
        } catch (SQLException e) {
            log.error("读取数组失败: {}", e.getMessage());
            return "null";
        }
    }

    /**
     * 格式化字符串值（转义单引号）
     */
    private String formatStringValue(String value) {
        return "'" + escapeSqlString(value) + "'";
    }

    /**
     * 格式化 BigDecimal（避免科学计数法）
     */
    private String formatBigDecimal(BigDecimal value) {
        return value.toPlainString();
    }

    /**
     * 格式化其他对象类型
     * 支持：PostgreSQL PGobject、Oracle STRUCT 等
     */
    private String formatObjectValue(Object obj, Class<?> clazz) {
        String className = clazz.getName();

        // PostgreSQL PGobject (JSON、UUID、几何类型等)
        if (className.contains("PGobject")) {
            try {
                Object value = clazz.getMethod("getValue").invoke(obj);
                return "'" + escapeSqlString(String.valueOf(value)) + "'";
            } catch (Exception e) {
                log.warn("处理 PGobject 失败: {}", e.getMessage());
            }
        }

        // Oracle/PostgreSQL STRUCT (结构化类型)
        if (obj instanceof Struct) {
            try {
                Object[] attrs = ((Struct) obj).getAttributes();
                return "'" + escapeSqlString(Arrays.toString(attrs)) + "'";
            } catch (SQLException e) {
                log.error("读取 STRUCT 失败: {}", e.getMessage());
                return "null";
            }
        }

        // 其他对象尝试 toString()
        return "'" + escapeSqlString(obj.toString()) + "'";
    }

    /**
     * 转义 SQL 字符串中的单引号
     * SQL 标准转义方式：两个单引号表示一个单引号
     */
    protected String escapeSqlString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String makeSqlDeleteRow(final String tableName, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final Map dataRecord) {
        List<String> dataKeyValueList = makeSqlKeyColumnSelect(keyColumns, tableColumns, dataRecord);
        if (CollectionUtils.isEmpty(dataKeyValueList)) {
            return null;
        }

        StringBuffer selectSql = new StringBuffer();
        selectSql.append("delete from ");
        selectSql.append(tableName.toLowerCase());
        selectSql.append(" where ");
        selectSql.append(dataKeyValueList.stream().collect(Collectors.joining(" and ")));
        return selectSql.toString();
    }

    public abstract String makeSqlComment(String tableName, String columnName, String comment);
    public abstract String makeSqlComment(String comment);
    public String makeSqlKeyColumn(String tableName, String pkName, String pkColumns) {
        DataBaseEnum dbType = getDataBaseType();

        // Oracle 和 SQL Server 必须指定约束名称
        if (dbType == DataBaseEnum.ORACLE || dbType == DataBaseEnum.DAMENG
                || dbType == DataBaseEnum.SQLSERVER) {
            // 处理 MySQL 的特殊主键名 "PRIMARY"，它是保留字，不能用作约束名
            String safePkName;
            if (pkName == null || pkName.isEmpty() || "PRIMARY".equalsIgnoreCase(pkName)) {
                safePkName = "pk_" + tableName;
            } else {
                safePkName = pkName;
            }
            return "alter table " + tableName + " add constraint " + safePkName + " primary key (" + pkColumns + ")";
        }

        // MySQL/PostgreSQL 约束名称可选
        return "alter table " + tableName + " add primary key (" + pkColumns + ")";
    }

    public String makeSqlIndexColumn(String tableName, String indexName, String indexColumns) {
        return "create index " + indexName + " on " + tableName + "(" + indexColumns + ")";
    }

    @Override
    public String makeSqlForeignKey(String tableName, String fkName, String fkColumns,
                                    String pkTableName, String pkColumns,
                                    String onDelete, String onUpdate) {
        StringBuilder sql = new StringBuilder();
        sql.append("alter table ").append(tableName);
        sql.append(" add constraint ").append(fkName);
        sql.append(" foreign key (").append(fkColumns).append(")");
        sql.append(" references ").append(pkTableName).append("(").append(pkColumns).append(")");

        // 添加删除规则
        if (!StringUtils.isEmpty(onDelete)) {
            sql.append(" on delete ").append(onDelete);
        }

        // 添加更新规则（Oracle/SQL Server 不支持 ON UPDATE）
        if (!StringUtils.isEmpty(onUpdate)) {
            DataBaseEnum dbType = getDataBaseType();
            if (dbType != DataBaseEnum.ORACLE && dbType != DataBaseEnum.DAMENG
                    && dbType != DataBaseEnum.SQLSERVER) {
                sql.append(" on update ").append(onUpdate);
            }
        }

        return sql.toString();
    }

    public String makeSqlUpdateRow(final String tableName, final List<String> keyColumns, final List<TableColumnEntity> tableColumns, final Map dataRecordBefore, final Map dataRecordAfter) {
        StringBuffer updateSql = new StringBuffer();
        updateSql.append(" update ");
        updateSql.append(tableName.toLowerCase());
        updateSql.append(" set ");
        updateSql.append(makeSqlUpdateColumnValueAfter(tableColumns, dataRecordAfter));
        updateSql.append(" where ");
        List<String> dataKeyValueList = makeSqlKeyColumnSelect(keyColumns, tableColumns, dataRecordBefore);
        if (CollectionUtils.isEmpty(dataKeyValueList)) {
            return null;
        }
        updateSql.append(dataKeyValueList.stream().collect(Collectors.joining(" and ")));
        return updateSql.toString();
    }

    private String makeSqlUpdateColumnValueAfter(List<TableColumnEntity> tableColumns, Map dataRow) {
        List<String> dataColumnList = new ArrayList<>();
        for (TableColumnEntity iterator : tableColumns) {
            String columnValueTmp = iterator.getColumnName() + " = ";
            Object columnValue = dataRow.get(iterator.getColumnName());
            if (columnValue != null) {
                columnValueTmp += formatColumnValueForInsert(iterator, columnValue);
            } else {
                columnValueTmp += "null";
            }
            dataColumnList.add(columnValueTmp);
        }
        return " " + dataColumnList.stream().collect(Collectors.joining(",")) + " ";
    }
}
