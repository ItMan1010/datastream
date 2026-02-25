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
package com.itman.datastream.common.config;

import lombok.Data;
//import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.itman.datastream.common.utils.CommUtils.getDataBaseType;

@Configuration
@Data
//@Slf4j
@ConfigurationProperties(prefix = "datastream")
public class DataStreamConfig {
    @PostConstruct
    public void init() {
        try {
            this.hostName = InetAddress.getLocalHost().getHostName();
            this.hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            //log.error("init: UnknownHostException=" + e);
        }

        metaDbBaseType = getDataBaseType(metaDbBaseUrl);
        if (metaDbBaseType == null) {
            //log.error("init: metaDbBaseType is null");
        }
    }

    private SourceProperties source = new SourceProperties();
    private TargetProperties target = new TargetProperties();

    private String hostName;
    private String hostIP;
    @Value("${spring.datasource.druid.metadb.url}")
    private String metaDbBaseUrl;
    private Integer metaDbBaseType;

    @Value("${spring.datasource.druid.metadb.sequence-mode:3}")
    private Integer sequenceMode;

    @Value("${spring.datasource.druid.metadb.db-teledb-type:0}")
    private Integer metaTeledbType;

    @Value("${datastream.queue.size:3}")
    private Integer dataStreamQueueSize;

    @Value("${datastream.queue.channel:1}")
    private Integer dataStreamQueueChannel;

    @Value("${datastream.parallel-stream-size:150}")
    private Integer dataStreamParallelStreamSize;

    @Value("${datastream.datasource.min-idle:#{null}}")
    private Integer dataStreamDataSourceMinIdle;

    @Value("${datastream.datasource.max-wait:#{null}}")
    private Integer dataStreamDataSourceMaxWait;

    @Value("${datastream.datasource.time-between-eviction-runs-millis:#{null}}")
    private Integer dataStreamDataSourceTimeBetweenEvictionRunsMillis;

    @Value("${datastream.datasource.min-evictable-idle-time-millis:#{null}}")
    private Integer dataStreamDataSourceMinEvictableIdleTimeMillis;

    @Value("${datastream.table-meta-oracle-type:2}")
    private Integer dataStreamTableMetaOracleType;

    @Value("${datastream.start-executor-type:#{null}}")
    private String dataStreamStartExecutorType;

    @Value("${datastream.move-trace.enable:false}")
    private Boolean dataStreamMoveTraceEnable;

    @Value("${datastream.parallel-task-size:10}")
    private Integer dataStreamParallelTaskSize;

    @Value("${init-data-meta-db.enable:false}")
    private Boolean dataStreamInitDataMetaDbEnable;

    /**
     * 是否任务运行异步入库:flase默认不开启
     */
    @Value("${datastream.move-info-async.enable:false}")
    private Boolean dataStreamMoveInfoAsyncEnable;
}
