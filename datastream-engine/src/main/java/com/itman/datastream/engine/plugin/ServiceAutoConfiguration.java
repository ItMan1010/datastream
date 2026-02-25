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
package com.itman.datastream.engine.plugin;

import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.api.ITableMetaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

//@Configuration
public class ServiceAutoConfiguration {
    @Value("${plugin.directory:./plugins}")
    private String pluginDirectoryPath;

    private ClassLoader pluginClassLoader;


    @PostConstruct
    public void init() throws MalformedURLException {
//        File pluginDir = new File(pluginDirectoryPath);
//        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
//            throw new RuntimeException("Plugin directory not found: " + pluginDirectoryPath);
//        }
//
//        List<URL> urls = new ArrayList<>();
//        for (File file : pluginDir.listFiles(f -> f.getName().endsWith(".jar"))) {
//            urls.add(file.toURI().toURL());
//        }
//
//        if (!urls.isEmpty()) {
//            pluginClassLoader = new URLClassLoader(
//                    urls.toArray(new URL[0]),
//                    Thread.currentThread().getContextClassLoader()
//            );
//        }
    }
//
//    @Bean
//    public List<IDatabaseAdapter> dataBaseAdapterServices() {
//        if (pluginClassLoader == null) {
//            return new ArrayList<>();
//        }
//        ClassLoader original = Thread.currentThread().getContextClassLoader();
//        try {
//            // 切换到插件类加载器
//            Thread.currentThread().setContextClassLoader(pluginClassLoader);
//
//            List<IDatabaseAdapter> services = new ArrayList<>();
//            ServiceLoader.load(IDatabaseAdapter.class, pluginClassLoader).forEach(services::add);
//            return services;
//        } finally {
//            // 恢复原始类加载器
//            Thread.currentThread().setContextClassLoader(original);
//        }
//    }
//
//    @Bean
//    public List<ITableMetaApi> tableMetaApiServices() {
//        if (pluginClassLoader == null) {
//            return new ArrayList<>();
//        }
//        ClassLoader original = Thread.currentThread().getContextClassLoader();
//        try {
//            // 切换到插件类加载器
//            Thread.currentThread().setContextClassLoader(pluginClassLoader);
//
//            List<ITableMetaApi> services = new ArrayList<>();
//            ServiceLoader.load(ITableMetaApi.class, pluginClassLoader).forEach(services::add);
//            return services;
//        } finally {
//            // 恢复原始类加载器
//            Thread.currentThread().setContextClassLoader(original);
//        }
//    }
}
