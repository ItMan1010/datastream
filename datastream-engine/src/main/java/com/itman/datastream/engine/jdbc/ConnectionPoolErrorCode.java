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

import com.itman.datastream.common.errcode.IErrorCode;

/**
 * 连接池相关错误码枚举
 *
 * @author DataStream
 */
public enum ConnectionPoolErrorCode implements IErrorCode {

    /**
     * 获取连接超时
     */
    CONNECTION_TIMEOUT("POOL_001", "获取数据库连接超时"),

    /**
     * 连接池已满
     */
    POOL_EXHAUSTED("POOL_002", "连接池已满，无法创建新连接"),

    /**
     * 连接正在使用中
     */
    CONNECTION_IN_USE("POOL_003", "数据库连接正在使用中"),

    /**
     * 连接无效
     */
    CONNECTION_INVALID("POOL_004", "数据库连接无效"),

    /**
     * 创建连接失败
     */
    CONNECTION_CREATE_FAILED("POOL_005", "创建数据库连接失败"),

    /**
     * 校验语句执行失败
     */
    VALIDATION_FAILED("POOL_006", "数据库校验语句执行失败"),

    /**
     * 连接已关闭
     */
    CONNECTION_CLOSED("POOL_007", "数据库连接已关闭"),

    /**
     * 连接归还失败
     */
    CONNECTION_RELEASE_FAILED("POOL_008", "归还数据库连接失败"),

    /**
     * 数据源不存在
     */
    DATASOURCE_NOT_FOUND("POOL_009", "数据源不存在"),

    /**
     * 连接泄漏检测警告
     */
    CONNECTION_LEAK_DETECTED("POOL_010", "检测到可能的连接泄漏");

    private final String code;
    private final String message;

    ConnectionPoolErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
