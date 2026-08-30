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

import com.itman.datastream.admin.service.IColumnTypeConfigService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.ColumnTypeBuiltInConstant;
import com.itman.datastream.common.entity.ColumnTypeDefineEntity;
import com.itman.datastream.common.entity.ColumnTypeMapEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.ColumnTypeConfigDao;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.COLUMN_TYPE_CLASSIFY_DATETIME;
import static com.itman.datastream.common.constant.DataStreamConstant.COLUMN_TYPE_CLASSIFY_NONE;
import static com.itman.datastream.common.constant.DataStreamConstant.COLUMN_TYPE_CLASSIFY_NUMERIC;
import static com.itman.datastream.common.constant.DataStreamConstant.COLUMN_TYPE_CLASSIFY_STRING;
import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_COLUMN_TYPE_DEFINE_ID;
import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_COLUMN_TYPE_MAP_ID;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;

/**
 * 字段类型定义与映射配置服务实现。
 * 包含分页/详情/新增/编辑/删除，以及下列保护约束：
 * <ul>
 *     <li>类型定义唯一性（数据库类型 + 类型名称）校验；</li>
 *     <li>禁止删除被映射引用或库内置的类型定义；</li>
 *     <li>禁止删除库内置的类型映射；</li>
 *     <li>映射新增/编辑校验源、目标类型定义存在。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnTypeConfigServiceImpl implements IColumnTypeConfigService {

    private final ColumnTypeConfigDao columnTypeConfigDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;
    private final PermissionService permissionService;

    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    // ==================== 类型定义 ====================

    @Override
    public Integer getTypeDefineCount(Integer queryFlag, String queryValue) throws DataStreamException {
        return columnTypeConfigDao.selectTypeDefineListCount(queryFlag, queryValue);
    }

    @Override
    public List<ColumnTypeDefineEntity> queryTypeDefineByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        return columnTypeConfigDao.selectTypeDefineList(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }

    @Override
    public List<ColumnTypeDefineEntity> queryAllTypeDefine() throws DataStreamException {
        return columnTypeConfigDao.selectTypeDefineList(1, null, null);
    }

    @Override
    public ColumnTypeDefineEntity getTypeDefineById(Long columnTypeDefineId) throws DataStreamException {
        return columnTypeConfigDao.selectTypeDefineById(columnTypeDefineId);
    }

    @Override
    public Long addTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException {
        validateDefineRequired(define);
        checkDefineUnique(define.getDatabaseType(), define.getColumnTypeName(), null);
        define.setColumnTypeClassify(deriveClassifyFromCategory(define.getTypeCategory()));
        define.setColumnTypeDefineId(dataStreamDao.querySequence(SEQ_COLUMN_TYPE_DEFINE_ID));
        define.setSystemUserCode(permissionService.getCurrentUserCode());
        columnTypeConfigDao.insertTypeDefine(define);
        return define.getColumnTypeDefineId();
    }

    @Override
    public void modifyTypeDefine(ColumnTypeDefineEntity define) throws DataStreamException {
        if (define.getColumnTypeDefineId() == null) {
            throw new DataStreamException("CT_DEFINE_001", "类型定义ID不能为空");
        }
        validateDefineRequired(define);
        checkDefineUnique(define.getDatabaseType(), define.getColumnTypeName(), define.getColumnTypeDefineId());
        define.setColumnTypeClassify(deriveClassifyFromCategory(define.getTypeCategory()));
        columnTypeConfigDao.updateTypeDefine(define);
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void delTypeDefine(Long columnTypeDefineId) throws DataStreamException {
        if (columnTypeDefineId == null) {
            throw new DataStreamException("CT_DEFINE_002", "类型定义ID不能为空");
        }
        if (ColumnTypeBuiltInConstant.BUILTIN_TYPE_DEFINE_IDS.contains(columnTypeDefineId)) {
            throw new DataStreamException("CT_DEFINE_003", "库内置类型不可删除，只能修改");
        }
        Integer refCount = columnTypeConfigDao.countTypeDefineReferenced(columnTypeDefineId);
        if (refCount != null && refCount > 0) {
            throw new DataStreamException("CT_DEFINE_004", "该类型定义仍被 " + refCount + " 条类型映射引用，不能删除");
        }
        columnTypeConfigDao.deleteTypeDefine(columnTypeDefineId);
    }

    // ==================== 类型映射 ====================

    @Override
    public Integer getTypeMapCount(Integer queryFlag, String queryValue) throws DataStreamException {
        return columnTypeConfigDao.selectTypeMapListCount(queryFlag, queryValue);
    }

    @Override
    public List<ColumnTypeMapEntity> queryTypeMapByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        return columnTypeConfigDao.selectTypeMapList(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }

    @Override
    public ColumnTypeMapEntity getTypeMapById(Long columnTypeMapId) throws DataStreamException {
        return columnTypeConfigDao.selectTypeMapById(columnTypeMapId);
    }

    @Override
    public Long addTypeMap(ColumnTypeMapEntity map) throws DataStreamException {
        validateMap(map);
        map.setColumnTypeMapId(dataStreamDao.querySequence(SEQ_COLUMN_TYPE_MAP_ID));
        map.setSystemUserCode(permissionService.getCurrentUserCode());
        columnTypeConfigDao.insertTypeMap(map);
        return map.getColumnTypeMapId();
    }

    @Override
    public void modifyTypeMap(ColumnTypeMapEntity map) throws DataStreamException {
        if (map.getColumnTypeMapId() == null) {
            throw new DataStreamException("CT_MAP_001", "类型映射ID不能为空");
        }
        validateMap(map);
        columnTypeConfigDao.updateTypeMap(map);
    }

    @Override
    public void delTypeMap(Long columnTypeMapId) throws DataStreamException {
        if (columnTypeMapId == null) {
            throw new DataStreamException("CT_MAP_002", "类型映射ID不能为空");
        }
        if (ColumnTypeBuiltInConstant.BUILTIN_TYPE_MAP_IDS.contains(columnTypeMapId)) {
            throw new DataStreamException("CT_MAP_003", "库内置类型映射不可删除");
        }
        columnTypeConfigDao.deleteTypeMap(columnTypeMapId);
    }

    // ==================== 校验工具 ====================

    private void validateDefineRequired(ColumnTypeDefineEntity define) throws DataStreamException {
        if (define == null) {
            throw new DataStreamException("CT_DEFINE_005", "类型定义不能为空");
        }
        if (StringUtils.isEmpty(define.getDatabaseType())) {
            throw new DataStreamException("CT_DEFINE_006", "数据库类型不能为空");
        }
        if (StringUtils.isEmpty(define.getColumnTypeName())) {
            throw new DataStreamException("CT_DEFINE_007", "类型名称不能为空");
        }
        if (StringUtils.isEmpty(define.getTypeCategory())) {
            throw new DataStreamException("CT_DEFINE_008", "类型分类不能为空");
        }
    }

    private void checkDefineUnique(String databaseType, String columnTypeName, Long excludeId) throws DataStreamException {
        Integer count = columnTypeConfigDao.selectTypeDefineByDbTypeAndName(databaseType, columnTypeName, excludeId);
        if (count != null && count > 0) {
            throw new DataStreamException("CT_DEFINE_009", "数据库类型【" + databaseType + "】下已存在类型【" + columnTypeName + "】");
        }
    }

    /**
     * 由新分类 {@link ColumnTypeDefineEntity#typeCategory} 推导旧的数值分类
     * {@link ColumnTypeDefineEntity#columnTypeClassify}，用于回填表中仍为 NOT NULL 的
     * {@code column_type_classify} 列。
     */
    private Integer deriveClassifyFromCategory(String typeCategory) {
        if (typeCategory == null) {
            return COLUMN_TYPE_CLASSIFY_NONE;
        }
        if (typeCategory.startsWith("NUMERIC") || "BOOLEAN".equals(typeCategory)) {
            return COLUMN_TYPE_CLASSIFY_NUMERIC;
        }
        if (typeCategory.startsWith("STRING")) {
            return COLUMN_TYPE_CLASSIFY_STRING;
        }
        if (typeCategory.startsWith("DATETIME")) {
            return COLUMN_TYPE_CLASSIFY_DATETIME;
        }
        return COLUMN_TYPE_CLASSIFY_NONE;
    }

    private void validateMap(ColumnTypeMapEntity map) throws DataStreamException {
        if (map == null) {
            throw new DataStreamException("CT_MAP_004", "类型映射不能为空");
        }
        if (map.getColumnTypeDefineIdA() == null) {
            throw new DataStreamException("CT_MAP_005", "源类型定义不能为空");
        }
        if (map.getColumnTypeDefineIdB() == null) {
            throw new DataStreamException("CT_MAP_006", "目标类型定义不能为空");
        }
        if (map.getMatchLevel() == null) {
            throw new DataStreamException("CT_MAP_007", "匹配级别不能为空");
        }
        ColumnTypeDefineEntity defineA = columnTypeConfigDao.selectTypeDefineById(map.getColumnTypeDefineIdA());
        if (defineA == null) {
            throw new DataStreamException("CT_MAP_008", "源类型定义【" + map.getColumnTypeDefineIdA() + "】不存在");
        }
        ColumnTypeDefineEntity defineB = columnTypeConfigDao.selectTypeDefineById(map.getColumnTypeDefineIdB());
        if (defineB == null) {
            throw new DataStreamException("CT_MAP_009", "目标类型定义【" + map.getColumnTypeDefineIdB() + "】不存在");
        }
        // 非精确匹配时由前端基于 match_level 提示转换警告，这里仅保证引用的类型定义存在
    }
}