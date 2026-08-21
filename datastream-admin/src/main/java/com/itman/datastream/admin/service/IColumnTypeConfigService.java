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

import com.itman.datastream.common.entity.ColumnTypeDefineEntity;
import com.itman.datastream.common.entity.ColumnTypeMapEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

/**
 * 字段类型定义与映射配置服务。
 */
public interface IColumnTypeConfigService {

    // ---- 类型定义 ----

    Integer getTypeDefineCount(Integer queryFlag, String queryValue) throws DataStreamException;

    List<ColumnTypeDefineEntity> queryTypeDefineByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException;

    List<ColumnTypeDefineEntity> queryAllTypeDefine() throws DataStreamException;

    ColumnTypeDefineEntity getTypeDefineById(Long columnTypeDefineId) throws DataStreamException;

    Long addTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException;

    void modifyTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException;

    void delTypeDefine(Long columnTypeDefineId) throws DataStreamException;

    // ---- 类型映射 ----

    Integer getTypeMapCount(Integer queryFlag, String queryValue) throws DataStreamException;

    List<ColumnTypeMapEntity> queryTypeMapByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException;

    ColumnTypeMapEntity getTypeMapById(Long columnTypeMapId) throws DataStreamException;

    Long addTypeMap(ColumnTypeMapEntity map) throws DataStreamException;

    void modifyTypeMap(ColumnTypeMapEntity map) throws DataStreamException;

    void delTypeMap(Long columnTypeMapId) throws DataStreamException;
}