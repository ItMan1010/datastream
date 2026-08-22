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
package com.itman.datastream.engine.systemlog;

import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.common.entity.SystemLogEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;


public interface ISystemLogService {
    Boolean isTokenExpiration(String tokenKey, Long expiration);

    Integer appendJobLogback(JobLogbackEntity jobLogback) throws DataStreamException;

    List<SystemLogEntity> getSystemLog(Integer type, Integer page, Integer count, String username, String moduleName,
                                     String startDate, String endDate, String keyword) throws DataStreamException;

    Integer getSystemLogCount(Integer type, String username, String moduleName, String startDate, String endDate, String keyword) throws DataStreamException;

    Integer deleteSystemLog(Long systemLogId) throws DataStreamException;

    Integer appendSystemSession(String tokenKey, String username, Integer state, Long expiration);

    Integer refreshSystemSessionState(String tokenKey, Integer state);

    List<JobLogbackEntity> getJobLogback(Integer jobType, Long jobId, Integer page, Integer count) throws DataStreamException;

    Integer appendSystemLog(SystemLogEntity systemLog) throws DataStreamException;
}