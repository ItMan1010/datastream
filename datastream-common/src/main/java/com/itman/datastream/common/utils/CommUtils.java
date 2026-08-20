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
package com.itman.datastream.common.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.itman.datastream.common.constant.DataBaseEnum;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.itman.datastream.common.constant.DataStreamConstant.*;

public class CommUtils {
    public static final Map<Integer, String> jdbcUrlDataBaseTypeMap = new HashMap<>();
    public static final Map<Integer, Integer> dataSourceCategoryMap = new HashMap<>();
    //应用依赖元数据基本类型映射
    public static final Map<String, Integer> metaDataBaseTypeMapped = new HashMap<>();


    static {
        //应用依赖元数据基本类型映射，目前只支持四种
        metaDataBaseTypeMapped.put("mysql", DATA_SOURCE_TYPE_MYSQL);
        metaDataBaseTypeMapped.put("oracle", DATA_SOURCE_TYPE_ORACLE);
        metaDataBaseTypeMapped.put("postgresql", DATA_SOURCE_TYPE_PG);
        metaDataBaseTypeMapped.put("h2", DATA_SOURCE_TYPE_H2);

        /**
         * 数据库jdbc url的数据库sql类型
         */
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_SHARDING, "mysql");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_MYSQL, "mysql");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_ORACLE, "oracle");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_PG, "postgresql");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_DORIS, "mysql");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_MEM, "mysql");
        jdbcUrlDataBaseTypeMap.put(DATA_SOURCE_TYPE_H2, "h2");

        //todo 以后可以设计成可配置
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_SHARDING, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_MYSQL, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_ORACLE, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_PG, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_DORIS, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_MEM, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_H2, DATA_SOURCE_CATEGORY_DATABASE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_TEXT, DATA_SOURCE_CATEGORY_FILE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_EXCEL, DATA_SOURCE_CATEGORY_FILE);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_KAFKA, DATA_SOURCE_CATEGORY_MQ);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_ROCKETMQ, DATA_SOURCE_CATEGORY_MQ);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_RABBITMQ, DATA_SOURCE_CATEGORY_MQ);
        dataSourceCategoryMap.put(DATA_SOURCE_TYPE_ACTIVEMQ, DATA_SOURCE_CATEGORY_MQ);

    }

    public static Boolean isDataBaseDataSource(Integer dataSourceType) {
        if (dataSourceCategoryMap.get(dataSourceType) == null) {
            return false;
        }
        return dataSourceCategoryMap.get(dataSourceType).equals(DATA_SOURCE_CATEGORY_DATABASE);
    }

    public static Boolean isFileDataSource(Integer dataSourceType) {
        if (dataSourceCategoryMap.get(dataSourceType) == null) {
            return false;
        }
        return dataSourceCategoryMap.get(dataSourceType).equals(DATA_SOURCE_CATEGORY_FILE);
    }

    public static Boolean isMQDataSource(Integer dataSourceType) {
        if (dataSourceCategoryMap.get(dataSourceType) == null) {
            return false;
        }
        return dataSourceCategoryMap.get(dataSourceType).equals(DATA_SOURCE_CATEGORY_MQ);
    }

    /**
     * 向 JDBC URL 追加查询参数，自动处理分隔符：
     * URL 以 ? 或 & 结尾时直接追加；已含 ? 时补 &；不含 ? 时补 ?；
     * URL 中已存在同名参数（paramName=）时不重复追加。
     *
     * @param url         JDBC URL
     * @param paramName   参数名
     * @param paramValue  参数值
     * @return 追加参数后的 URL；url 为 null 或空串时原样返回
     */
    public static String appendUrlParam(String url, String paramName, String paramValue) {
        if (url == null || url.isEmpty()) {
            return url;
        }

        if (url.contains(paramName + "=")) {
            return url;
        }

        String param = paramName + "=" + paramValue;
        char lastChar = url.charAt(url.length() - 1);
        if (lastChar == '?' || lastChar == '&') {
            return url + param;
        }

        return url + (url.contains("?") ? "&" : "?") + param;
    }

    public static String parseJdbcUrl(String url) {
        for (DataBaseEnum type : DataBaseEnum.values()) {
            Pattern namePattern = Pattern.compile(type.getUrlPattern());
            Matcher dateMatcher = namePattern.matcher(url);
            while (dateMatcher.find()) {
                return dateMatcher.group("type");
            }
        }
        return null;
    }

    public static String parseSchemaNameJdbcUrl(String url) {
        for (DataBaseEnum type : DataBaseEnum.values()) {
            Pattern namePattern = Pattern.compile(type.getUrlPattern());
            Matcher dateMatcher = namePattern.matcher(url);
            while (dateMatcher.find()) {
                return dateMatcher.group("database");
            }
        }
        return null;
    }

    public static String parseHostJdbcUrl(String url) {
        for (DataBaseEnum type : DataBaseEnum.values()) {
            Pattern namePattern = Pattern.compile(type.getUrlPattern());
            Matcher dateMatcher = namePattern.matcher(url);
            while (dateMatcher.find()) {
                return dateMatcher.group("host");
            }
        }
        return null;
    }

    public static String parsePortJdbcUrl(String url) {
        for (DataBaseEnum type : DataBaseEnum.values()) {
            Pattern namePattern = Pattern.compile(type.getUrlPattern());
            Matcher dateMatcher = namePattern.matcher(url);
            while (dateMatcher.find()) {
                return dateMatcher.group("port");
            }
        }
        return null;
    }

    public static Integer getDataBaseType(String url) {
        String metaDbBaseTypeName = parseJdbcUrl(url);
        if (metaDbBaseTypeName != null) {
            return metaDataBaseTypeMapped.get(metaDbBaseTypeName.toLowerCase());
        }
        return null;
    }

    public static Integer genPageRow(Integer page, Integer count) {
        if (page == null) {
            return null;
        } else {
            return (page - 1) * count;
        }
    }

    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    public static String timestampGenerator() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = now.format(formatter);
        return timestamp;
    }

    public static String getSplitFlag(Integer splitFlagId) {
        switch (splitFlagId) {
            case SPLIT_FLAG_VERTICAL_LINE:
                return "|";
            case SPLIT_FLAG_COMMA:
                return ",";
            case SPLIT_FLAG_AND:
                return "&";
            default:
                break;
        }
        return null;
    }


    public static String mapToJsonByJacksonStreaming(Map<?, ?> map) throws Exception {
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createGenerator(stringWriter);

        // 手动开始JSON对象
        jsonGenerator.writeStartObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().toString();
            Object value = entry.getValue();
            // 手动写入键值对
            jsonGenerator.writeFieldName(key);
            jsonGenerator.writeObject(value); // 自动适配基本类型/对象类型
        }
        // 手动结束JSON对象
        jsonGenerator.writeEndObject();

        jsonGenerator.close();
        return stringWriter.toString();
    }

    private static final Random random = new Random();

    public static long generateUniqueLong() {
        long timestamp = System.currentTimeMillis();
        int randomInt = random.nextInt(99999);
        return timestamp * 100000 + randomInt;
    }

    /**
     * 规范化类型名称，去掉精度参数
     * 例如: TIMESTAMP(6) -> TIMESTAMP, VARCHAR2(100) -> VARCHAR2
     */
    public static String normalizeTypeName(String typeName) {
        if (StringUtils.isEmpty(typeName)) {
            return typeName;
        }
        // 去掉括号及后面的内容
        int parenIndex = typeName.indexOf('(');
        return parenIndex > 0 ? typeName.substring(0, parenIndex).trim() : typeName.trim();
    }

}
