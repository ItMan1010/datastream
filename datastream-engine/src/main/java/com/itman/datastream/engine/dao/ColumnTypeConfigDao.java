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
package com.itman.datastream.engine.dao;

import com.itman.datastream.common.entity.ColumnTypeDefineEntity;
import com.itman.datastream.common.entity.ColumnTypeMapEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.mapper.ColumnTypeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字段类型定义与映射配置 DAO。
 * 统一异常为 {@link DataStreamException}，供管理端配置服务调用。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ColumnTypeConfigDao {

    public final ColumnTypeConfigMapper columnTypeConfigMapper;

    // ---- 类型定义 ----

    public List<ColumnTypeDefineEntity> selectTypeDefineList(Integer queryFlag, String queryValue, String sqlLimit) throws DataStreamException {
        try {
            return columnTypeConfigMapper.selectTypeDefineList(queryFlag, queryValue, sqlLimit);
        } catch (Exception e) {
            log.error("查询类型定义列表失败", e);
            throw new DataStreamException("CT_DEFINE_DB_001", "查询类型定义列表失败：" + e.getMessage());
        }
    }

    public Integer selectTypeDefineListCount(Integer queryFlag, String queryValue) throws DataStreamException {
        try {
            return columnTypeConfigMapper.selectTypeDefineListCount(queryFlag, queryValue);
        } catch (Exception e) {
            log.error("查询类型定义总数失败", e);
            throw new DataStreamException("CT_DEFINE_DB_002", "查询类型定义总数失败：" + e.getMessage());
        }
    }

    public ColumnTypeDefineEntity selectTypeDefineById(Long columnTypeDefineId) throws DataStreamException {
        try {
            List<ColumnTypeDefineEntity> list = columnTypeConfigMapper.selectTypeDefineById(columnTypeDefineId);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.error("查询类型定义详情失败", e);
            throw new DataStreamException("CT_DEFINE_DB_003", "查询类型定义详情失败：" + e.getMessage());
        }
    }

    public Integer selectTypeDefineByDbTypeAndName(String databaseType, String columnTypeName, Long excludeId) throws DataStreamException {
        try {
            return columnTypeConfigMapper.selectTypeDefineByDbTypeAndName(databaseType, columnTypeName, excludeId);
        } catch (Exception e) {
            log.error("校验类型定义唯一性失败", e);
            throw new DataStreamException("CT_DEFINE_DB_004", "校验类型定义唯一性失败：" + e.getMessage());
        }
    }

    public Integer insertTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException {
        try {
            return columnTypeConfigMapper.insertTypeDefine(define);
        } catch (Exception e) {
            log.error("新增类型定义失败", e);
            throw new DataStreamException("CT_DEFINE_DB_005", "新增类型定义失败：" + e.getMessage());
        }
    }

    public Integer updateTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException {
        try {
            return columnTypeConfigMapper.updateTypeDefine(define);
        } catch (Exception e) {
            log.error("修改类型定义失败", e);
            throw new DataStreamException("CT_DEFINE_DB_006", "修改类型定义失败：" + e.getMessage());
        }
    }

    public Integer deleteTypeDefine(Long columnTypeDefineId) throws DataStreamException {
        try {
            return columnTypeConfigMapper.deleteTypeDefine(columnTypeDefineId);
        } catch (Exception e) {
            log.error("删除类型定义失败", e);
            throw new DataStreamException("CT_DEFINE_DB_007", "删除类型定义失败：" + e.getMessage());
        }
    }

    public Integer countTypeDefineReferenced(Long columnTypeDefineId) throws DataStreamException {
        try {
            return columnTypeConfigMapper.countTypeDefineReferenced(columnTypeDefineId);
        } catch (Exception e) {
            log.error("统计类型定义引用失败", e);
            throw new DataStreamException("CT_DEFINE_DB_008", "统计类型定义引用失败：" + e.getMessage());
        }
    }

    // ---- 类型映射 ----

    public List<ColumnTypeMapEntity> selectTypeMapList(Integer queryFlag, String queryValue, String sqlLimit) throws DataStreamException {
        try {
            return columnTypeConfigMapper.selectTypeMapList(queryFlag, queryValue, sqlLimit);
        } catch (Exception e) {
            log.error("查询类型映射列表失败", e);
            throw new DataStreamException("CT_MAP_DB_001", "查询类型映射列表失败：" + e.getMessage());
        }
    }

    public Integer selectTypeMapListCount(Integer queryFlag, String queryValue) throws DataStreamException {
        try {
            return columnTypeConfigMapper.selectTypeMapListCount(queryFlag, queryValue);
        } catch (Exception e) {
            log.error("查询类型映射总数失败", e);
            throw new DataStreamException("CT_MAP_DB_002", "查询类型映射总数失败：" + e.getMessage());
        }
    }

    public ColumnTypeMapEntity selectTypeMapById(Long columnTypeMapId) throws DataStreamException {
        try {
            List<ColumnTypeMapEntity> list = columnTypeConfigMapper.selectTypeMapById(columnTypeMapId);
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            log.error("查询类型映射详情失败", e);
            throw new DataStreamException("CT_MAP_DB_003", "查询类型映射详情失败：" + e.getMessage());
        }
    }

    public Integer insertTypeMap(ColumnTypeMapEntity map) throws DataStreamException {
        try {
            return columnTypeConfigMapper.insertTypeMap(map);
        } catch (Exception e) {
            log.error("新增类型映射失败", e);
            throw new DataStreamException("CT_MAP_DB_004", "新增类型映射失败：" + e.getMessage());
        }
    }

    public Integer updateTypeMap(ColumnTypeMapEntity map) throws DataStreamException {
        try {
            return columnTypeConfigMapper.updateTypeMap(map);
        } catch (Exception e) {
            log.error("修改类型映射失败", e);
            throw new DataStreamException("CT_MAP_DB_005", "修改类型映射失败：" + e.getMessage());
        }
    }

    public Integer deleteTypeMap(Long columnTypeMapId) throws DataStreamException {
        try {
            return columnTypeConfigMapper.deleteTypeMap(columnTypeMapId);
        } catch (Exception e) {
            log.error("删除类型映射失败", e);
            throw new DataStreamException("CT_MAP_DB_006", "删除类型映射失败：" + e.getMessage());
        }
    }
}