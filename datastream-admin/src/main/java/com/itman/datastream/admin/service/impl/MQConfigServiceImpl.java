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

import com.itman.datastream.admin.service.IMQConfigService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.MQConfigEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.MQConfigDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;


@Slf4j
@Service
@RequiredArgsConstructor
public class MQConfigServiceImpl implements IMQConfigService {
    private final MQConfigDao MQConfigDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;


    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer getConfigCount(Integer queryFlag, String queryValue) throws DataStreamException {
        return MQConfigDao.getMQConfigCountById(queryFlag, queryValue);
    }

    public List<MQConfigEntity> queryConfigByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        queryValue = queryFlag.equals(MQ_CONFIG_QUERY_FLAG_FILE_NAME) ? "%" + queryValue + "%" : queryValue;
        return MQConfigDao.selectMQConfigByPage(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }

    @Override
    public MQConfigEntity getConfigById(Long mqConfigId) throws DataStreamException {
        try {
            List<MQConfigEntity> entityList = MQConfigDao.queryMQConfigById(mqConfigId);
            return entityList.isEmpty() ? null : entityList.get(0);
        } catch (Exception e) {
            log.error("查询MQ配置详情失败", e);
            throw new DataStreamException("MQ_DB_003", "查询MQ配置详情失败：" + e.getMessage());
        }
    }

    private IDatabaseAdapter geMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Long insertConfig(MQConfigEntity mqConfig) throws DataStreamException {
        mqConfig.setMqConfigId(dataStreamDao.querySequence(SEQ_MQ_CONFIG_ID));
        mqConfig.setOnLineFlag(COMMON_STATE_OFFLINE);
        mqConfig.setState(COMMON_STATE_OFFLINE);
        MQConfigDao.insertMQConfig(mqConfig, geMetaDbObject().makeSqlSystemDate());
        return mqConfig.getMqConfigId();
    }

    @Override
    public void updateConfig(MQConfigEntity mqConfig) throws DataStreamException {
        try {
            MQConfigDao.updateMQConfig(mqConfig, geMetaDbObject().makeSqlSystemDate());
        } catch (Exception e) {
            log.error("修改MQ配置失败", e);
            throw new DataStreamException("MQ_DB_005", "修改MQ配置失败：" + e.getMessage());
        }
    }

    @Override
    public void deleteConfig(Long mqConfigId) throws DataStreamException {
        try {
            MQConfigDao.deleteMQConfig(mqConfigId);
        } catch (Exception e) {
            log.error("删除MQ配置失败", e);
            throw new DataStreamException("MQ_DB_006", "删除MQ配置失败：" + e.getMessage());
        }
    }

    @Override
    public void updateConfigOnLineFlagById(Long mqConfigId, Integer onLineFlag) throws DataStreamException {
        MQConfigDao.updateMQConfigOnLineFlagById(mqConfigId, onLineFlag);
    }
}

