package com.itman.datastream.engine.route;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池信息
 * 用于跟踪连接池的使用情况
 */
@Data
class PoolInfo {
    /**
     * 连接池引用计数（有多少任务在使用）
     */
    private final AtomicInteger referenceCount = new AtomicInteger(0);

    /**
     * 连接池创建时间
     */
    private final long createTime;

    /**
     * 最后使用时间（最后一次被任务使用的时间）
     */
    private volatile long lastAccessTime;

    /**
     * 使用该连接池的任务ID集合
     */
    private final Set<Long> taskIds = ConcurrentHashMap.newKeySet();

    /**
     * 连接池数据源
     */
    private final DruidDataSource dataSource;

    /**
     * 数据源key
     */
    private final String dataSourceKey;

    public PoolInfo(DruidDataSource dataSource, String dataSourceKey) {
        this.dataSource = dataSource;
        this.dataSourceKey = dataSourceKey;
        this.createTime = System.currentTimeMillis();
        this.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 增加引用计数
     */
    public int incrementRef(Long taskId) {
        taskIds.add(taskId);
        lastAccessTime = System.currentTimeMillis();
        return referenceCount.incrementAndGet();
    }

    /**
     * 减少引用计数
     */
    public int decrementRef(Long taskId) {
        taskIds.remove(taskId);
        lastAccessTime = System.currentTimeMillis();
        return referenceCount.decrementAndGet();
    }

    /**
     * 是否可以被清理（引用计数为0且超过清理阈值时间）
     */
    public boolean canBeCleaned(long idleTimeoutMs) {
        return referenceCount.get() == 0 && (System.currentTimeMillis() - lastAccessTime) > idleTimeoutMs;
    }
}