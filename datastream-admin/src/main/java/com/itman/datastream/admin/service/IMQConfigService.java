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

import com.itman.datastream.common.entity.MQConfigEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

public interface IMQConfigService {

    Integer getConfigCount(Integer queryFlag, String queryValue) throws DataStreamException;


    List<MQConfigEntity> queryConfigByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException;


    MQConfigEntity getConfigById(Long mqConfigId) throws DataStreamException;


    Long insertConfig(MQConfigEntity mqConfig) throws DataStreamException;


    void updateConfig(MQConfigEntity mqConfig) throws DataStreamException;

    void deleteConfig(Long mqConfigId) throws DataStreamException;

    void updateConfigOnLineFlagById(Long mqConfigId, Integer onLineFlag) throws DataStreamException;
}

