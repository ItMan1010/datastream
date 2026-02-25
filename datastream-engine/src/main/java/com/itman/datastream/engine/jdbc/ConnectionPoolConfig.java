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
package com.itman.datastream.engine.jdbc;

import lombok.Builder;
import lombok.Data;

/**
 * 连接池配置类
 * 用于配置连接池的各项参数
 *
 * @author DataStream
 */
@Data
@Builder
public class ConnectionPoolConfig {

    /**
     * 最大连接数，默认10
     */
    @Builder.Default
    private int maxPoolSize = 10;

    /**
     * 最小空闲连接数，默认2
     */
    @Builder.Default
    private int minIdleSize = 2;

    /**
     * 获取连接超时时间（毫秒），默认30秒
     */
    @Builder.Default
    private long connectionTimeoutMs = 30000L;

    /**
     * 连接最大空闲时间（毫秒），默认5分钟
     */
    @Builder.Default
    private long maxIdleTimeMs = 300000L;

    /**
     * 连接最大存活时间（毫秒），默认30分钟
     */
    @Builder.Default
    private long maxLifetimeMs = 1800000L;

    /**
     * 连接验证超时时间（秒），默认5秒
     */
    @Builder.Default
    private int validationTimeoutSeconds = 5;

    /**
     * 空闲连接检测间隔（毫秒），默认1分钟
     */
    @Builder.Default
    private long idleCheckIntervalMs = 60000L;

    /**
     * 连接泄漏检测阈值（毫秒），默认0表示禁用
     * 当连接借出时间超过此阈值时，记录警告日志
     */
    @Builder.Default
    private long leakDetectionThresholdMs = 0L;

    /**
     * 获取默认配置
     */
    public static ConnectionPoolConfig defaultConfig() {
        return ConnectionPoolConfig.builder().build();
    }

    /**
     * 校验配置参数
     */
    public void validate() {
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("maxPoolSize must be greater than 0");
        }
        if (minIdleSize < 0) {
            throw new IllegalArgumentException("minIdleSize must be greater than or equal to 0");
        }
        if (minIdleSize > maxPoolSize) {
            throw new IllegalArgumentException("minIdleSize must be less than or equal to maxPoolSize");
        }
        if (connectionTimeoutMs <= 0) {
            throw new IllegalArgumentException("connectionTimeoutMs must be greater than 0");
        }
        if (maxIdleTimeMs <= 0) {
            throw new IllegalArgumentException("maxIdleTimeMs must be greater than 0");
        }
        if (maxLifetimeMs <= 0) {
            throw new IllegalArgumentException("maxLifetimeMs must be greater than 0");
        }
        if (validationTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("validationTimeoutSeconds must be greater than 0");
        }
    }
}
