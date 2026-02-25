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

import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.engine.event.JobLogbackEvent;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import static com.itman.datastream.engine.context.ThreadContext.getThreadLocalJobId;
import static com.itman.datastream.engine.context.ThreadContext.getThreadLocalJobType;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogEventImpl implements ISystemLogEvent {
    private final ApplicationEventPublisher applicationEventPublisher;

    public void jobLogbackEvent(Integer jobType, Long infoId, String content) {
        JobLogbackEntity jobLogback = new JobLogbackEntity();
        jobLogback.setJobId(infoId);
        jobLogback.setJobType(jobType);
        jobLogback.setContent(content);
        applicationEventPublisher.publishEvent(new JobLogbackEvent(jobLogback));
    }

    public void jobLogbackEvent(String content) {
        if (getThreadLocalJobId() != null && getThreadLocalJobId() > 0L && getThreadLocalJobType() != null && getThreadLocalJobType() > 0) {
            JobLogbackEntity jobLogback = new JobLogbackEntity();
            jobLogback.setJobId(getThreadLocalJobId());
            jobLogback.setJobType(getThreadLocalJobType());
            jobLogback.setContent(content);
            applicationEventPublisher.publishEvent(new JobLogbackEvent(jobLogback));
        }
    }
}
