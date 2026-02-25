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
package com.itman.datastream.admin.controller;

import com.itman.datastream.admin.controller.domain.request.MonitorTaskRunningQueueRequest;
import com.itman.datastream.admin.controller.domain.response.MonitorTaskRunningQueueResponse;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {
    private final IMetaService metaService;

    /**
     * http://localhost:9199/datastream/metrics/monitorTaskRunningQueue
     *
     */
    @PostMapping(path = "/monitorTaskRunningQueue", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<MonitorTaskRunningQueueResponse> monitorTaskRunningQueue(@RequestBody MonitorTaskRunningQueueRequest monitorTaskRunningQueueRequest) {
        MonitorTaskRunningQueueResponse monitorTaskRunningQueueResponse = new MonitorTaskRunningQueueResponse();
        try {
            monitorTaskRunningQueueResponse.setMetricsList(metaService.queryMetrics(monitorTaskRunningQueueRequest.getTaskId()));
        } catch (DataStreamException aie) {
            monitorTaskRunningQueueResponse.setErrorCode(aie.getErrCode());
            monitorTaskRunningQueueResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            monitorTaskRunningQueueResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            monitorTaskRunningQueueResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("monitorTaskRunningQueueResponse={}", monitorTaskRunningQueueResponse);
        }
        return new ResponseEntity<>(monitorTaskRunningQueueResponse, HttpStatus.OK);
    }
}
