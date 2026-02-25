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


import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.DATA_SOURCE_TYPE_KAFKA;


@Slf4j
@RestController
@RequestMapping("/api/kafka")
public class KafkaAdapterController {

    private final DataSourceFactory dataSourceFactory;

    public KafkaAdapterController(DataSourceFactory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public IMQAdapterApi matchMQ(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchMQ(dataSourceType);
    }

    //curl -X POST "http://localhost:9199/datastream/api/kafka/bindProducer?taskId=1&destination=my-topic&bootstrapServers=192.168.1.8:19092"
    @PostMapping("/bindProducer")
    public ResponseEntity<String> bindProducer(@RequestParam Long taskId, @RequestParam String destination, @RequestParam String bootstrapServers, @RequestBody(required = false) Map<String, Object> additionalProps) {
        log.info("收到绑定请求: linkTaskId={}, destination={}, bootstrapServers={}", taskId, destination, bootstrapServers);
        String resultInfo = "success";
        try {
            matchMQ(DATA_SOURCE_TYPE_KAFKA).bindProducerDestination(taskId, destination, bootstrapServers, additionalProps != null ? additionalProps : Collections.emptyMap());
        } catch (DataStreamException aie) {
            resultInfo = aie.getErrMsg();
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            resultInfo = e.getMessage();
            log.error("Exception=", e);
        }
        return ResponseEntity.ok(resultInfo);
    }

    //curl -X POST "http://localhost:9199/datastream/api/kafka/sendMessage?taskId=1&message=oooooo"
    @PostMapping("/sendMessage")
    public ResponseEntity<String> sendMessage(@RequestParam Long taskId, @RequestParam String message) {
        log.info("收到发送请求: linkTaskId={}, message={}", taskId, message);
        String resultInfo = "success";
        try {
            matchMQ(DATA_SOURCE_TYPE_KAFKA).sendMQMessage(taskId, message);
        } catch (DataStreamException aie) {
            resultInfo = aie.getErrMsg();
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            resultInfo = e.getMessage();
            log.error("Exception=", e);
        }
        return ResponseEntity.ok(resultInfo);
    }

    //curl -X POST "http://localhost:9199/datastream/api/kafka/unbindProducer?taskId=1"
    @PostMapping("/unbindProducer")
    public ResponseEntity<String> unbindProducer(@RequestParam Long taskId) {
        log.info("unbindProducer: linkTaskId={}", taskId);

        String resultInfo = "success";
        try {
            matchMQ(DATA_SOURCE_TYPE_KAFKA).unbindProducerDestination(taskId);
        } catch (DataStreamException aie) {
            resultInfo = aie.getErrMsg();
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            resultInfo = e.getMessage();
            log.error("Exception=", e);
        }
        return ResponseEntity.ok(resultInfo);
    }

    //curl -X POST "http://localhost:9199/datastream/api/kafka/bindConsumer?taskId=1&destination=my-topic-2&bootstrapServers=192.168.1.3:19092"
    @PostMapping("/bindConsumer")
    public ResponseEntity<String> bindConsumer(@RequestParam Long taskId, @RequestParam String destination, @RequestParam String bootstrapServers, @RequestBody(required = false) Map<String, Object> additionalProps) {
        log.info("收到绑定请求: linkTaskId={}, destination={}, bootstrapServers={}", taskId, destination, bootstrapServers);
        String resultInfo = "success";
        try {
            String groupId = "test-group-consumer-3";

            matchMQ(DATA_SOURCE_TYPE_KAFKA).bindConsumerDestination(taskId, destination, bootstrapServers, groupId, additionalProps, (message, topicName, partition, offset) -> {
                log.info("**************** linkTaskId:" + taskId + ",Topic: " + topicName + ",Partition: " + partition + ",Offset: " + offset + ",Message: " + message);
            }, null);
        } catch (DataStreamException aie) {
            resultInfo = aie.getErrMsg();
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            resultInfo = e.getMessage();
            log.error("Exception=", e);
        }
        return ResponseEntity.ok(resultInfo);
    }

    //curl -X POST "http://localhost:9199/datastream/api/kafka/unbindConsumer?taskId=1"
    @PostMapping("/unbindConsumer")
    public ResponseEntity<String> unbindConsumer(@RequestParam Long taskId) {
        log.info("unbindConsumer: linkTaskId={}", taskId);

        String resultInfo = "success";
        try {
            matchMQ(DATA_SOURCE_TYPE_KAFKA).unbindConsumerDestination(taskId);
        } catch (DataStreamException aie) {
            resultInfo = aie.getErrMsg();
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            resultInfo = e.getMessage();
            log.error("Exception=", e);
        }
        return ResponseEntity.ok(resultInfo);
    }
}
