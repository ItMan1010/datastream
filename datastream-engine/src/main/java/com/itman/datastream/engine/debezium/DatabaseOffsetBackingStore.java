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
package com.itman.datastream.engine.debezium;

import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.DebeziumOffsetEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.storage.OffsetBackingStore;
import org.apache.kafka.connect.util.Callback;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DatabaseOffsetBackingStore implements OffsetBackingStore, ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private DataStreamDao dataStreamDao;
    private DataSourceFactory dataSourceFactory;
    private DataStreamConfig dataStreamConfig;


    private IDatabaseAdapter geMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    // 1. 添加无参构造函数
    public DatabaseOffsetBackingStore() {
        log.info("DatabaseOffsetBackingStore constructor called");
    }

    public DatabaseOffsetBackingStore(DataStreamDao dataStreamDao) {
        this.dataStreamDao = dataStreamDao;
    }

    @Override
    public void start() {
        log.info("---------------start");

    }

    @Override
    public void stop() {
        log.info("---------------stop");

    }

    private static byte[] byteBufferChangToBytes(ByteBuffer byteBuffer) {
        byte[] keyBytes = new byte[byteBuffer.remaining()];
        byteBuffer.duplicate().get(keyBytes);
        return keyBytes;
    }

    @Override
    public Future<Map<ByteBuffer, ByteBuffer>> get(Collection<ByteBuffer> keys, Callback<Map<ByteBuffer, ByteBuffer>> callback) {
        log.info("---------------get");
        if (CollectionUtils.isEmpty(keys)) {
            log.info("No keys to query");
            if (callback != null) {
                callback.onCompletion(null, new HashMap<>());
            }
            return new CompletedFuture<>(new HashMap<>());
        }

        // 将 ByteBuffer 转换为 byte[]
        List<byte[]> offsetKeys = keys.stream().map(keyBuffer -> byteBufferChangToBytes(keyBuffer)).collect(Collectors.toList());
        log.info("---------------offsetKeys={}", offsetKeys.stream().map(k -> new String(k)).collect(Collectors.joining(", ")));

        DebeziumOffsetEntity debeziumOffset = null;
        try {
            String offsetKeysString = new String(offsetKeys.get(0), StandardCharsets.UTF_8);
            debeziumOffset = dataStreamDao.findOffsets(offsetKeysString);
        } catch (DataStreamException e) {
            throw new RuntimeException(e);
        }

        // 转换为ByteBuffer
        Map<ByteBuffer, ByteBuffer> result = new HashMap<>();
        if (debeziumOffset != null && debeziumOffset.getOffsetKey() != null && debeziumOffset.getOffsetValue() != null) {
            result.put(ByteBuffer.wrap(debeziumOffset.getOffsetKey().getBytes(StandardCharsets.UTF_8)), ByteBuffer.wrap(debeziumOffset.getOffsetValue().getBytes(StandardCharsets.UTF_8)));
        }

        // 4. 回调
        if (callback != null) {
            callback.onCompletion(null, result);
        }
        return new CompletedFuture<>(result);
    }

    @Override
    public Future<Void> set(Map<ByteBuffer, ByteBuffer> bufferMap, Callback<Void> callback) {
        log.info("---------------set");
        if (bufferMap == null || bufferMap.isEmpty()) {
            // 重要：立即调用回调
            if (callback != null) {
                callback.onCompletion(null, null);
            }
            return new CompletedFuture<>(null);
        }


        // 使用异步执行，避免阻塞
        CompletableFuture.runAsync(() -> {
            for (Map.Entry<ByteBuffer, ByteBuffer> entry : bufferMap.entrySet()) {
                byte[] keyBytes = byteBufferChangToBytes(entry.getKey());
                byte[] valueBytes = byteBufferChangToBytes(entry.getValue());
                String offsetkey = new String(keyBytes, StandardCharsets.UTF_8);
                String offsetValue = new String(valueBytes, StandardCharsets.UTF_8);
                log.info("---------------offsetkey={}, value={}", offsetkey, offsetValue);

                try {
                    DebeziumOffsetEntity debeziumOffset = dataStreamDao.findOffsets(offsetkey);
                    if (debeziumOffset != null && debeziumOffset.getOffsetKey() != null) {
                        dataStreamDao.updateOffsets(offsetkey, offsetValue, geMetaDbObject().makeSqlSystemDate());
                    } else {
                        dataStreamDao.insertOffsets(offsetkey, offsetValue, geMetaDbObject().makeSqlSystemDate());
                    }
                } catch (DataStreamException e) {
                    log.error("Failed to save offsets", e);
                }
            }
        });

        // 重要：成功时调用回调
        if (callback != null) {
            callback.onCompletion(null, null);
        }
        return new CompletedFuture<>(null);
    }

    @Override
    public void configure(WorkerConfig workerConfig) {
        // 获取所有原始配置
        Map<String, String> configs = workerConfig.originalsStrings();

        // 查找自定义配置
        String taskId = null;

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("offset.storage.")) {
                log.info("Custom config [{}] = {}", key, value);

                if (key.equals("offset.storage.linkTaskId")) {
                    taskId = value;
                }
            }
        }

        log.info("---------------configure:linkTaskId=" + taskId);

        // 从Spring上下文获取DataStreamDao
        if (this.dataStreamDao == null && applicationContext != null) {
            try {
                this.dataStreamDao = applicationContext.getBean(DataStreamDao.class);
                log.info("Successfully obtained DataStreamDao from Spring context in configure()");
            } catch (Exception e) {
                log.error("Could not get DataStreamDao from Spring context in configure()", e);
                throw new RuntimeException("Failed to initialize DataStreamDao", e);
            }
        }

        if (this.dataSourceFactory == null && applicationContext != null) {
            try {
                this.dataSourceFactory = applicationContext.getBean(DataSourceFactory.class);
                log.info("Successfully obtained dataSourceFactory from Spring context in configure()");
            } catch (Exception e) {
                log.error("Could not get dataSourceFactory from Spring context in configure()", e);
                throw new RuntimeException("Failed to initialize dataSourceFactory", e);
            }
        }

        if (this.dataStreamConfig == null && applicationContext != null) {
            try {
                this.dataStreamConfig = applicationContext.getBean(DataStreamConfig.class);
                log.info("Successfully obtained dataStreamConfig from Spring context in configure()");
            } catch (Exception e) {
                log.error("Could not get dataStreamConfig from Spring context in configure()", e);
                throw new RuntimeException("Failed to initialize dataStreamConfig", e);
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DatabaseOffsetBackingStore.applicationContext = applicationContext;
    }

    // CompletedFuture 内部类
    private static class CompletedFuture<T> implements Future<T> {
        private final T result;

        CompletedFuture(T result) {
            this.result = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() {
            return result;
        }

        @Override
        public T get(long timeout, java.util.concurrent.TimeUnit unit) {
            return result;
        }
    }
}
