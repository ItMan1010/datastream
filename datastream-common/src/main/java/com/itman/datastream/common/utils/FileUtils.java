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
package com.itman.datastream.common.utils;


import java.io.File;
import java.util.ArrayList;
import java.util.List;

//@Slf4j
public class FileUtils {
    private FileUtils() {
    }

    public static List<String> getFiles(String path) {
        List<String> fileNameList = new ArrayList<>();
        File file = new File(path);
        if (file.isDirectory()) {
            // 获取路径下的所有文件
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && !files[i].getName().substring(0, 1).equals(".")) {
                    fileNameList.add(files[i].getName());
                }
            }
        } else {
            fileNameList.add(file.getName());
        }
        return fileNameList;
    }
}
