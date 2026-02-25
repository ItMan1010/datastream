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

import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 专业的数据库连接池管理器
 * <p>
 * 功能特性：
 * <ul>
 *   <li>线程安全的连接获取与归还</li>
 *   <li>连接有效性验证</li>
 *   <li>空闲连接自动清理</li>
 *   <li>连接泄漏检测</li>
 *   <li>连接最大存活时间控制</li>
 *   <li>获取连接超时控制</li>
 *   <li>连接池统计信息</li>
 * </ul>
 * <p>
 * 使用jdbcUrl作为连接池的唯一标识Key
 *
 * @author DataStream
 */
@Slf4j
@Component
public class ConnectionPoolManager {

    /**
     * 连接池配置
     */
    private final ConnectionPoolConfig config;

    /**
     * 按jdbcUrl分组的连接池
     * Key: jdbcUrl, Value: 该数据源的连接列表
     */
    private final ConcurrentHashMap<String, List<ConnectionWrapper>> connectionPools;

    /**
     * 数据源连接凭证缓存
     * Key: jdbcUrl, Value: 连接凭证信息
     */
    private final ConcurrentHashMap<String, ConnectionCredentials> credentialsMap;

    /**
     * 每个数据源的锁，用于创建连接时的同步
     */
    private final ConcurrentHashMap<String, ReentrantLock> poolLocks;

    /**
     * 定时任务执行器，用于空闲连接清理和泄漏检测
     */
    private ScheduledExecutorService scheduledExecutor;

    /**
     * 连接池是否已关闭
     */
    private volatile boolean closed = false;

    /**
     * 连接凭证信息内部类
     */
    private static class ConnectionCredentials {
        final String username;
        final String password;

        ConnectionCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /**
     * 默认构造函数，使用默认配置
     */
    public ConnectionPoolManager() {
        this(ConnectionPoolConfig.defaultConfig());
    }

    /**
     * 带配置的构造函数
     *
     * @param config 连接池配置
     */
    public ConnectionPoolManager(ConnectionPoolConfig config) {
        this.config = config;
        this.config.validate();
        this.connectionPools = new ConcurrentHashMap<>();
        this.credentialsMap = new ConcurrentHashMap<>();
        this.poolLocks = new ConcurrentHashMap<>();
    }

    /**
     * 初始化连接池管理器
     */
    @PostConstruct
    public void init() {
        // 创建定时任务执行器
        scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread thread = new Thread(r, "ConnectionPool-Monitor");
            thread.setDaemon(true);
            return thread;
        });

        // 启动空闲连接清理任务
        scheduledExecutor.scheduleWithFixedDelay(
                this::cleanupIdleConnections,
                config.getIdleCheckIntervalMs(),
                config.getIdleCheckIntervalMs(),
                TimeUnit.MILLISECONDS
        );

        // 如果启用了泄漏检测，启动泄漏检测任务
        if (config.getLeakDetectionThresholdMs() > 0) {
            scheduledExecutor.scheduleWithFixedDelay(
                    this::detectLeakedConnections,
                    config.getLeakDetectionThresholdMs(),
                    config.getLeakDetectionThresholdMs() / 2,
                    TimeUnit.MILLISECONDS
            );
        }

