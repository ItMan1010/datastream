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
package com.itman.datastream.admin.collector;

import com.itman.datastream.admin.controller.domain.response.*;
import com.itman.datastream.engine.holder.DataStreamHolder;
import com.itman.datastream.engine.jdbc.ConnectionPoolConfig;
import com.itman.datastream.engine.jdbc.ConnectionPoolManager;
import com.itman.datastream.engine.route.DataBaseSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static com.itman.datastream.common.constant.DataStreamConstant.*;

/**
 * 资源指标采集器
 * 负责采集系统各种资源的监控指标（只读操作，不修改任何资源状态）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceMetricsCollector {

    private final DataStreamHolder dataStreamHolder;
    private final ConnectionPoolManager connectionPoolManager;
    private final DataBaseSource dataBaseSource;
    private final ApplicationContext applicationContext;

    /**
     * 采集所有资源指标（轻量级，只读操作）
     */
    public ResourceMetricsResponse collectAllMetrics() {
        ResourceMetricsResponse response = new ResourceMetricsResponse();

        try {
            // 系统级指标
            response.setSystemMetrics(collectSystemMetrics());

            // 任务级指标
            response.setTaskMetrics(collectTaskMetrics());

            // 连接池指标
            response.setConnectionMetrics(collectConnectionPoolMetrics());

            // 线程池指标
            response.setThreadPoolMetrics(collectThreadPoolMetrics());

            // 队列指标
            response.setQueueMetrics(collectQueueMetrics());

            response.setTimestamp(new Timestamp(System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("采集资源指标失败", e);
            response.setErrorCode("COLLECT_METRICS_ERROR");
            response.setErrorMsg("采集资源指标失败: " + e.getMessage());
        }

        return response;
    }

    /**
     * 采集系统级指标
     */
    public SystemResourceMetrics collectSystemMetrics() {
        SystemResourceMetrics metrics = new SystemResourceMetrics();

        try {
            // 运行中任务数
            Integer runningTaskCount = DataStreamHolder.getTaskRunningSize();
            metrics.setRunningTaskCount(runningTaskCount);

            // JVM内存
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
            metrics.setJvmMemoryUsed(usedMemory);
            metrics.setTotalMemoryUsed(totalMemory);
            
            // 计算内存使用率
            double memoryUsageRate = totalMemory > 0 ? (double) usedMemory / totalMemory : 0.0;
            metrics.setMemoryUsageRate(memoryUsageRate);

            // 连接池统计（包括ConnectionPoolManager和DataBaseSource）
            int totalConnections = 0;
            int activeConnections = 0;
            int maxPoolSize = 0;
            
            // 统计ConnectionPoolManager的连接池
            List<Map<String, Object>> poolDetails = connectionPoolManager.getAllPoolDetails();
            for (Map<String, Object> detail : poolDetails) {
                totalConnections += (Integer) detail.getOrDefault("totalConnections", 0);
                activeConnections += (Integer) detail.getOrDefault("activeConnections", 0);
                maxPoolSize += (Integer) detail.getOrDefault("maxPoolSize", 0);
            }
            
            // 统计DataBaseSource的Druid连接池
            List<Map<String, Object>> druidPoolStats = dataBaseSource.getAllDruidPoolStats();
            for (Map<String, Object> druidStats : druidPoolStats) {
                totalConnections += (Integer) druidStats.getOrDefault("totalConnections", 0);
                activeConnections += (Integer) druidStats.getOrDefault("activeConnections", 0);
                maxPoolSize += (Integer) druidStats.getOrDefault("maxActive", 0);
            }
            
            metrics.setTotalConnectionCount(totalConnections);
            metrics.setActiveConnectionCount(activeConnections);

            // 计算连接使用率（基于所有连接池的最大连接数总和）
            double connectionUsageRate = maxPoolSize > 0 ? (double) activeConnections / maxPoolSize : 0.0;
            metrics.setConnectionUsageRate(connectionUsageRate);

            // 总任务数（暂时使用运行中任务数，后续可以从数据库获取）
            metrics.setTotalTaskCount(runningTaskCount);

        } catch (Exception e) {
            log.error("采集系统级指标失败", e);
        }

        return metrics;
    }

    /**
     * 采集任务级指标
     */
    public TaskResourceMetrics collectTaskMetrics() {
        TaskResourceMetrics metrics = new TaskResourceMetrics();
        List<TaskResourceDetail> details = new ArrayList<>();

        try {
            // 获取所有运行中的任务
            List<Long> runningTasks = DataStreamHolder.getTaskRunningList();

            for (Long taskId : runningTasks) {
                try {
                    TaskResourceDetail detail = new TaskResourceDetail();
                    detail.setTaskId(taskId);

                    // 队列大小
                    Integer queueSize = dataStreamHolder.getQueueRunningSize(taskId);
                    detail.setQueueSize(queueSize);

                    // 队列最大容量（暂时使用默认值，后续可以从配置获取）
                    detail.setQueueMaxSize(1000);

                    // 计算队列使用率
                    double queueUsageRate = detail.getQueueMaxSize() > 0 
                            ? (double) queueSize / detail.getQueueMaxSize() : 0.0;
                    detail.setQueueUsageRate(queueUsageRate);

                    // 状态
                    detail.setStatus("运行中");

                    // 其他指标暂时设为默认值
                    detail.setSourceThreadCount(0);
                    detail.setTargetThreadCount(0);
                    detail.setDataProcessed(0L);

                    details.add(detail);
                } catch (Exception e) {
                    log.warn("采集任务[{}]指标失败: {}", taskId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("采集任务级指标失败", e);
        }

        metrics.setTaskDetails(details);
        return metrics;
    }

    /**
     * 采集连接池指标
     * 包括：ConnectionPoolManager管理的连接池 + DataBaseSource管理的Druid连接池
     * 优化：减少临时对象创建，使用预分配容量
     */
    public ConnectionPoolMetrics collectConnectionPoolMetrics() {
        ConnectionPoolMetrics metrics = new ConnectionPoolMetrics();
        // 预分配容量，减少扩容时的内存分配
        List<ConnectionPoolDetail> details = new ArrayList<>(16);

        try {
            // 1. 采集ConnectionPoolManager管理的连接池
            List<Map<String, Object>> poolDetails = connectionPoolManager.getAllPoolDetails();
            if (poolDetails != null && !poolDetails.isEmpty()) {
                for (Map<String, Object> poolDetail : poolDetails) {
                    ConnectionPoolDetail detail = new ConnectionPoolDetail();
                    detail.setJdbcUrl((String) poolDetail.get("jdbcUrl"));
                    detail.setTotalConnections((Integer) poolDetail.getOrDefault("totalConnections", 0));
                    detail.setIdleConnections((Integer) poolDetail.getOrDefault("idleConnections", 0));
                    detail.setActiveConnections((Integer) poolDetail.getOrDefault("activeConnections", 0));
                    detail.setMaxPoolSize((Integer) poolDetail.getOrDefault("maxPoolSize", 0));
                    detail.setUsageRate((Double) poolDetail.getOrDefault("usageRate", 0.0));
                    detail.setStatus((String) poolDetail.getOrDefault("status", "正常"));
                    detail.setMaxLifetimeMs(((Number) poolDetail.getOrDefault("maxLifetimeMs", 0L)).longValue());
                    detail.setAvgBorrowDurationMs(((Number) poolDetail.getOrDefault("avgBorrowDurationMs", 0L)).longValue());
                    details.add(detail);
                }
            }

            // 2. 采集DataBaseSource管理的Druid连接池
            List<Map<String, Object>> druidPoolStats = dataBaseSource.getAllDruidPoolStats();
            if (druidPoolStats != null && !druidPoolStats.isEmpty()) {
                for (Map<String, Object> druidStats : druidPoolStats) {
                    ConnectionPoolDetail detail = new ConnectionPoolDetail();
                    // 数据源标识（keyName_dataBaseId）
                    String dataSourceKey = (String) druidStats.get("dataSourceKey");
                    String jdbcUrl = (String) druidStats.get("jdbcUrl");
                    // 使用数据源key作为标识，如果没有jdbcUrl则使用key
                    detail.setJdbcUrl(jdbcUrl != null ? jdbcUrl : ("DruidPool:" + dataSourceKey));
                    
                    detail.setTotalConnections((Integer) druidStats.getOrDefault("totalConnections", 0));
                    detail.setIdleConnections((Integer) druidStats.getOrDefault("idleConnections", 0));
                    detail.setActiveConnections((Integer) druidStats.getOrDefault("activeConnections", 0));
                    detail.setMaxPoolSize((Integer) druidStats.getOrDefault("maxActive", 0));
                    detail.setUsageRate((Double) druidStats.getOrDefault("usageRate", 0.0));
                    detail.setStatus((String) druidStats.getOrDefault("status", "正常"));
                    // Druid连接池没有maxLifetimeMs和avgBorrowDurationMs，设为0
                    detail.setMaxLifetimeMs(0L);
                    detail.setAvgBorrowDurationMs(0L);
                    details.add(detail);
                }
            }
        } catch (Exception e) {
            log.error("采集连接池指标失败", e);
        }

        metrics.setPoolDetails(details);
        return metrics;
    }

    /**
     * 采集线程池指标
     */
    public ThreadPoolMetrics collectThreadPoolMetrics() {
        ThreadPoolMetrics metrics = new ThreadPoolMetrics();
        List<ThreadPoolDetail> details = new ArrayList<>();

        try {
            // 采集各个线程池的指标
            String[] poolNames = {
                    SOURCE_WORKS_POOL_EXECUTOR,
                    TARGET_WORKS_POOL_EXECUTOR,
                    TASK_WORKS_POOL_EXECUTOR,
                    EVENT_WORKS_POOL_EXECUTOR
            };

            for (String poolName : poolNames) {
                try {
                    ThreadPoolDetail detail = collectThreadPoolDetail(poolName);
                    if (detail != null) {
                        details.add(detail);
                    }
                } catch (Exception e) {
                    log.warn("采集线程池[{}]指标失败: {}", poolName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("采集线程池指标失败", e);
        }

        metrics.setPoolDetails(details);
        return metrics;
    }

    /**
     * 采集单个线程池的指标
     */
    private ThreadPoolDetail collectThreadPoolDetail(String poolName) {
        try {
            Object executorBean = applicationContext.getBean(poolName);
            if (!(executorBean instanceof ThreadPoolTaskExecutor)) {
                log.warn("线程池[{}]不是ThreadPoolTaskExecutor类型", poolName);
                return null;
            }

            ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) executorBean;
            ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
            if (threadPoolExecutor == null) {
                log.warn("线程池[{}]的ThreadPoolExecutor为null", poolName);
                return null;
            }

            ThreadPoolDetail detail = new ThreadPoolDetail();
            detail.setPoolName(poolName);
            detail.setCorePoolSize(threadPoolExecutor.getCorePoolSize());
            detail.setMaxPoolSize(threadPoolExecutor.getMaximumPoolSize());
            detail.setActiveThreads(threadPoolExecutor.getActiveCount());
            detail.setQueueSize(threadPoolExecutor.getQueue().size());
            detail.setQueueCapacity(threadPoolExecutor.getQueue().remainingCapacity() + threadPoolExecutor.getQueue().size());
            detail.setCompletedTaskCount(threadPoolExecutor.getCompletedTaskCount());
            detail.setRejectedTaskCount(0L); // ThreadPoolExecutor不提供拒绝任务数，设为0

            // 计算使用率
            int maxPoolSize = detail.getMaxPoolSize();
            double usageRate = maxPoolSize > 0 ? (double) detail.getActiveThreads() / maxPoolSize : 0.0;
            detail.setUsageRate(usageRate);

            // 状态判断
            String status = "正常";
            if (usageRate > 0.9) {
                status = "异常";
            } else if (usageRate > 0.7) {
                status = "告警";
            }
            detail.setStatus(status);

            return detail;
        } catch (Exception e) {
            log.warn("获取线程池[{}]Bean失败: {}", poolName, e.getMessage());
            return null;
        }
    }

    /**
     * 采集队列指标
     */
    public QueueMetrics collectQueueMetrics() {
        QueueMetrics metrics = new QueueMetrics();

        try {
            // 汇总所有任务的队列信息
            List<Long> runningTasks = DataStreamHolder.getTaskRunningList();
            int totalSize = 0;
            int totalCapacity = 0;

            for (Long taskId : runningTasks) {
                try {
                    Integer queueSize = dataStreamHolder.getQueueRunningSize(taskId);
                    totalSize += queueSize != null ? queueSize : 0;
                    totalCapacity += 1000; // 假设每个任务队列最大容量为1000
                } catch (Exception e) {
                    log.warn("获取任务[{}]队列大小失败: {}", taskId, e.getMessage());
                }
            }

            metrics.setTotalSize(totalSize);
            metrics.setTotalCapacity(totalCapacity);

            // 计算使用率
            double usageRate = totalCapacity > 0 ? (double) totalSize / totalCapacity : 0.0;
            metrics.setUsageRate(usageRate);

        } catch (Exception e) {
            log.error("采集队列指标失败", e);
        }

        return metrics;
    }
}
