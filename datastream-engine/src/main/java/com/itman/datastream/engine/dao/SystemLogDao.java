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

import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.common.entity.SessionEntity;
import com.itman.datastream.common.entity.SystemLogEntity;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.engine.mapper.SystemLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SystemLogDao {
    public final SystemLogMapper systemLogMapper;
    private final DataStreamConfig dataStreamConfig;

    public List<JobLogbackEntity> querySystemJobLogbackLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer jobType, Long jobId) throws DataStreamException {
        try {
            return systemLogMapper.querySystemJobLogbackLikeOracle(pageBeginRow, pageEndRow, jobType, jobId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_JOB_LOGBACK_ERROR);
        }
    }

    public List<SystemLogEntity> querySystemLog(String sqlLimit, Integer type, String username, String moduleName,
                                                String startDate, String endDate, String keyword) throws DataStreamException {
        try {
            return systemLogMapper.querySystemLog(dataStreamConfig.getMetaTeledbType(), sqlLimit, type, username, moduleName, startDate, endDate, keyword);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_SYSTEM_LOG_ERROR);
        }
    }

    public List<SystemLogEntity> querySystemLogLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer type,
                                                          String username, String moduleName, String startDate, String endDate, String keyword) throws DataStreamException {
        try {
            return systemLogMapper.querySystemLogLikeOracle(pageBeginRow, pageEndRow, type, username, moduleName, startDate, endDate, keyword);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_SYSTEM_LOG_ERROR);
        }
    }

    public Integer insertSystemLog(String sysdate, SystemLogEntity systemLog) throws DataStreamException {
        try {
            return systemLogMapper.insertSystemLog(sysdate, systemLog);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_SYSTEM_LOG_ERROR);
        }
    }

    public Integer querySystemLogCount(Integer type, String username, String moduleName,
                                       String startDate, String endDate, String keyword) throws DataStreamException {
        try {
            return systemLogMapper.querySystemLogCount(dataStreamConfig.getMetaTeledbType(), type, username, moduleName, startDate, endDate, keyword);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_SYSTEM_LOG_COUNT_ERROR);
        }
    }

    public Integer deleteSystemLog(Long systemLogId) throws DataStreamException {
        try {
            return systemLogMapper.deleteSystemLog(systemLogId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_DEL_SYSTEM_LOG_ERROR);
        }
    }

    public Integer insertSystemSession(String createDate, String expireDate, String tokenKey, String username, Integer state) throws DataStreamException {
        try {
            return systemLogMapper.insertSystemSession(createDate, expireDate, tokenKey, username, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_SESSION_ERROR);
        }
    }

    public Integer updateSystemSessionState(String tokenKey, Integer state) throws DataStreamException {
        try {
            return systemLogMapper.updateSystemSessionState(tokenKey, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_SESSION_ERROR);
        }
    }

    public SessionEntity selectSystemSession(String tokenKey, Integer state, String expireDateColumn) throws DataStreamException {
        try {
            return systemLogMapper.selectSystemSession(dataStreamConfig.getMetaTeledbType(), tokenKey, state, expireDateColumn);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_SELECT_SESSION_ERROR);
        }
    }

    public Integer updateSystemSessionExpireDate(String tokenKey, String expireDate) throws DataStreamException {
        try {
            return systemLogMapper.updateSystemSessionExpireDate(tokenKey, expireDate);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_SESSION_EXPIRE_DATE_ERROR);
        }
    }

    public List<JobLogbackEntity> querySystemJobLogback(String sqlLimit, Integer jobType, Long jobId) throws DataStreamException {
        try {
            return systemLogMapper.querySystemJobLogback(dataStreamConfig.getMetaTeledbType(), sqlLimit, jobType, jobId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_JOB_LOGBACK_ERROR);
        }
    }

    public Integer insertSystemJobLogback(String sysdate, JobLogbackEntity JobLogback) throws DataStreamException {
        try {
            return systemLogMapper.insertSystemJobLogback(sysdate, JobLogback);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_JOB_LOGBACK_ERROR);
        }
    }



}
