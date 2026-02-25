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
package com.itman.datastream.connectors.kafka;

import com.itman.datastream.common.api.ConsumerMessageHandler;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.constant.MQTypeEnum;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.itman.datastream.common.constant.MQTypeEnum.KAKFA;

@Slf4j
@Component
public class KafkaAdapterImpl implements IMQAdapterApi {
    @Override
    public Boolean chooseMQ(MQTypeEnum mQTypeEnum) {
        return mQTypeEnum.equals(KAKFA);
    }

    // 生产者相关缓存
    //todo 后续优化使用myconfigId做主键,多任务共享消息连接资源，或通过配置方式单任务独占资源还是共享方式
    private final Map<Long, KafkaTemplate<String, String>> producerCache = new ConcurrentHashMap<>();
    private final Map<Long, DefaultKafkaProducerFactory<String, String>> producerFactoryCache = new ConcurrentHashMap<>();

    // 消费者相关缓存
    private final Map<Long, KafkaConsumer<String, String>> consumerCacheMap = new ConcurrentHashMap<>();
    private final Map<Long, AtomicBoolean> consumerRunningFlagsMap = new ConcurrentHashMap<>();
    private final Map<Long, ExecutorService> consumerExecutorMap = new ConcurrentHashMap<>();

    public void bindProducerDestination(Long taskId, String destination, String bootstrapServers, Map<String, Object> additionalProps) throws DataStreamException {
        if (producerCache.containsKey(taskId)) {
            log.info("连接已存在，无需重新绑定: {}", taskId);
            throw new DataStreamException("xxxxx", "已经绑定");
        }

        try {
            log.info("开始创建动态 Kafka 连接: linkTaskId={}, servers={}, topic={}", taskId, bootstrapServers, destination);

            // 1. 构建 Kafka 配置参数
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            // 设置默认参数，也可以被 additionalProps 覆盖
            props.put(ProducerConfig.RETRIES_CONFIG, 0);
            props.put(ProducerConfig.ACKS_CONFIG, "1");

            // 合并额外参数
            if (additionalProps != null) {
                props.putAll(additionalProps);
            }

            // 2. 手动创建 ProducerFactory
            DefaultKafkaProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(props);

            // 3. 创建 KafkaTemplate
            KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
            // 如果只发往特定 topic，可以设置 defaultTopic
            kafkaTemplate.setDefaultTopic(destination);

            // 4. 存入缓存
            producerFactoryCache.put(taskId, producerFactory);
            producerCache.put(taskId, kafkaTemplate);

            log.info("动态 Kafka 连接创建成功: linkTaskId{},bootstrapServers={}", taskId, bootstrapServers);

        } catch (Exception e) {
            log.error("linkTaskId=" + taskId, e);
            throw new DataStreamException("xxxxx", e.getMessage());
        }
    }

    /**
     * 动态解绑（移除连接并释放资源）
     * 这个方法需要在 Controller 添加对应的 @PostMapping("/unbind")
     */
    public void unbindProducerDestination(Long taskId) throws DataStreamException {
        try {
            // 1. 移除 Template
            if (producerCache.containsKey(taskId)) {
                producerCache.remove(taskId);
            } else {
                log.info("producerCache not found, linkTaskId=" + taskId);
            }


            // 2. 获取并销毁 Factory (断开连接，释放线程资源)
            if (producerFactoryCache.containsKey(taskId)) {
                DefaultKafkaProducerFactory<String, String> factory = producerFactoryCache.remove(taskId);
                if (factory != null) {
                    factory.destroy(); // 关键：优雅关闭 Kafka 连接
                }
            } else {
                log.info("producerFactoryCache not found, linkTaskId=" + taskId);
            }

            log.info("动态 KafkaProducer已销毁: linkTaskId={}", taskId);
        } catch (Exception e) {
            log.error("linkTaskId=" + taskId, e);
            throw new DataStreamException("xxxxx", e.getMessage());
        }
    }

    public void sendMQMessage(Long taskId, String message) throws DataStreamException {
        KafkaTemplate<String, String> template = producerCache.get(taskId);

        //todo 后续增加发送成功还是失败
        if (template != null) {
            template.sendDefault(message); // 发送到绑定的 defaultTopic
        } else {
            throw new DataStreamException("xxxxx", "No active binding found for linkTaskId=" + taskId);
        }
    }


