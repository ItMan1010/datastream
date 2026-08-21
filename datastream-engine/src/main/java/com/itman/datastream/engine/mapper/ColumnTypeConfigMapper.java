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
package com.itman.datastream.engine.mapper;

import com.itman.datastream.common.entity.ColumnTypeDefineEntity;
import com.itman.datastream.common.entity.ColumnTypeMapEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * 字段类型定义与映射配置 Mapper。
 * 为管理端「字段类型配置」页面提供两表的 CRUD 与保护校验所需查询。
 */
@Mapper
public interface ColumnTypeConfigMapper {

    // ---- 类型定义 data_stream_column_type_define ----

    List<ColumnTypeDefineEntity> selectTypeDefineList(@Param("queryFlag") Integer queryFlag,
                                                      @Param("queryValue") String queryValue,
                                                      @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    Integer selectTypeDefineListCount(@Param("queryFlag") Integer queryFlag,
                                      @Param("queryValue") String queryValue) throws DataAccessException;

    List<ColumnTypeDefineEntity> selectTypeDefineById(@Param("columnTypeDefineId") Long columnTypeDefineId) throws DataAccessException;

    Integer selectTypeDefineByDbTypeAndName(@Param("databaseType") String databaseType,
                                            @Param("columnTypeName") String columnTypeName,
                                            @Param("excludeId") Long excludeId) throws DataAccessException;

    Integer insertTypeDefine(@Param("define") ColumnTypeDefineEntity define) throws DataAccessException;

    Integer updateTypeDefine(@Param("define") ColumnTypeDefineEntity define) throws DataAccessException;

    Integer deleteTypeDefine(@Param("columnTypeDefineId") Long columnTypeDefineId) throws DataAccessException;

    Integer countTypeDefineReferenced(@Param("columnTypeDefineId") Long columnTypeDefineId) throws DataAccessException;

    // ---- 类型映射 data_stream_column_type_map ----

    List<ColumnTypeMapEntity> selectTypeMapList(@Param("queryFlag") Integer queryFlag,
                                                @Param("queryValue") String queryValue,
                                                @Param("sqlLimit") String sqlLimit) throws DataAccessException;

    Integer selectTypeMapListCount(@Param("queryFlag") Integer queryFlag,
                                   @Param("queryValue") String queryValue) throws DataAccessException;

    List<ColumnTypeMapEntity> selectTypeMapById(@Param("columnTypeMapId") Long columnTypeMapId) throws DataAccessException;

    Integer insertTypeMap(@Param("map") ColumnTypeMapEntity map) throws DataAccessException;

    Integer updateTypeMap(@Param("map") ColumnTypeMapEntity map) throws DataAccessException;

    Integer deleteTypeMap(@Param("columnTypeMapId") Long columnTypeMapId) throws DataAccessException;
}