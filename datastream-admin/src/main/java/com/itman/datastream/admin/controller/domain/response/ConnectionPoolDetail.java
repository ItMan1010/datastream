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
package com.itman.datastream.admin.controller.domain.response;

import lombok.Data;

/**
 * 连接池详情
 */
@Data
public class ConnectionPoolDetail {
    /**
     * JDBC URL (脱敏)
     */
    private String jdbcUrl;

    /**
     * 总连接数
     */
    private Integer totalConnections;

    /**
     * 空闲连接数
     */
    private Integer idleConnections;

    /**
     * 活跃连接数
     */
    private Integer activeConnections;

    /**
     * 最大连接数
     */
    private Integer maxPoolSize;

    /**
     * 使用率
     */
    private Double usageRate;

    /**
     * 状态 (正常/告警/异常)
     */
    private String status;

    /**
     * 最大存活时间(毫秒)
     */
    private Long maxLifetimeMs;

    /**
     * 平均借用时长(毫秒)
     */
    private Long avgBorrowDurationMs;
}
