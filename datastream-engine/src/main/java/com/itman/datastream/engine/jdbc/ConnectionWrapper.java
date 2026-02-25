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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据库连接包装类
 * 封装原生Connection，提供连接状态管理和生命周期追踪
 *
 * @author DataStream
 */
@Slf4j
@Getter
public class ConnectionWrapper {

    /**
     * 连接状态枚举
     */
    public enum ConnectionState {
        /**
         * 空闲状态，可被借出
         */
        IDLE(0),
        /**
         * 使用中状态，已被借出
         */
        IN_USE(1),
        /**
         * 已关闭状态
         */
        CLOSED(2),
        /**
         * 无效状态，需要被移除
         */
        INVALID(3);

        private final int code;

        ConnectionState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 原生数据库连接
     */
    private final Connection connection;

    /**
     * JDBC URL（作为连接池的唯一标识）
     */
    private final String jdbcUrl;

    /**
     * 连接创建时间戳
     */
    private final long createTimestamp;

    /**
     * 最后访问时间戳（原子操作保证线程安全）
     */
    private final AtomicLong lastAccessTimestamp;

    /**
     * 最后借出时间戳
     */
    private final AtomicLong lastBorrowTimestamp;

    /**
     * 连接状态（原子操作保证线程安全）
     */
    private final AtomicInteger state;

    /**
     * 借出次数统计
     */
    private final AtomicLong borrowCount;

    /**
     * 当前借用者线程名称（用于泄漏检测）
     */
    private volatile String borrowerThreadName;

    /**
     * 当前借用者堆栈信息（用于泄漏检测）
     */
    private volatile StackTraceElement[] borrowerStackTrace;

    /**
     * 构造函数
     *
     * @param connection 原生数据库连接
     * @param jdbcUrl    JDBC URL
     */
    public ConnectionWrapper(Connection connection, String jdbcUrl) {
        this.connection = connection;
        this.jdbcUrl = jdbcUrl;
        this.createTimestamp = System.currentTimeMillis();
        this.lastAccessTimestamp = new AtomicLong(this.createTimestamp);
        this.lastBorrowTimestamp = new AtomicLong(0);
        this.state = new AtomicInteger(ConnectionState.IDLE.getCode());
        this.borrowCount = new AtomicLong(0);
    }

    /**
     * 尝试将连接从空闲状态切换为使用中状态（原子操作）
     *
     * @return 是否成功借出
     */
    public boolean tryBorrow() {
        boolean success = state.compareAndSet(ConnectionState.IDLE.getCode(), ConnectionState.IN_USE.getCode());
        if (success) {
            long now = System.currentTimeMillis();
            lastAccessTimestamp.set(now);
            lastBorrowTimestamp.set(now);
            borrowCount.incrementAndGet();
            // 记录借用者信息用于泄漏检测
            Thread currentThread = Thread.currentThread();
            borrowerThreadName = currentThread.getName();
            borrowerStackTrace = currentThread.getStackTrace();
            log.debug("Connection [jdbcUrl={}] borrowed by thread [{}]", maskJdbcUrl(jdbcUrl), borrowerThreadName);
        }
        return success;
    }

    /**
     * 归还连接，将状态从使用中切换为空闲
     *
     * @return 是否成功归还
     */
    public boolean release() {
        boolean success = state.compareAndSet(ConnectionState.IN_USE.getCode(), ConnectionState.IDLE.getCode());
        if (success) {
            lastAccessTimestamp.set(System.currentTimeMillis());
            borrowerThreadName = null;
            borrowerStackTrace = null;
            log.debug("Connection [jdbcUrl={}] released", maskJdbcUrl(jdbcUrl));
        }
        return success;
    }

    /**
     * 标记连接为无效状态
     */
    public void markInvalid() {
        state.set(ConnectionState.INVALID.getCode());
        log.warn("Connection [jdbcUrl={}] marked as invalid", maskJdbcUrl(jdbcUrl));
    }

    /**
     * 标记连接为已关闭状态
     */
    public void markClosed() {
        state.set(ConnectionState.CLOSED.getCode());
    }

    /**
     * 检查连接是否处于空闲状态
     */
    public boolean isIdle() {
        return state.get() == ConnectionState.IDLE.getCode();
    }

    /**
     * 检查连接是否处于使用中状态
     */
    public boolean isInUse() {
        return state.get() == ConnectionState.IN_USE.getCode();
    }

    /**
     * 检查连接是否已关闭或无效
     */
    public boolean isClosedOrInvalid() {
        int currentState = state.get();
        return currentState == ConnectionState.CLOSED.getCode() 
            || currentState == ConnectionState.INVALID.getCode();
    }

    /**
     * 获取连接存活时间（毫秒）
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - createTimestamp;
    }

    /**
     * 获取空闲时间（毫秒）
     */
    public long getIdleTimeMs() {
        return System.currentTimeMillis() - lastAccessTimestamp.get();
    }

    /**
     * 获取当前借用时长（毫秒）
     * 仅在连接被借出时有意义
     */
    public long getCurrentBorrowDurationMs() {
        long borrowTime = lastBorrowTimestamp.get();
        if (borrowTime == 0 || !isInUse()) {
            return 0;
        }
        return System.currentTimeMillis() - borrowTime;
    }

    /**
     * 验证连接是否有效
     *
     * @param timeoutSeconds 验证超时时间（秒）
     * @return 连接是否有效
     */
    public boolean isValid(int timeoutSeconds) {
        if (isClosedOrInvalid()) {
            return false;
        }
        try {
            return connection != null && connection.isValid(timeoutSeconds);
        } catch (SQLException e) {
            log.warn("Failed to validate connection [jdbcUrl={}]: {}", maskJdbcUrl(jdbcUrl), e.getMessage());
            return false;
        }
    }

    /**
     * 关闭底层连接
     */
    public void close() {
        markClosed();
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    log.debug("Connection [jdbcUrl={}] closed successfully", maskJdbcUrl(jdbcUrl));
                }
            } catch (SQLException e) {
                log.warn("Error closing connection [jdbcUrl={}]: {}", maskJdbcUrl(jdbcUrl), e.getMessage());
            }
        }
    }

    /**
     * 获取当前连接状态
     */
    public ConnectionState getConnectionState() {
        int currentState = state.get();
        for (ConnectionState cs : ConnectionState.values()) {
            if (cs.getCode() == currentState) {
                return cs;
            }
        }
        return ConnectionState.INVALID;
    }

    /**
     * 掩码处理JDBC URL，隐藏敏感信息
     */
    private String maskJdbcUrl(String url) {
        if (url == null || url.length() <= 30) {
            return url;
        }
        return url.substring(0, 30) + "...";
    }

    @Override
    public String toString() {
        return String.format("ConnectionWrapper[jdbcUrl=%s, state=%s, age=%dms, idle=%dms, borrowCount=%d]",
                maskJdbcUrl(jdbcUrl), getConnectionState(), getAgeMs(), getIdleTimeMs(), borrowCount.get());
    }
}
