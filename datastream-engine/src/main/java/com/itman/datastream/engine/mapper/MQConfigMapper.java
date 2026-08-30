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

import com.itman.datastream.common.entity.MQConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;


@Mapper
public interface MQConfigMapper {
    Integer getMQConfigCountById(@Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<MQConfigEntity> selectMQConfigByPage(@Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("sqlLimit") String sqlLimit, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    List<MQConfigEntity> queryMQConfigById(@Param("mqConfigId") Long mqConfigId) throws DataAccessException;

    Integer insertMQConfig(@Param("mqConfig") MQConfigEntity mqConfig, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateMQConfig(@Param("mqConfig") MQConfigEntity mqConfig, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteMQConfig(@Param("mqConfigId") Long mqConfigId) throws DataAccessException;

    Integer updateMQConfigOnLineFlagById(@Param("mqConfigId") Long mqConfigId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

}