    /**
     * 绑定消费者目的地并启动消费
     */
    public void bindConsumerDestination(Long taskId, String destination, String bootstrapServers, String groupId, Map<String, Object> additionalProps, ConsumerMessageHandler messageHandler, Runnable onStopped) throws DataStreamException {
        if (consumerCacheMap.containsKey(taskId)) {
            log.info("消费者连接已存在，无需重新绑定: {}", taskId);
            throw new DataStreamException("xxxxx", "消费者已经绑定1,linkTaskId=" + taskId);
        }

        if (consumerRunningFlagsMap.containsKey(taskId)) {
            log.info("消费者连接已存在，无需重新绑定: {}", taskId);
            throw new DataStreamException("xxxxx", "消费者已经绑定2,linkTaskId=" + taskId);
        }

        if (consumerExecutorMap.containsKey(taskId)) {
            log.info("消费者连接已存在，无需重新绑定: {}", taskId);
            throw new DataStreamException("xxxxx", "消费者已经绑定3,linkTaskId=" + taskId);
        }

        try {
            log.info("开始创建动态 Kafka 消费者连接: linkTaskId={}, servers={}, topic={}, groupId={}", taskId, bootstrapServers, destination, groupId);

            Properties properties = new Properties();
            // Kafka集群地址
            properties.put("bootstrap.servers", bootstrapServers);
            // 消费者组，仅在subscribe模式下生效，用于分区自动再均衡，而assign模式直接指定分区
            properties.put("group.id", groupId);
            // 反序列化器
            properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

            // 关键配置：当没有初始偏移量时从哪里开始消费
            // earliest: 从最早的消息开始消费（包括历史消息）
            // latest: 只消费启动后发送的新消息
            properties.put("auto.offset.reset", "earliest");

            // 启用自动提交偏移量
            properties.put("enable.auto.commit", "true");
            // 自动提交间隔（毫秒）
            properties.put("auto.commit.interval.ms", "1000");

            // 会话超时（毫秒）
            properties.put("session.timeout.ms", "30000");
            // 心跳间隔（毫秒）
            properties.put("heartbeat.interval.ms", "3000");

            // 请求超时时间
            properties.put("request.timeout.ms", "40000");
            // 获取数据的最小字节数
            properties.put("fetch.min.bytes", "1");
            // 等待 fetch 响应的最大时间
            properties.put("fetch.max.wait.ms", "500");

            // 创建消费者
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

            log.info("尝试获取 Topic 元数据: linkTaskId={}, destination={}", taskId, destination);
            // 获取 topic 的 partition 信息
            List<PartitionInfo> partitions = consumer.partitionsFor(destination);
            log.info("Topic partitions: linkTaskId={}, destination={}, partitions={}", taskId, destination, partitions);

            if (partitions == null || partitions.isEmpty()) {
                log.error("Topic 不存在或没有 partitions: linkTaskId={}, destination={}", taskId, destination);
                throw new DataStreamException("TOPIC_NOT_FOUND", "Topic 不存在或无法访问: " + destination);
            }

            // **测试方案：手动分配 partition 并从 earliest 开始消费**
            List<TopicPartition> topicPartitions = partitions.stream().map(p -> new TopicPartition(p.topic(), p.partition())).collect(Collectors.toList());

            consumer.assign(topicPartitions);
            log.info("手动分配 partitions: linkTaskId={}, partitions={}", taskId, topicPartitions);

            // **尝试加载保存的 offset 并 seek 到指定位置**
            boolean hasSeek = false;
            for (TopicPartition tp : topicPartitions) {
                Long savedOffset = loadOffset(taskId, tp.topic(), tp.partition());
                if (savedOffset != null) {
                    log.info("从文件加载 offset 并 seek: linkTaskId={}, topic={}, partition={}, offset={}", taskId, tp.topic(), tp.partition(), savedOffset);
                    consumer.seek(tp, savedOffset);
                    hasSeek = true;
                }
            }

            if (!hasSeek) {
                // 强制 seek 到 earliest offset
                consumer.seekToBeginning(topicPartitions);
                log.info("强制 seek 到 earliest offset: linkTaskId={}", taskId);
            }

            // **记录当前消费位点到文件**（无论是否从文件加载，都记录当前位置）
            for (TopicPartition tp : topicPartitions) {
                long position = consumer.position(tp);
                saveOffset(taskId, tp.topic(), tp.partition(), position);
                log.info("保存初始消费位点: linkTaskId={}, topic={}, partition={}, offset={}", taskId, tp.topic(), tp.partition(), position);
            }

            log.info("Kafka 消费者创建并订阅成功: linkTaskId={}, destination={}", taskId, destination);

            // 启动消费线程
            AtomicBoolean runningFlag = new AtomicBoolean(true);
            consumerRunningFlagsMap.put(taskId, runningFlag);
            consumerCacheMap.put(taskId, consumer);

            //todo 线程数设计成可配置
            ExecutorService consumerExecutor = Executors.newFixedThreadPool(1);
            // 启动消费者线程
            consumerExecutor.submit(() -> {
                consumeMessages(taskId, consumer, runningFlag, messageHandler, onStopped);
            });
            consumerExecutorMap.put(taskId, consumerExecutor);

            log.info("动态 Kafka 消费者启动成功: linkTaskId={}, topic={}, groupId={}", taskId, destination, groupId);

        } catch (Exception e) {
            log.error("linkTaskId=" + taskId, e);
            throw new DataStreamException("xxxxx", e.getMessage());
        }
    }

