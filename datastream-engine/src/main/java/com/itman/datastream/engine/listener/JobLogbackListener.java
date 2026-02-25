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
package com.itman.datastream.engine.listener;

import cn.hutool.extra.spring.SpringUtil;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.JobLogbackEntity;
import com.itman.datastream.engine.event.JobLogbackEvent;
import com.itman.datastream.engine.systemlog.ISystemLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static com.itman.datastream.common.constant.DataStreamConstant.EVENT_WORKS_POOL_EXECUTOR;

@Service
@Slf4j
public class JobLogbackListener implements ApplicationListener<JobLogbackEvent> {
    @Override
    @Async(EVENT_WORKS_POOL_EXECUTOR)
    public void onApplicationEvent(JobLogbackEvent event) {
        try {
            JobLogbackEntity canalJobLogbackEntity = new JobLogbackEntity();
            BeanUtils.copyProperties(event.getSource(), canalJobLogbackEntity);
            SpringUtil.getBean(ISystemLogService.class).appendJobLogback(canalJobLogbackEntity);
        } catch (DataStreamException e) {
            log.error("", e);
        }
    }
}
