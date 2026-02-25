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
import com.itman.datastream.common.entity.DebeziumHistoryEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.dao.DataStreamDao;
import io.debezium.config.Configuration;
import io.debezium.document.DocumentReader;
import io.debezium.document.DocumentWriter;
import io.debezium.relational.history.*;
import io.debezium.util.FunctionalReadWriteLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.itman.datastream.common.constant.DataStreamConstant.SEQ_DEBEZIUM_HISTORY_ID;


@Component
@Slf4j
public class DataStreamDatabaseHistory extends AbstractDatabaseHistory implements ApplicationContextAware {
    private static ApplicationContext applicationContext;
    private DataStreamDao dataStreamDao;
    private final DocumentWriter documentWriter = DocumentWriter.defaultWriter();
    private final DocumentReader documentReader = DocumentReader.defaultReader();
    private DataSourceFactory dataSourceFactory;
    private DataStreamConfig dataStreamConfig;
    private final AtomicBoolean running = new AtomicBoolean();
    private final FunctionalReadWriteLock lock = FunctionalReadWriteLock.reentrant();


    @PostConstruct
    public void init() {
        String server = Thread.currentThread().getName();
        log.info("---------------init,server={}", server);
    }

    private IDatabaseAdapter geMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }


    public void configure(Configuration config, HistoryRecordComparator comparator, DatabaseHistoryListener listener, boolean useCatalogBeforeSchema) {
        String server = Thread.currentThread().getName();
        log.info("---------------configure,server={}", server);

        log.info("Configured server name for history: server={}", server);
        // 确保 DataStreamDao 已被初始化
        if (this.dataStreamDao == null && applicationContext != null) {
            try {
                this.dataStreamDao = applicationContext.getBean(DataStreamDao.class);
                log.info("Successfully obtained DataStreamDao from Spring context in configure ,server={}", server);
            } catch (Exception e) {
                log.error("Could not get DataStreamDao from Spring context in configure ,server={}", server, e);
                throw new RuntimeException("Failed to initialize DataStreamDao", e);
            }
        }

        if (this.dataSourceFactory == null && applicationContext != null) {
            try {
                this.dataSourceFactory = applicationContext.getBean(DataSourceFactory.class);
                log.info("Successfully obtained dataSourceFactory from Spring context in configure ,server={}", server);
            } catch (Exception e) {
                log.error("Could not get dataSourceFactory from Spring context in configure ,server={}", server, e);
                throw new RuntimeException("Failed to initialize dataSourceFactory", e);
            }
        }

        if (this.dataStreamConfig == null && applicationContext != null) {
            try {
                this.dataStreamConfig = applicationContext.getBean(DataStreamConfig.class);
                log.info("Successfully obtained dataStreamConfig from Spring context in configure,server={}", server);
            } catch (Exception e) {
                log.error("Could not get dataStreamConfig from Spring context in configure,server={}", server, e);
                throw new RuntimeException("Failed to initialize dataStreamConfig", e);
            }
        }

        if (this.running.get()) {
            throw new IllegalStateException("Database history file already initialized to ,server=" + server);
        } else {
            super.configure(config, comparator, listener, useCatalogBeforeSchema);
        }
    }

    public void start() {
        String server = Thread.currentThread().getName();
        log.info("---------------start,server={}", server);

        super.start();
        this.lock.write(() -> {
            if (this.running.compareAndSet(false, true)) {
                if (!this.storageExists()) {
                    log.info("storageExists is error");
                    throw new DatabaseHistoryException("Unable to create history file at");
                }
            }
        });
    }

    @Override
    protected void storeRecord(HistoryRecord record) throws DatabaseHistoryException {
        String server = Thread.currentThread().getName();
        log.info("---------------storeRecord,server={}", server);

        if (record != null) {
            this.lock.write(() -> {
                if (!this.running.get()) {
                    throw new IllegalStateException("The history has been stopped and will not accept more records");
                } else {
                    try {
                        String recordJson = documentWriter.write(record.document());
                        log.info("*****server={}, recordJson={}", server, recordJson);
                        Long debeziumHistoryId = dataStreamDao.querySequence(SEQ_DEBEZIUM_HISTORY_ID);
                        dataStreamDao.insertDebeziumHistory(debeziumHistoryId, geMetaDbObject().makeSqlSystemDate(), server, recordJson);
                    } catch (DataStreamException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    public void stop() {
        String server = Thread.currentThread().getName();
        log.info("---------------stop, server={}", server);

        this.running.set(false);
        super.stop();
    }

    @Override
    protected void recoverRecords(Consumer<HistoryRecord> consumer) {
        String server = Thread.currentThread().getName();
        log.info("---------------recoverRecords, server={}", server);

        this.lock.write(() -> {
            if (this.exists()) {
                try {
                    // 从数据库查询历史记录（使用 ThreadLocal 获取当前线程的 server 名称）
                    log.info("---------------recoverRecords，server={}", server);

                    List<DebeziumHistoryEntity> historyList = dataStreamDao.selectDebeziumHistory(server);

                    if (historyList != null && !historyList.isEmpty()) {
                        log.info("server={},Recovered {} database history records", server, historyList.size());

                        for (DebeziumHistoryEntity history : historyList) {
                            try {
                                consumer.accept(new HistoryRecord(documentReader.read(history.getHistoryData())));
                            } catch (Exception e) {
                                log.error("Failed to recover history record: {}, server={}", history.getHistoryData(), server, e);
                            }
                        }
                    } else {
                        log.info("No database history records found to recover, server={}", server);
                    }
                } catch (DataStreamException e) {
                    throw new DatabaseHistoryException("Failed to recover database history, server={}" + server, e);
                } catch (Exception e) {
                    throw new DatabaseHistoryException("Failed to parse history records, server={}" + server, e);
                }
            }
        });
    }

    @Override
    public boolean storageExists() {
        String server = Thread.currentThread().getName();
        log.info("---------------storageExists,server={}", server);
        try {
            dataStreamDao.selectDebeziumHistory(server);
        } catch (DataStreamException e) {
            log.error("---------------storageExists DataStreamException server={}", server, e);
            return false;
        } catch (Exception e) {
            log.error("---------------storageExists Exception server={}", server, e);
            return false;
        }
        return true;
    }

    @Override
    public boolean exists() {
        String server = Thread.currentThread().getName();
        log.info("---------------exists,server={}", server);
        return storageExists();
    }

    public String toString() {
        return " table data_stream_debezium_history";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        DataStreamDatabaseHistory.applicationContext = applicationContext;
    }
}