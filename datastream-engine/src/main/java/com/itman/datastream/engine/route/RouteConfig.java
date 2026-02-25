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

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static com.itman.datastream.common.constant.DataStreamConstant.SOURCE_DATA_META_DATA_KEY_NAME;

@Configuration
@Slf4j
public class RouteConfig {
    @Bean(name = SOURCE_DATA_META_DATA_KEY_NAME)
    @ConfigurationProperties(prefix = "spring.datasource.druid.metadb")
    public javax.sql.DataSource oneDataSource() {
        return DruidDataSourceBuilder.create().build();
    }

    @Bean
    @Primary
    public DataBaseSource dataSource(@Qualifier(SOURCE_DATA_META_DATA_KEY_NAME) javax.sql.DataSource dataSource) {
        return new DataBaseSource(dataSource);
    }
}
