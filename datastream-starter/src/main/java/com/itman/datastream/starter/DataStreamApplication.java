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
package com.itman.datastream.starter;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@ComponentScan(basePackages = {
        "com.itman.datastream.common",
        "com.itman.datastream.connectors",
        "com.itman.datastream.engine",
        "com.itman.datastream.admin",
        "com.itman.datastream.security"
})
@MapperScan({
        "com.itman.datastream.engine.mapper",
        "com.itman.datastream.connectors.oracle.dao",
        "com.itman.datastream.connectors.postgres.dao",
        "com.itman.datastream.connectors.mysql.dao"})
@EnableAsync
public class DataStreamApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataStreamApplication.class, args);
        log.info("********启动DataStreamApplication成功****************!\n");
    }
}
