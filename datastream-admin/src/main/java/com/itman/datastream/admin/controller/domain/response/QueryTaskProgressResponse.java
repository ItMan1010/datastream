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
package com.itman.datastream.admin.controller.domain.response;

import com.itman.datastream.common.entity.DataMoveProgressEntity;
import com.itman.datastream.common.entity.TaskExecuteEntity;
import lombok.Data;

import java.util.List;

@Data
public class QueryTaskProgressResponse extends AbstractResponse {
    private Integer count = 0;
    private List<DataMoveProgressEntity> dataMoveProgressList;
    private Integer sourceObjectCount;
    private List<TaskExecuteEntity> taskExecuteList;
    /**
     * 表结构迁移记录总数
     */
    private Integer tableMoveCount = 0;
    /**
     * 表结构迁移已经处理记录总数
     */
    private Integer tableMoveDoneCount = 0;
    /**
     * 表结构迁移实际成功记录总数
     */
    private Integer tableMoveActualCount = 0;
    /**
     * 数据稽核差异数量
     */
    private Integer dataCheckCount = 0;
}
