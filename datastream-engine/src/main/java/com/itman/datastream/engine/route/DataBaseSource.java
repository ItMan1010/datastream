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
package com.itman.datastream.engine.route;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.itman.datastream.common.utils.AESUtils;
import com.itman.datastream.common.entity.DataBaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.itman.datastream.common.constant.DataStreamConstant.SOURCE_DATA_META_DATA_KEY_NAME;

@Slf4j
public class DataBaseSource extends AbstractRoutingDataSource {
    /**
     * 连接池映射（key: dataSourceKey, value: DruidDataSource）
     */
    private final Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();
    /**
     * 连接池信息映射（key: dataSourceKey, value: PoolInfo）
     */
    private final Map<String, PoolInfo> poolInfoMap = new ConcurrentHashMap<>();
    /**
     * 任务到连接池的映射（key: linkTaskId, value: Set<dataSourceKey>）
     * 用于快速查找任务使用的所有连接池
     */
    private final Map<Long, Set<String>> taskToPoolsMap = new ConcurrentHashMap<>();
    /**
     * 定时清理任务执行器
     */
    private ScheduledExecutorService cleanupExecutor;

    /**
     * 连接池空闲超时时间（默认30分钟）
     */
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 30 * 60 * 1000L;

    /**
     * 清理任务执行间隔（默认5分钟）
     */
    private static final long CLEANUP_INTERVAL_MS = 5 * 60 * 1000L;


    @Override
    protected Object determineCurrentLookupKey() {
        return RouteHolder.getRouteKey();
    }

    @Override
    public void afterPropertiesSet() {
        setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    public DataBaseSource(javax.sql.DataSource defaultTargetDataSource) {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(new ConcurrentHashMap<Object, Object>() {{
            put(SOURCE_DATA_META_DATA_KEY_NAME, defaultTargetDataSource);
        }});
        super.afterPropertiesSet();
    }

    /**
     * 添加数据库连接池（支持任务ID跟踪）
     *
     * @param dataBaseList 数据库实体列表
     * @param taskId       任务ID（可选，如果为null则不跟踪任务）
     */
    public void addDataBase(List<DataBaseEntity> dataBaseList, Long taskId) {
        for (DataBaseEntity iterator : dataBaseList) {
            //不同任务连接池必须共享，这样可以控制多个任务对同一个库连接池数创建
            //但这样就必须维持好所有任务线程数据和连接池数据合理关系
            String dataSourceKey = iterator.getKeyName() + "_" + iterator.getDataBaseId();

            // 检查连接池是否已存在
            PoolInfo poolInfo = poolInfoMap.get(dataSourceKey);
            if (poolInfo != null) {
                // 连接池已存在，增加引用计数
                if (taskId != null) {
                    poolInfo.incrementRef(taskId);
                    taskToPoolsMap.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(dataSourceKey);
                }
                continue;
            }

            DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
            dataSource.setDriverClassName(iterator.getDriverClass());
            dataSource.setUrl(iterator.getUrl());
            dataSource.setUsername(iterator.getUserName());
            dataSource.setPassword(AESUtils.decrypt(iterator.getPassWord()));
            dataSource.setInitialSize(iterator.getDataPoolCount());
            dataSource.setMaxActive(iterator.getDataPoolCount());
            dataSource.setMinIdle(iterator.getDataBaseMinIdle() == null ? 0 : iterator.getDataBaseMinIdle());
            dataSource.setMaxWait(iterator.getDataBaseMaxWait() == null ? 6000 : iterator.getDataBaseMaxWait());
            dataSource.setTimeBetweenEvictionRunsMillis(iterator.getDataBaseTimeBetweenEvictionRunsMillis() == null ? 5000 : iterator.getDataBaseTimeBetweenEvictionRunsMillis());
            dataSource.setMinEvictableIdleTimeMillis(iterator.getDataBaseMinEvictableIdleTimeMillis() == null ? 40000 : iterator.getDataBaseMinEvictableIdleTimeMillis());
            dataSource.setValidationQuery(iterator.getSqlValidationQuery());
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(true);
            dataSource.setTestOnReturn(false);
            dataSource.setConnectionErrorRetryAttempts(3);
            dataSource.setBreakAfterAcquireFailure(true);

            // 创建PoolInfo并初始化引用计数
            poolInfo = new PoolInfo(dataSource, dataSourceKey);
            if (taskId != null) {
                poolInfo.incrementRef(taskId);
                taskToPoolsMap.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(dataSourceKey);
            }

            targetDataSources.put(dataSourceKey, dataSource);
            poolInfoMap.put(dataSourceKey, poolInfo);
        }

        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * 释放任务使用的连接池引用
     * 当任务完成时调用此方法，减少连接池的引用计数
     *
     * @param taskId 任务ID
     */
    public void releaseTaskDataSources(Long taskId) {
        if (taskId == null) {
            return;
        }

        Set<String> poolKeys = taskToPoolsMap.remove(taskId);
        if (poolKeys == null || poolKeys.isEmpty()) {
            return;
        }

        for (String dataSourceKey : poolKeys) {
            PoolInfo poolInfo = poolInfoMap.get(dataSourceKey);
            if (poolInfo != null) {
                int refCount = poolInfo.decrementRef(taskId);
                if (refCount == 0) {
                    // 引用计数为0，更新最后使用时间，等待定时清理
                    log.debug("连接池[{}]引用计数为0，等待清理", dataSourceKey);
                }
            }
        }
    }

    /**
     * 初始化定时清理任务
     */
    @PostConstruct
    public void initCleanupTask() {
        if (cleanupExecutor == null) {
            cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
                Thread thread = new Thread(r, "DataBaseSource-Cleanup");
                thread.setDaemon(true);
                return thread;
            });

            // 启动定时清理任务
            cleanupExecutor.scheduleWithFixedDelay(
                    this::cleanupIdlePools,
                    CLEANUP_INTERVAL_MS,
                    CLEANUP_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );

            log.info("DataBaseSource定时清理任务已启动，清理间隔: {}分钟",
                    CLEANUP_INTERVAL_MS / 60000);
        }
    }

