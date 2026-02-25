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
 * 线程池详情
 */
@Data
public class ThreadPoolDetail {
    /**
     * 线程池名称
     */
    private String poolName;

    /**
     * 核心线程数
     */
    private Integer corePoolSize;

    /**
     * 最大线程数
     */
    private Integer maxPoolSize;

    /**
     * 活跃线程数
     */
    private Integer activeThreads;

    /**
     * 队列大小
     */
    private Integer queueSize;

    /**
     * 队列容量
     */
    private Integer queueCapacity;

    /**
     * 已完成任务数
     */
    private Long completedTaskCount;

    /**
     * 使用率
     */
    private Double usageRate;

    /**
     * 状态
     */
    private String status;

    /**
     * 拒绝任务数
     */
    private Long rejectedTaskCount;
}
