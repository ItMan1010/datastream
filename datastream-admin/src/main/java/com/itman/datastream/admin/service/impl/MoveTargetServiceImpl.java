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


import cn.hutool.extra.spring.SpringUtil;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.admin.service.IMoveTargetService;
import com.itman.datastream.common.entity.DataMoveInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.entity.TableColumnEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.DATA_SOURCE_TYPE_ORACLE;

@Service
@RequiredArgsConstructor
public class MoveTargetServiceImpl implements IMoveTargetService {
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;

    @Override
    public Integer insertDataList(Long dataSourceId, Long infoId, Integer dataCount, String pageRowEnd, String insertSql) throws DataStreamException {
        return dataStreamDao.insertDataListByTransaction(infoId, dataCount, pageRowEnd, insertSql);
    }

    @Override
    public Integer insertDataListBindVar(Long dataSourceId, String insertSqlColumns, List<List<Object>> dataListTarget) throws DataStreamException {
        return dataStreamDao.insertDataListBindVar(insertSqlColumns, dataListTarget);
    }

    @Override
    public Long executeSelectRecordCountSql(Long dataSourceId, String selectSql) throws DataStreamException {
        return dataStreamDao.executeSelectRecordCountSql(selectSql);
    }

    @Override
    public List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException {
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).getTableColumns(dataSource.getDataBaseType(), dataSource.getSchemaName(), dataSource.getUserName(), tableName);
    }

    @Override
    public long getTableRecordCount(Long dataSourceId, String selectSql) throws DataStreamException {
        return SpringUtil.getBean(IMoveTargetService.class).executeSelectRecordCountSql(dataSourceId, selectSql);
    }

    private IDatabaseAdapter getDataBaseObject(Integer dataSourceType) throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataSourceType);
    }


    @Override
    public Integer insertDataMoveInfo(Long dataSourceId, Integer dataSourceType, List<DataMoveInfoEntity> dataMoveInfoList) throws DataStreamException {
        return (!dataSourceType.equals(DATA_SOURCE_TYPE_ORACLE)) ? dataStreamDao.insertDataMoveInfo(getDataBaseObject(dataSourceType).makeSqlSystemDate(), dataMoveInfoList) : dataStreamDao.insertDataMoveInfoByTransaction(getDataBaseObject(dataSourceType).makeSqlSystemDate(), dataMoveInfoList);
    }

    @Override
    public List<DataMoveInfoEntity> queryDataMoveInfoByInfoId(Long dataSourceId, Long infoId) throws DataStreamException {
        return dataStreamDao.queryDataMoveInfoByInfoId(infoId);
    }

    @Override
    public Integer executeMetaDbSql(Long dataSourceId, String metaDbSql) throws DataStreamException {
        return dataStreamDao.executeMetaDbSql(metaDbSql);
    }

    @Override
    public Integer deleteDataList(Long dataSourceId, List<String> dataDeleteSqlList) throws DataStreamException {
        Integer deleteCount = 0;
        for (String dataIterator : dataDeleteSqlList) {
            if (dataStreamDao.executeDeleteRecordSql(dataIterator) == 1) {
                deleteCount++;
            }
        }
        return deleteCount;
    }
}