    /**
     * 消费者消息处理线程
     */
    private void consumeMessages(Long taskId, KafkaConsumer<String, String> consumer, AtomicBoolean runningFlag, ConsumerMessageHandler messageHandler, Runnable onStopped) {
        log.info("启动消费者线程: linkTaskId={}", taskId);

        int emptyCount = 0; // 记录连续空轮询次数

        try {
            while (runningFlag.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000)); // 增加到3秒

                if (!records.isEmpty()) {
                    emptyCount = 0; // 重置空轮询计数
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            // 调用消息处理回调
                            messageHandler.handleMessage(record.value(), record.topic(), record.partition(), record.offset());
                        } catch (Exception e) {
                            log.error("处理消息失败: linkTaskId={}, topic={}, partition={}, offset={}, value={}", taskId, record.topic(), record.partition(), record.offset(), record.value(), e);
                            // 消息处理失败时停止消费
                            runningFlag.set(false);
                            return;
                        }
                        // 处理成功后保存 offset
                        saveOffset(taskId, record.topic(), record.partition(), record.offset());
                    }
                } else {
                    emptyCount++;
                    if (emptyCount % 10 == 0) { // 每10次空轮询记录一次日志
                        log.info("连续 {} 次 poll 未获取到消息: linkTaskId={}", emptyCount, taskId);
                    } else {
                        log.trace("无消息: linkTaskId={}", taskId);
                    }
                }
            }
        } catch (Exception e) {
            log.error("消费者线程异常: linkTaskId={}", taskId, e);
        } finally {
            consumer.close();
            log.info("消费者线程结束: linkTaskId={}", taskId);
            // 调用停止回调
            if (onStopped != null) {
                onStopped.run();
            }
        }
    }

    /**
     * 解绑消费者目的地
     */
    public void unbindConsumerDestination(Long taskId) throws DataStreamException {
        try {
            // 停止消费者线程
            if (consumerRunningFlagsMap.containsKey(taskId)) {
                AtomicBoolean runningFlag = consumerRunningFlagsMap.remove(taskId);
                if (runningFlag != null) {
                    runningFlag.set(false);
                }
            } else {
                log.info("消费者consumerRunningFlagsMap不存在, linkTaskId=" + taskId);
            }

            if (consumerCacheMap.containsKey(taskId)) {
                // 移除消费者
                KafkaConsumer<String, String> consumer = consumerCacheMap.remove(taskId);
                if (consumer != null) {
                    consumer.wakeup(); // 唤醒阻塞的poll操作
                }
            } else {
                log.info("消费者consumerCacheMap不存在, linkTaskId=" + taskId);
            }

            if (consumerExecutorMap.containsKey(taskId)) {
                //再关闭
                ExecutorService consumerExecutor = consumerExecutorMap.remove(taskId);
                // 关闭线程池
                consumerExecutor.shutdown();
                try {
                    if (!consumerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        consumerExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    consumerExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            } else {
                log.info("消费者consumerExecutorMap不存在, linkTaskId=" + taskId);
            }

            log.info("动态 Kafka 消费者已停止: linkTaskId={}", taskId);
        } catch (Exception e) {
            log.error("linkTaskId=" + taskId, e);
            throw new DataStreamException("xxxxx", e.getMessage());
        }
    }


    /**
     * 应用关闭时清理
     */
    @PreDestroy
    public void cleanup() {
        log.info("开始清理Kafka连接资源...");

        // 清理所有缓存的连接
        for (Long taskId : new ArrayList<>(producerCache.keySet())) {
            try {
                producerCache.remove(taskId);
                DefaultKafkaProducerFactory<String, String> factory = producerFactoryCache.remove(taskId);
                if (factory != null) {
                    factory.destroy();
                }
                log.info("清理Kafka连接: linkTaskId={}", taskId);
            } catch (Exception e) {
                log.error("清理Kafka连接失败: linkTaskId={}", taskId, e);
            }
        }

        // 清理消费者资源
        for (Long taskId : new ArrayList<>(consumerExecutorMap.keySet())) {
            log.info("清理Kafka consumer begin: linkTaskId={}", taskId);
            try {
                unbindConsumerDestination(taskId);
            } catch (DataStreamException e) {
                log.error("清理Kafka连接失败: linkTaskId={}", taskId, e);
            } catch (Exception e) {
                log.error("清理Kafka连接失败: linkTaskId={}", taskId, e);
            }
            log.info("清理Kafka consumer end: linkTaskId={}", taskId);
        }

        log.info("Kafka连接资源清理完成");
    }

    /**
     * 获取 offset 文件路径
     * 格式: kafkaOffset/{linkTaskId}_{topic}_{partition}.offset
     */
    private String getOffsetFilePath(Long taskId, String topic, int partition) {
        return "kafkaOffset/" + taskId + "_" + topic + "_" + partition + ".offset";
    }

    /**
     * 加载 offset 文件
     */
    private Long loadOffset(Long taskId, String topic, int partition) {
        String filePath = getOffsetFilePath(taskId, topic, partition);
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            log.info("Offset 文件不存在，创建新文件: {}", filePath);
            try {
                // 确保父目录存在
                file.getParentFile().mkdirs();
                // 创建新文件并写入初始值 0
                java.nio.file.Files.write(file.toPath(), "0".getBytes());
                return 0L;
            } catch (Exception e) {
                log.warn("创建 offset 文件失败: {}", filePath, e.getMessage());
                return null;
            }
        }
        try {
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            if (!lines.isEmpty()) {
                String offsetStr = lines.get(0).trim();
                Long offset = Long.parseLong(offsetStr);
                log.info("加载 offset: linkTaskId={}, topic={}, partition={}, offset={}", taskId, topic, partition, offset);
                return offset;
            }
        } catch (Exception e) {
            log.warn("加载 offset 失败: {}", filePath, e.getMessage());
        }
        return null;
    }

    /**
     * 保存 offset 文件
     */
    private void saveOffset(Long taskId, String topic, int partition, long offset) {
        String filePath = getOffsetFilePath(taskId, topic, partition);
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filePath), String.valueOf(offset).getBytes());
            log.debug("保存 offset: linkTaskId={}, topic={}, partition={}, offset={}", taskId, topic, partition, offset);
        } catch (Exception e) {
            log.warn("保存 offset 失败: {}", filePath, e.getMessage());
        }
    }

    public void testMqConnection(String bootstrapServers) throws DataStreamException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

        try (AdminClient adminClient = AdminClient.create(props)) {
            adminClient.describeCluster().clusterId().get(5, TimeUnit.SECONDS);
            log.info("MQ连接测试成功，服务地址：{}", bootstrapServers);
        } catch (Exception e) {
            log.error("MQ连接测试失败，服务地址：{}，错误：{}", bootstrapServers, e.getMessage());
            throw new DataStreamException("MQ_007", "MQ连接失败：" + e.getMessage());
        }
    }
}