    /**
     * 清理空闲连接池
     */
    private void cleanupIdlePools() {
        try {
            List<String> poolsToClean = new ArrayList<>();

            // 找出可以清理的连接池
            for (Map.Entry<String, PoolInfo> entry : poolInfoMap.entrySet()) {
                PoolInfo poolInfo = entry.getValue();
                if (poolInfo.canBeCleaned(DEFAULT_IDLE_TIMEOUT_MS)) {
                    poolsToClean.add(entry.getKey());
                }
            }

            // 清理连接池
            for (String dataSourceKey : poolsToClean) {
                try {
                    removeDataBase(dataSourceKey);
                    log.info("定时清理：已关闭空闲连接池[{}]", dataSourceKey);
                } catch (Exception e) {
                    log.warn("清理连接池[{}]失败: {}", dataSourceKey, e.getMessage());
                }
            }

            if (!poolsToClean.isEmpty()) {
                log.info("定时清理完成，共清理{}个连接池", poolsToClean.size());
            }
        } catch (Exception e) {
            log.error("定时清理连接池时发生异常", e);
        }
    }

    /**
     * 移除并关闭指定的数据源连接池
     *
     * @param dataSourceKey 数据源key（keyName_dataBaseId）
     */
    public void removeDataBase(String dataSourceKey) {
        Object dataSource = targetDataSources.remove(dataSourceKey);
        // 同时清理 poolInfoMap
        poolInfoMap.remove(dataSourceKey);
        // 清理 taskToPoolsMap 中的引用
        taskToPoolsMap.values().removeIf(poolKeys -> poolKeys.remove(dataSourceKey) && poolKeys.isEmpty());
        
        if (dataSource instanceof DruidDataSource) {
            try {
                ((DruidDataSource) dataSource).close();
            } catch (Exception e) {
                log.warn("关闭连接池[{}]失败: {}", dataSourceKey, e.getMessage());
            }
        }
        super.setTargetDataSources(targetDataSources);
        super.afterPropertiesSet();
    }

