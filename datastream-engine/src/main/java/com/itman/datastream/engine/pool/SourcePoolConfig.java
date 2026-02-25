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
package com.itman.datastream.engine.pool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static com.itman.datastream.common.constant.DataStreamConstant.SOURCE_WORKS_POOL_EXECUTOR;


@Configuration
@EnableAsync
@Slf4j
public class SourcePoolConfig {
    @Value("${datastream.source.core-pool-size:50}")
    private Integer sourceCorePoolSize;

    @Bean(SOURCE_WORKS_POOL_EXECUTOR)
    public Executor myExecutor() {
        ThreadPoolTaskExecutor sourceExecutor = new ThreadPoolTaskExecutor();
        // 核心线程数：线程池创建时候初始化的线程数
        sourceExecutor.setCorePoolSize(sourceCorePoolSize);
        // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
        sourceExecutor.setMaxPoolSize(sourceCorePoolSize);
        // 缓冲队列：用来缓冲执行任务的队列
        sourceExecutor.setQueueCapacity(500);
        // 允许线程的空闲时间60秒：当超过了核心线程之外的线程在空闲时间到达之后会被销毁
        sourceExecutor.setKeepAliveSeconds(60);
        // 线程池名的前缀：设置好了之后可以方便我们定位处理任务所在的线程池
        sourceExecutor.setThreadNamePrefix(SOURCE_WORKS_POOL_EXECUTOR + "-");
        // 缓冲队列满了之后的拒绝策略：由调用线程处理（一般是主线程）
        sourceExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        sourceExecutor.initialize();
        return sourceExecutor;
    }
}