        log.info("ConnectionPoolManager initialized with config: maxPoolSize={}, minIdleSize={}, " +
                        "connectionTimeoutMs={}, maxIdleTimeMs={}, maxLifetimeMs={}",
                config.getMaxPoolSize(), config.getMinIdleSize(),
                config.getConnectionTimeoutMs(), config.getMaxIdleTimeMs(), config.getMaxLifetimeMs());
    }

    /**
     * 获取数据库连接
     * <p>
     * 此方法会：
     * <ol>
     *   <li>优先从连接池中获取空闲且有效的连接</li>
     *   <li>如果没有可用连接且未达到最大连接数，创建新连接</li>
     *   <li>如果连接池已满，等待直到超时</li>
     * </ol>
     *
     * @param jdbcUrl  JDBC URL（作为连接池的唯一标识）
     * @param username 用户名
     * @param password 密码
     * @return 数据库连接
     * @throws DataStreamException 获取连接失败时抛出
     */
    public Connection getConnection(String jdbcUrl, String username, String password)
            throws DataStreamException {
        // 参数校验
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            throw new DataStreamException(ConnectionPoolErrorCode.CONNECTION_CREATE_FAILED,
                    new IllegalArgumentException("jdbcUrl cannot be null or empty"));
        }
        
        if (closed) {
            throw new DataStreamException(ConnectionPoolErrorCode.CONNECTION_CLOSED);
        }

        // 缓存连接凭证
        credentialsMap.computeIfAbsent(jdbcUrl, k -> new ConnectionCredentials(username, password));

        long startTime = System.currentTimeMillis();
        long timeout = config.getConnectionTimeoutMs();

        while (System.currentTimeMillis() - startTime < timeout) {
            // 尝试从池中获取空闲连接
            ConnectionWrapper wrapper = tryGetIdleConnection(jdbcUrl);
            if (wrapper != null) {
                log.debug("Got idle connection from pool for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
                return wrapper.getConnection();
            }

            // 尝试创建新连接
            wrapper = tryCreateConnection(jdbcUrl, username, password);
            if (wrapper != null) {
                log.info("Created new connection for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
                return wrapper.getConnection();
            }

            // 连接池已满，等待一段时间后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DataStreamException(ConnectionPoolErrorCode.CONNECTION_TIMEOUT);
            }
        }

        // 超时，抛出异常
        log.error("Connection timeout for jdbcUrl={}, waited {}ms", maskJdbcUrl(jdbcUrl), timeout);
        throw new DataStreamException(ConnectionPoolErrorCode.CONNECTION_TIMEOUT);
    }

    /**
     * 尝试从连接池获取空闲连接
     */
    private ConnectionWrapper tryGetIdleConnection(String jdbcUrl) {
        List<ConnectionWrapper> pool = connectionPools.get(jdbcUrl);
        if (pool == null || pool.isEmpty()) {
            return null;
        }

        synchronized (pool) {
            for (ConnectionWrapper wrapper : pool) {
                if (wrapper.isIdle() && !wrapper.isClosedOrInvalid()) {
                    // 验证连接有效性
                    if (!wrapper.isValid(config.getValidationTimeoutSeconds())) {
                        wrapper.markInvalid();
                        log.warn("Connection invalid, marked for removal: {}", wrapper);
                        continue;
                    }

                    // 检查连接是否超过最大存活时间
                    if (wrapper.getAgeMs() > config.getMaxLifetimeMs()) {
                        wrapper.markInvalid();
                        log.info("Connection exceeded max lifetime, marked for removal: {}", wrapper);
                        continue;
                    }

                    // 尝试借用连接
                    if (wrapper.tryBorrow()) {
                        return wrapper;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 尝试创建新连接
     */
    private ConnectionWrapper tryCreateConnection(String jdbcUrl, String username, String password) 
            throws DataStreamException {
        // 获取该数据源的锁
        ReentrantLock lock = poolLocks.computeIfAbsent(jdbcUrl, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            return null; // 其他线程正在创建连接，返回null让调用者重试
        }

        try {
            List<ConnectionWrapper> pool = connectionPools.computeIfAbsent(jdbcUrl, k -> new ArrayList<>());

            // 再次检查池大小（double-check）
            synchronized (pool) {
                if (pool.size() >= config.getMaxPoolSize()) {
                    return null; // 已达到最大连接数
                }

                // 创建新连接
                Connection connection;
                try {
                    connection = DriverManager.getConnection(jdbcUrl, username, password);
                } catch (SQLException e) {
                    log.error("Failed to create connection for jdbcUrl={}: {}", 
                            maskJdbcUrl(jdbcUrl), e.getMessage());
                    throw new DataStreamException(ConnectionPoolErrorCode.CONNECTION_CREATE_FAILED, e);
                }

                ConnectionWrapper wrapper = new ConnectionWrapper(connection, jdbcUrl);
                wrapper.tryBorrow(); // 标记为使用中
                pool.add(wrapper);

                log.info("New connection created for jdbcUrl={}, pool size={}", 
                        maskJdbcUrl(jdbcUrl), pool.size());
                return wrapper;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 归还连接到连接池
     * <p>
     * 调用此方法将连接归还到池中，使其可以被其他线程复用。
     *
     * @param jdbcUrl    JDBC URL
     * @param connection 要归还的连接
     * @throws DataStreamException 归还失败时抛出
     */
    public void releaseConnection(String jdbcUrl, Connection connection) throws DataStreamException {
        if (connection == null) {
            return;
        }

        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            log.debug("Closing connection (jdbcUrl is null or empty)");
            closeConnectionQuietly(connection);
            return;
        }

        List<ConnectionWrapper> pool = connectionPools.get(jdbcUrl);
        if (pool == null) {
            log.warn("No pool found for jdbcUrl={}, closing connection directly", maskJdbcUrl(jdbcUrl));
            closeConnectionQuietly(connection);
            return;
        }

        synchronized (pool) {
            for (ConnectionWrapper wrapper : pool) {
                if (wrapper.getConnection() == connection) {
                    if (wrapper.release()) {
                        log.debug("Connection released for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
                        return;
                    } else {
                        log.warn("Failed to release connection, current state: {}", wrapper);
                    }
                }
            }
        }

        // 没找到对应的wrapper，可能是外部创建的连接，直接关闭
        log.warn("Connection not found in pool for jdbcUrl={}, closing directly", maskJdbcUrl(jdbcUrl));
        closeConnectionQuietly(connection);
    }

    /**
     * 执行SQL验证查询
     * <p>
     * 获取连接，执行验证SQL，然后自动归还连接。
     *
     * @param sqlValidationQuery 验证SQL语句
     * @param jdbcUrl            JDBC URL
     * @param username           用户名
     * @param password           密码
     * @throws DataStreamException 验证失败时抛出
     */
    public void executeSqlValidationQuery(String sqlValidationQuery, String jdbcUrl, 
                                          String username, String password) 
            throws DataStreamException {
        // 参数校验
        if (sqlValidationQuery == null || sqlValidationQuery.trim().isEmpty()) {
            throw new DataStreamException(ConnectionPoolErrorCode.VALIDATION_FAILED,
                    new IllegalArgumentException("sqlValidationQuery cannot be null or empty"));
        }
        
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Integer resultValue = null;

        try {
            connection = getConnection(jdbcUrl, username, password);
            preparedStatement = connection.prepareStatement(sqlValidationQuery);
            resultSet = preparedStatement.executeQuery();
            
            if (resultSet.next()) {
                resultValue = resultSet.getInt(1);
            }
        } catch (SQLException e) {
            log.error("SQL validation error for jdbcUrl={}: {}", maskJdbcUrl(jdbcUrl), e.getMessage());
            throw new DataStreamException(ConnectionPoolErrorCode.VALIDATION_FAILED, e);
        } finally {
            // 关闭ResultSet和PreparedStatement
            closeQuietly(resultSet);
            closeQuietly(preparedStatement);
            
            // 归还连接到池
            if (connection != null) {
                try {
                    releaseConnection(jdbcUrl, connection);
                } catch (DataStreamException e) {
                    log.warn("Failed to release connection: {}", e.getMessage());
                }
            }
            log.debug("Validation query resources closed for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
        }

        if (resultValue == null || !resultValue.equals(1)) {
            throw new DataStreamException(ConnectionPoolErrorCode.VALIDATION_FAILED);
        }

        log.info("SQL validation successful for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
    }

    /**
     * 清理空闲连接
     * <p>
     * 此方法由定时任务调用，清理超过最大空闲时间的连接。
     */
    public void cleanupIdleConnections() {
        if (closed) {
            return;
        }

        log.debug("Starting idle connection cleanup...");
        int cleanedCount = 0;

        for (Map.Entry<String, List<ConnectionWrapper>> entry : connectionPools.entrySet()) {
            String jdbcUrl = entry.getKey();
            List<ConnectionWrapper> pool = entry.getValue();

            synchronized (pool) {
                Iterator<ConnectionWrapper> iterator = pool.iterator();
                int currentSize = pool.size();

                while (iterator.hasNext()) {
                    ConnectionWrapper wrapper = iterator.next();

                    // 跳过使用中的连接
                    if (wrapper.isInUse()) {
                        continue;
                    }

                    // 检查是否需要清理
                    boolean shouldRemove = false;
                    String reason = null;

                    if (wrapper.isClosedOrInvalid()) {
                        shouldRemove = true;
                        reason = "closed or invalid";
                    } else if (wrapper.getAgeMs() > config.getMaxLifetimeMs()) {
                        shouldRemove = true;
                        reason = "exceeded max lifetime";
                    } else if (wrapper.getIdleTimeMs() > config.getMaxIdleTimeMs() 
                            && currentSize > config.getMinIdleSize()) {
                        shouldRemove = true;
                        reason = "exceeded max idle time";
                    }

                    if (shouldRemove) {
                        wrapper.close();
                        iterator.remove();
                        currentSize--;
                        cleanedCount++;
                        log.info("Cleaned up connection for jdbcUrl={}, reason: {}", 
                                maskJdbcUrl(jdbcUrl), reason);
                    }
                }
            }
        }

        if (cleanedCount > 0) {
            log.info("Idle connection cleanup completed, cleaned {} connections", cleanedCount);
        }
    }

    /**
     * 检测泄漏的连接
     * <p>
     * 此方法由定时任务调用，检测借出时间过长的连接。
     */
    public void detectLeakedConnections() {
        if (closed || config.getLeakDetectionThresholdMs() <= 0) {
            return;
        }

        for (Map.Entry<String, List<ConnectionWrapper>> entry : connectionPools.entrySet()) {
            String jdbcUrl = entry.getKey();
            List<ConnectionWrapper> pool = entry.getValue();

            synchronized (pool) {
                for (ConnectionWrapper wrapper : pool) {
                    if (wrapper.isInUse()) {
                        long borrowDuration = wrapper.getCurrentBorrowDurationMs();
                        if (borrowDuration > config.getLeakDetectionThresholdMs()) {
                            log.warn("Possible connection leak detected! jdbcUrl={}, " +
                                            "borrowDuration={}ms, borrower={}, stackTrace={}",
                                    maskJdbcUrl(jdbcUrl), borrowDuration,
                                    wrapper.getBorrowerThreadName(),
                                    formatStackTrace(wrapper.getBorrowerStackTrace()));
                        }
                    }
                }
            }
        }
    }

    /**
     * 关闭指定jdbcUrl的所有连接
     *
     * @param jdbcUrl JDBC URL
     */
    public void closePool(String jdbcUrl) {
        List<ConnectionWrapper> pool = connectionPools.remove(jdbcUrl);
        credentialsMap.remove(jdbcUrl);
        poolLocks.remove(jdbcUrl);

        if (pool != null) {
            synchronized (pool) {
                for (ConnectionWrapper wrapper : pool) {
                    wrapper.close();
                }
                pool.clear();
            }
            log.info("All connections closed for jdbcUrl={}", maskJdbcUrl(jdbcUrl));
        }
    }

    /**
     * 获取连接池统计信息
     *
     * @param jdbcUrl JDBC URL
     * @return 统计信息字符串
     */
    public String getPoolStats(String jdbcUrl) {
        List<ConnectionWrapper> pool = connectionPools.get(jdbcUrl);
        if (pool == null) {
            return String.format("Pool[%s]: No pool", maskJdbcUrl(jdbcUrl));
        }

        int total, idle, inUse;
        synchronized (pool) {
            total = pool.size();
            idle = (int) pool.stream().filter(ConnectionWrapper::isIdle).count();
            inUse = (int) pool.stream().filter(ConnectionWrapper::isInUse).count();
        }

        return String.format("Pool[%s]: total=%d, idle=%d, inUse=%d", 
                maskJdbcUrl(jdbcUrl), total, idle, inUse);
    }

    /**
     * 获取所有连接池的统计信息
     */
    public String getAllPoolStats() {
        StringBuilder sb = new StringBuilder("ConnectionPool Statistics:\n");
        for (String jdbcUrl : connectionPools.keySet()) {
            sb.append("  ").append(getPoolStats(jdbcUrl)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 关闭连接池管理器
     */
    @PreDestroy
    public void shutdown() {
        if (closed) {
            return;
        }
        closed = true;

        log.info("Shutting down ConnectionPoolManager...");

        // 关闭定时任务
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 关闭所有连接
        for (String jdbcUrl : new ArrayList<>(connectionPools.keySet())) {
            closePool(jdbcUrl);
        }

        log.info("ConnectionPoolManager shutdown completed");
    }

    /**
     * 静默关闭AutoCloseable资源
     */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.debug("Error closing resource: {}", e.getMessage());
            }
        }
    }

    /**
     * 静默关闭Connection
     */
    private void closeConnectionQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.debug("Error closing connection: {}", e.getMessage());
            }
        }
    }

    /**
     * 格式化堆栈信息
     */
    private String formatStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace == null || stackTrace.length == 0) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder("\n");
        int limit = Math.min(stackTrace.length, 10); // 只显示前10行
        for (int i = 0; i < limit; i++) {
            sb.append("    at ").append(stackTrace[i]).append("\n");
        }
        if (stackTrace.length > limit) {
            sb.append("    ... ").append(stackTrace.length - limit).append(" more\n");
        }
        return sb.toString();
    }

    /**
     * 掩码处理JDBC URL，隐藏敏感信息
     */
    private String maskJdbcUrl(String url) {
        if (url == null || url.length() <= 50) {
            return url;
        }
        return url.substring(0, 50) + "...";
    }

    /**
     * 检查连接池是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * 获取连接池配置
     */
    public ConnectionPoolConfig getConfig() {
        return config;
    }

    /**
     * 获取所有连接池的详细信息（只读操作，用于监控）
     *
     * @return 连接池详情列表，每个元素包含jdbcUrl、总连接数、空闲连接数、活跃连接数等信息
     */
    public List<Map<String, Object>> getAllPoolDetails() {
        List<Map<String, Object>> details = new ArrayList<>();
        
        for (Map.Entry<String, List<ConnectionWrapper>> entry : connectionPools.entrySet()) {
            String jdbcUrl = entry.getKey();
            List<ConnectionWrapper> pool = entry.getValue();
            
            Map<String, Object> detail = new ConcurrentHashMap<>();
            detail.put("jdbcUrl", maskJdbcUrl(jdbcUrl));
            
            if (pool != null) {
                synchronized (pool) {
                    int total = pool.size();
                    int idle = (int) pool.stream().filter(ConnectionWrapper::isIdle).count();
                    int inUse = (int) pool.stream().filter(ConnectionWrapper::isInUse).count();
                    
                    detail.put("totalConnections", total);
                    detail.put("idleConnections", idle);
                    detail.put("activeConnections", inUse);
                    detail.put("maxPoolSize", config.getMaxPoolSize());
                    
                    // 计算使用率
                    double usageRate = total > 0 ? (double) inUse / config.getMaxPoolSize() : 0.0;
                    detail.put("usageRate", usageRate);
                    
                    // 状态判断
                    String status = "正常";
                    if (usageRate > 0.9) {
                        status = "异常";
                    } else if (usageRate > 0.7) {
                        status = "告警";
                    }
                    detail.put("status", status);
                    
                    detail.put("maxLifetimeMs", config.getMaxLifetimeMs());
                    
                    // 计算平均借用时长
                    long totalBorrowDuration = 0;
                    int borrowCount = 0;
                    for (ConnectionWrapper wrapper : pool) {
                        if (wrapper.isInUse()) {
                            totalBorrowDuration += wrapper.getCurrentBorrowDurationMs();
                            borrowCount++;
                        }
                    }
                    long avgBorrowDuration = borrowCount > 0 ? totalBorrowDuration / borrowCount : 0;
                    detail.put("avgBorrowDurationMs", avgBorrowDuration);
                }
            } else {
                detail.put("totalConnections", 0);
                detail.put("idleConnections", 0);
                detail.put("activeConnections", 0);
                detail.put("maxPoolSize", config.getMaxPoolSize());
                detail.put("usageRate", 0.0);
                detail.put("status", "正常");
                detail.put("maxLifetimeMs", config.getMaxLifetimeMs());
                detail.put("avgBorrowDurationMs", 0L);
            }
            
            details.add(detail);
        }
        
        return details;
    }
}
