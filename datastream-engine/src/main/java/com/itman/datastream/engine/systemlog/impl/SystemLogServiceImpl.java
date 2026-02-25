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
package com.itman.datastream.engine.systemlog.impl;

import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.common.entity.SessionEntity;
import com.itman.datastream.common.entity.SystemLogEntity;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.SystemLogDao;
import com.itman.datastream.engine.systemlog.ISystemLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.utils.CommUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogServiceImpl implements ISystemLogService {
    private final SystemLogDao systemLogDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;

    private IDatabaseAdapter getMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer appendSystemLog(SystemLogEntity systemLog) throws DataStreamException {
        systemLog.setSystemLogId(dataStreamDao.querySequence(SEQ_SYSTEM_LOG_ID));
        return systemLogDao.insertSystemLog(getMetaDbObject().makeSqlSystemDate(), systemLog);
    }

    @Override
    public List<SystemLogEntity> getSystemLog(Integer type, Integer page, Integer count) throws DataStreamException {
        return (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ?
                systemLogDao.querySystemLog(getMetaDbObject().makeSqlLimit(genPageRow(page, count), count), type) :
                systemLogDao.querySystemLogLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), type);
    }

    @Override
    public Integer getSystemLogCount(Integer type) throws DataStreamException {
        return systemLogDao.querySystemLogCount(type);
    }

    @Override
    public Integer appendSystemSession(String tokenKey, String username, Integer state, Long expiration) {
        try {
            LocalDateTime nowTime = LocalDateTime.now();
            LocalDateTime delayedTime = nowTime.plusSeconds(expiration);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            String createDate = getMetaDbObject().stringToDate(formatter.format(nowTime));
            String expireDate = getMetaDbObject().stringToDate(formatter.format(delayedTime));

            return systemLogDao.insertSystemSession(createDate, expireDate, tokenKey, username, state);
        } catch (DataStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer refreshSystemSessionState(String tokenKey, Integer state) {
        try {
            return systemLogDao.updateSystemSessionState(tokenKey, state);
        } catch (DataStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean isTokenExpiration(String tokenKey, Long expiration) {
        try {
            SessionEntity systemSession = systemLogDao.selectSystemSession(tokenKey, 1, getMetaDbObject().dateToString3("expire_date"));
            if (Objects.isNull(systemSession)) {
                return false;
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime nowTime = LocalDateTime.now();
            LocalDateTime expireTime = LocalDateTime.parse(systemSession.getExpireDate(), formatter);
            Duration duration = Duration.between(nowTime, expireTime);
            long seconds = duration.getSeconds();
            if (seconds < 0) {
                log.info("tokenKey={}, expiration={}, nowTime={}, expireTime={}, seconds={}", tokenKey, expiration, nowTime, expireTime, seconds);
                return true;
            }

            LocalDateTime delayedTime = nowTime.plusSeconds(expiration);
            String expireDate = getMetaDbObject().stringToDate(formatter.format(delayedTime));

            systemLogDao.updateSystemSessionExpireDate(tokenKey, expireDate);
            return false;
        } catch (DataStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Integer appendJobLogback(JobLogbackEntity jobLogback) throws DataStreamException {
        jobLogback.setJobLogbackId(dataStreamDao.querySequence(SEQ_JOB_LOGBACK_ID));
        jobLogback.setContent(jobLogback.getContent().replace("\t", "\\\\t").replace("\n", "\\\\n"));
        return systemLogDao.insertSystemJobLogback(getMetaDbObject().makeSqlSystemDate(), jobLogback);
    }

    @Override
    public List<JobLogbackEntity> getJobLogback(Integer jobType, Long jobId, Integer page, Integer count) throws DataStreamException {
        return (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ?
                systemLogDao.querySystemJobLogback(getMetaDbObject().makeSqlLimit(genPageRow(page, count), count), jobType, jobId) :
                systemLogDao.querySystemJobLogbackLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), jobType, jobId);
    }
}
