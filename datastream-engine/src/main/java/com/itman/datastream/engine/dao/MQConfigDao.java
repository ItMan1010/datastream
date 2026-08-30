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

import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.mapper.MQConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;


@Slf4j
@Repository
@RequiredArgsConstructor
public class MQConfigDao {
    public final MQConfigMapper MQConfigMapper;


    public Integer getMQConfigCountById(Integer queryFlag, String queryValue, String systemUserCode) throws DataStreamException {
        return MQConfigMapper.getMQConfigCountById(queryFlag, queryValue, systemUserCode);
    }

    public List<MQConfigEntity> selectMQConfigByPage(Integer queryFlag, String queryValue, String sqlLimit, String systemUserCode) throws DataStreamException {
        return MQConfigMapper.selectMQConfigByPage(queryFlag, queryValue, sqlLimit, systemUserCode);
    }

    public List<MQConfigEntity> queryMQConfigById(Long mqConfigId) throws DataStreamException {
        return MQConfigMapper.queryMQConfigById(mqConfigId);
    }


    public Integer insertMQConfig(MQConfigEntity mqConfig, String sysdate) {
        return MQConfigMapper.insertMQConfig(mqConfig, sysdate);
    }

    public Integer updateMQConfig(MQConfigEntity mqConfig, String sysdate) {
        return MQConfigMapper.updateMQConfig(mqConfig, sysdate);
    }

    public Integer deleteMQConfig(Long mqConfigId) {
        return MQConfigMapper.deleteMQConfig(mqConfigId);
    }

    public Integer updateMQConfigOnLineFlagById(Long mqConfigId, Integer onLineFlag) {
        return MQConfigMapper.updateMQConfigOnLineFlagById(mqConfigId, onLineFlag);
    }
}
