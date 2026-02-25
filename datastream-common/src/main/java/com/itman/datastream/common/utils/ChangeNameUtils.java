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


import java.util.HashMap;
import java.util.Map;

import static com.itman.datastream.common.constant.DataStreamConstant.*;


public class ChangeNameUtils {
    private ChangeNameUtils() {
    }

    private static final Map<Integer, String> ON_LINE_FLAG_NAME_MAP = new HashMap<>(2);
    private static final Map<Integer, String> TASK_INSTANCE_RUN_STATE_NAME_MAP = new HashMap<>(4);
    private static final Map<Integer, String> RESULT_FLAG_NAME_MAP = new HashMap<>(3);
    private static final Map<Integer, String> REPAIR_STATE_NAME_MAP = new HashMap<>(3);
    private static final Map<Integer, String> OBJECT_TYPE_NAME_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_NAME_TYPE_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_TYPE_MAP = new HashMap<>(2);

    public static final Map<Integer, String> FILE_BAK_ACTION_MAP = new HashMap<>(3);
    public static final Map<Integer, String> FILE_SPLIT_FLAG_MAP = new HashMap<>(4);
    private static final Map<Integer, String> INFO_LEVEL_NAME_MAP = new HashMap<>(2);

    static {
        ON_LINE_FLAG_NAME_MAP.put(COMMON_STATE_OFFLINE, "下线");
        ON_LINE_FLAG_NAME_MAP.put(COMMON_STATE_ONLINE, "上线");

        FILE_NAME_TYPE_MAP.put(1, "固定名称");
//        FILE_NAME_TYPE_MAP.put(2, "正则表达式");

        FILE_BAK_ACTION_MAP.put(1, "不处理");
        FILE_BAK_ACTION_MAP.put(2, "直接删除");
        FILE_BAK_ACTION_MAP.put(3, "备份目录");

        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_FIX_WIDTH, "固定长度");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_VERTICAL_LINE, "竖线|");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_COMMA, "逗号,");
        FILE_SPLIT_FLAG_MAP.put(SPLIT_FLAG_AND, "与符号&");
    }

    public static String changeOnLineFlagName(Integer onLineFlag) {
        return ON_LINE_FLAG_NAME_MAP.containsKey(onLineFlag) ? ON_LINE_FLAG_NAME_MAP.get(onLineFlag) : "null";
    }

    public static String changeTaskInstanceRunStateName(Integer runState) {
        return TASK_INSTANCE_RUN_STATE_NAME_MAP.containsKey(runState) ? TASK_INSTANCE_RUN_STATE_NAME_MAP.get(runState) : "null";
    }

    public static String changeResultFlagName(Integer resultFlag) {
        return RESULT_FLAG_NAME_MAP.containsKey(resultFlag) ? RESULT_FLAG_NAME_MAP.get(resultFlag) : "null";
    }

    public static String changeRepairStateName(Integer repairState) {
        return REPAIR_STATE_NAME_MAP.containsKey(repairState) ? REPAIR_STATE_NAME_MAP.get(repairState) : "null";
    }

    public static String changeObjectTypeName(Integer objectType) {
        return OBJECT_TYPE_NAME_MAP.containsKey(objectType) ? OBJECT_TYPE_NAME_MAP.get(objectType) : "null";
    }

    public static String changeFileNameTypeName(Integer fileNameType) {
        return FILE_NAME_TYPE_MAP.containsKey(fileNameType) ? FILE_NAME_TYPE_MAP.get(fileNameType) : "null";
    }

    public static String changeFileBakActionName(Integer fileBakAction) {
        return FILE_BAK_ACTION_MAP.containsKey(fileBakAction) ? FILE_BAK_ACTION_MAP.get(fileBakAction) : "null";
    }

    public static String changeSplitFlagName(Integer splitFlag) {
        return FILE_SPLIT_FLAG_MAP.containsKey(splitFlag) ? FILE_SPLIT_FLAG_MAP.get(splitFlag) : "null";
    }

    public static String changeInfoLevelName(Integer infoLevel) {
        return INFO_LEVEL_NAME_MAP.containsKey(infoLevel) ? INFO_LEVEL_NAME_MAP.get(infoLevel) : "null";
    }
}