    /**
     * 关闭所有Druid连接池（用于应用关闭时清理资源）
     */
    public void closeAllDataSources() {
        for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
            Object dataSource = entry.getValue();
            if (dataSource instanceof DruidDataSource) {
                try {
                    ((DruidDataSource) dataSource).close();
                } catch (Exception e) {
                    // 关闭失败记录日志但不抛出异常
                }
            }
        }
        targetDataSources.clear();
    }

    /**
     * 获取所有Druid连接池的统计信息（增强版）
     * 同时检查 targetDataSources 和 poolInfoMap，确保所有连接池都能被统计
     * 包含引用计数、使用任务等信息
     */
    public List<Map<String, Object>> getAllDruidPoolStats() {
        // 预分配容量，使用 targetDataSources 的大小（可能比 poolInfoMap 大）
        List<Map<String, Object>> statsList = new ArrayList<>(targetDataSources.size());
        
        // 用于记录已处理的连接池，避免重复
        Set<String> processedKeys = ConcurrentHashMap.newKeySet();
        
        // 1. 先处理 poolInfoMap 中的连接池（有监控信息的）
        for (Map.Entry<String, PoolInfo> entry : poolInfoMap.entrySet()) {
            try {
                PoolInfo poolInfo = entry.getValue();
                DruidDataSource druidDataSource = poolInfo.getDataSource();
                String dataSourceKey = entry.getKey();
                
                Map<String, Object> stats = buildPoolStats(druidDataSource, dataSourceKey, poolInfo);
                statsList.add(stats);
                processedKeys.add(dataSourceKey);
            } catch (Exception e) {
                log.warn("获取连接池[{}]统计信息失败: {}", entry.getKey(), e.getMessage());
            }
        }
        
        // 2. 处理 targetDataSources 中但不在 poolInfoMap 中的连接池（兼容旧代码）
        for (Map.Entry<Object, Object> entry : targetDataSources.entrySet()) {
            try {
                String dataSourceKey = (String) entry.getKey();
                // 跳过已处理的连接池和特殊key
                if (processedKeys.contains(dataSourceKey) || SOURCE_DATA_META_DATA_KEY_NAME.equals(dataSourceKey)) {
                    continue;
                }
                
                Object dataSource = entry.getValue();
                if (!(dataSource instanceof DruidDataSource)) {
                    continue;
                }
                
                DruidDataSource druidDataSource = (DruidDataSource) dataSource;
                Map<String, Object> stats = buildPoolStats(druidDataSource, dataSourceKey, null);
                statsList.add(stats);
            } catch (Exception e) {
                log.warn("获取连接池[{}]统计信息失败: {}", entry.getKey(), e.getMessage());
            }
        }
        
        return statsList;
    }

    /**
     * 构建连接池统计信息
     */
    private Map<String, Object> buildPoolStats(DruidDataSource druidDataSource, String dataSourceKey, PoolInfo poolInfo) {
        Map<String, Object> stats = new HashMap<>();
        
        // 基本信息
        stats.put("dataSourceKey", dataSourceKey);
        
        // JDBC URL（脱敏处理）
        String jdbcUrl = druidDataSource.getUrl();
        if (jdbcUrl != null && jdbcUrl.length() > 50) {
            jdbcUrl = jdbcUrl.substring(0, 50) + "...";
        }
        stats.put("jdbcUrl", jdbcUrl);
        
        // 连接池统计信息
        int activeCount = druidDataSource.getActiveCount();
        int poolingCount = druidDataSource.getPoolingCount();
        int maxActive = druidDataSource.getMaxActive();
        
        stats.put("activeConnections", activeCount);
        stats.put("idleConnections", poolingCount);  // 添加缺失的字段
        stats.put("poolingCount", poolingCount);
        stats.put("maxActive", maxActive);
        stats.put("totalConnections", activeCount + poolingCount);
        
        // 计算使用率和状态
        double usageRate = maxActive > 0 ? (double) activeCount / maxActive : 0.0;
        stats.put("usageRate", usageRate);  // 添加缺失的字段
        
        String status = "正常";
        if (usageRate > 0.9) {
            status = "异常";
        } else if (usageRate > 0.7) {
            status = "告警";
        }
        stats.put("status", status);  // 添加缺失的字段
        
        // 监控增强信息（如果有 PoolInfo）
        if (poolInfo != null) {
            stats.put("referenceCount", poolInfo.getReferenceCount());
            stats.put("taskCount", poolInfo.getTaskIds().size());
            stats.put("taskIds", new ArrayList<>(poolInfo.getTaskIds()));
            stats.put("createTime", poolInfo.getCreateTime());
            stats.put("lastAccessTime", poolInfo.getLastAccessTime());
            stats.put("idleTimeMs", System.currentTimeMillis() - poolInfo.getLastAccessTime());
        } else {
            // 没有 PoolInfo 的情况，设置默认值
            stats.put("referenceCount", 0);
            stats.put("taskCount", 0);
            stats.put("taskIds", new ArrayList<>());
            stats.put("createTime", 0L);
            stats.put("lastAccessTime", 0L);
            stats.put("idleTimeMs", 0L);
        }
        
        return stats;
    }
}
