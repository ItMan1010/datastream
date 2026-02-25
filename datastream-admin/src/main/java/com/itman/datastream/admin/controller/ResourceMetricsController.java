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

import com.itman.datastream.admin.collector.ResourceMetricsCollector;
import com.itman.datastream.admin.controller.domain.response.*;
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

/**
 * 资源监控Controller
 * 提供系统资源监控相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/metrics/resource")
@RequiredArgsConstructor
public class ResourceMetricsController {

    private final ResourceMetricsCollector metricsCollector;

    /**
     * http://localhost:9199/datastream/metrics/resource/all
     * 获取所有资源指标
     */
    @PostMapping(path = "/all", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ResourceMetricsResponse> getAllMetrics(@RequestBody(required = false) Object request) {
        ResourceMetricsResponse response = new ResourceMetricsResponse();
        try {
            response = metricsCollector.collectAllMetrics();
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("getAllMetrics response={}", response);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/metrics/resource/system
     * 获取系统级指标
     */
    @PostMapping(path = "/system", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<SystemResourceMetricsResponse> getSystemMetrics(@RequestBody(required = false) Object request) {
        SystemResourceMetricsResponse response = new SystemResourceMetricsResponse();
        try {
            SystemResourceMetrics metrics = metricsCollector.collectSystemMetrics();
            response.setSystemMetrics(metrics);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/metrics/resource/task
     * 获取任务级指标
     */
    @PostMapping(path = "/task", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TaskResourceMetricsResponse> getTaskMetrics(@RequestBody(required = false) Object request) {
        TaskResourceMetricsResponse response = new TaskResourceMetricsResponse();
        try {
            TaskResourceMetrics metrics = metricsCollector.collectTaskMetrics();
            response.setTaskMetrics(metrics);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/metrics/resource/connection
     * 获取连接池指标
     */
    @PostMapping(path = "/connection", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ConnectionPoolMetricsResponse> getConnectionMetrics(@RequestBody(required = false) Object request) {
        ConnectionPoolMetricsResponse response = new ConnectionPoolMetricsResponse();
        try {
            ConnectionPoolMetrics metrics = metricsCollector.collectConnectionPoolMetrics();
            response.setConnectionMetrics(metrics);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/metrics/resource/threadpool
     * 获取线程池指标
     */
    @PostMapping(path = "/threadpool", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ThreadPoolMetricsResponse> getThreadPoolMetrics(@RequestBody(required = false) Object request) {
        ThreadPoolMetricsResponse response = new ThreadPoolMetricsResponse();
        try {
            ThreadPoolMetrics metrics = metricsCollector.collectThreadPoolMetrics();
            response.setThreadPoolMetrics(metrics);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
