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

import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.common.entity.SessionEntity;
import com.itman.datastream.common.entity.SystemLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;


@Mapper
public interface SystemLogMapper {
    Integer insertSystemLog(@Param("sysdate") String sysdate, @Param("systemLog") SystemLogEntity systemLog) throws DataAccessException;

    List<SystemLogEntity> querySystemLog(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("type") Integer type) throws DataAccessException;

    List<SystemLogEntity> querySystemLogLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("type") Integer type) throws DataAccessException;

    Integer querySystemLogCount(@Param("dbType") Integer dbType, @Param("type") Integer type);

    Integer insertSystemSession(@Param("createDate") String createDate, @Param("expireDate") String expireDate, @Param("tokenKey") String tokenKey, @Param("username") String username, @Param("state") Integer state) throws DataAccessException;

    Integer updateSystemSessionState(@Param("tokenKey") String tokenKey, @Param("state") Integer state) throws DataAccessException;

    SessionEntity selectSystemSession(@Param("dbType") Integer dbType, @Param("tokenKey") String tokenKey, @Param("state") Integer state, @Param("expireDateColumn") String expireDateColumn) throws DataAccessException;

    Integer updateSystemSessionExpireDate(@Param("tokenKey") String tokenKey, @Param("expireDate") String expireDate) throws DataAccessException;

    Integer insertSystemJobLogback(@Param("sysdate") String sysdate, @Param("jobLogback") JobLogbackEntity jobLogback) throws DataAccessException;

    List<JobLogbackEntity> querySystemJobLogback(@Param("dbType") Integer dbType, @Param("sqlLimit") String sqlLimit, @Param("jobType") Integer jobType, @Param("jobId") Long jobId) throws DataAccessException;

    List<JobLogbackEntity> querySystemJobLogbackLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("jobType") Integer jobType, @Param("jobId") Long jobId) throws DataAccessException;

}
