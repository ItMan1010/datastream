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
package com.itman.datastream.admin.handler;


import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.holder.DataStreamHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.*;


@Slf4j
@Component
public class MonitorHandler {
    private final IMetaService metaService;
    private final DataStreamHolder dataStreamHolder;

    public MonitorHandler(IMetaService metaService, DataStreamHolder dataStreamHolder) {
        this.metaService = metaService;
        this.dataStreamHolder = dataStreamHolder;
    }

    public List<Long> getTaskRunningList() {
        return dataStreamHolder.getTaskRunningList();
    }

    public Integer getDataStreamQueueRunningSize(Long taskId) {
        return dataStreamHolder == null ? 0 : dataStreamHolder.getQueueRunningSize(taskId);
    }

    public void monitorTaskRunningQueue() throws DataStreamException {
        String timestamp = timestampGenerator();
        List<Long> taskRunningList = getTaskRunningList();
        if (!CollectionUtils.isEmpty(taskRunningList)) {
            List<MetricsEntity> metricsList = new ArrayList<>();
            for (Long iterator : taskRunningList) {
                Integer queueRunningSize = getDataStreamQueueRunningSize(iterator);
                MetricsEntity metrics = new MetricsEntity();
                metrics.setMetricsTime(Long.parseLong(timestamp));
                metrics.setMetricsValue(queueRunningSize.longValue());
                metrics.setTaskId(iterator);
                metrics.setMetricsId(metaService.querySequence(SEQ_METRICS_ID));
                metricsList.add(metrics);
            }
            metaService.insertMetrics(metricsList);
        }
    }
}
