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
package com.itman.datastream.admin.executor;

import com.itman.datastream.admin.handler.LinkTaskHandler;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.entity.ColumnTypeDefineEntity;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.route.RegisterDataBase;
import com.itman.datastream.engine.systemlog.ISystemLogEvent;
import com.itman.datastream.common.entity.TableLinkTaskEntity;
import com.itman.datastream.admin.service.ILinkTargetService;
import com.itman.datastream.admin.service.IMetaService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;


import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.utils.CommUtils.getStackTraceAsString;

@Slf4j
@Component
@AllArgsConstructor
public class LinkTaskExecutor {
    private final IMetaService metaService;
    private final DataStreamConfig dataStreamConfig;
    private final LinkTaskHandler linkTaskHandler;
    private final ILinkTargetService backTargetService;
    private final ISystemLogEvent systemLogEvent;
    private final RegisterDataBase registerDataBase;

    @Async(TASK_WORKS_POOL_EXECUTOR)
    public void tableLinkTaskProcess(TableLinkTaskEntity tableLinkTask) throws DataStreamException {
        String errorCode = null;
        String errorMessage = null;
        jobLogbackEvenBackTask(tableLinkTask.getLinkTaskId(), "dataBackTaskProcess start");

        try {
            List<DataBaseEntity> dataBaseList = new ArrayList<>();
            dataBaseList.add(tableLinkTask.getSourceDataSource());
            dataBaseList.add(tableLinkTask.getTargetDataSource());

            registerDataBase.registerDataSources(dataBaseList, tableLinkTask.getLinkTaskId());

            List<ColumnTypeDefineEntity> sourceColumnTypeDefineList = metaService.queryColumnTypeDefine(DataBaseEnum.of(tableLinkTask.getSourceDataSource().getDataBaseType()).getName().toLowerCase());
            if (CollectionUtils.isEmpty(sourceColumnTypeDefineList)) {
                throw new DataStreamException("xxx", "获取源数据库字段类型名称定义记录失败");
            }
            tableLinkTask.setSourceColumnTypeDefineList(sourceColumnTypeDefineList);

            List<ColumnTypeDefineEntity> targetColumnTypeDefineList = metaService.queryColumnTypeDefine(DataBaseEnum.of(tableLinkTask.getTargetDataSource().getDataBaseType()).getName().toLowerCase());
            if (CollectionUtils.isEmpty(targetColumnTypeDefineList)) {
                throw new DataStreamException("xxx", "获取目标数据库字段类型名称定义记录失败");
            }
            tableLinkTask.setTargetColumnTypeDefineList(targetColumnTypeDefineList);

            tableLinkTaskByService(tableLinkTask);
        } catch (DataStreamException aie) {
            log.error("dispatchDataBackTaskProcess: SystemException={}", aie);
            errorCode = aie.getErrCode();
            errorMessage = aie.getMessage();
            jobLogbackEvenBackTask(tableLinkTask.getLinkTaskId(), getStackTraceAsString(aie));
        } catch (Exception e) {
            log.error("dispatchDataBackTaskProcess: Exception={}", e);
            errorCode = DataStreamErrorCode.UNKNOWN_ERROR.getCode();
            errorMessage = DataStreamErrorCode.UNKNOWN_ERROR.getMessage();
            jobLogbackEvenBackTask(tableLinkTask.getLinkTaskId(), getStackTraceAsString(e));
        }

        if (!metaService.updateTableLinkErrorTask(tableLinkTask.getLinkTaskId(), TABLE_LINK_TASK_STATE_ING, Objects.isNull(errorCode) ? TABLE_LINK_TASK_STATE_SUCCESS : TABLE_LINK_TASK_STATE_ERROR, errorCode, errorMessage).equals(1)) {
            jobLogbackEvenBackTask(tableLinkTask.getLinkTaskId(), "updateTableLinkErrorTask update state finish error, linkTaskId={" + tableLinkTask.getLinkTaskId() + "}");
        }

        registerDataBase.releaseTaskDataSources(tableLinkTask.getLinkTaskId());

        jobLogbackEvenBackTask(tableLinkTask.getLinkTaskId(), "dataBackTaskProcess end");
    }

    private void jobLogbackEvenBackTask(Long backTaskId, String content) {
        systemLogEvent.jobLogbackEvent(JOB_TYPE_MOVE_LINK, backTaskId, content);
    }

    public void tableLinkTaskByService(TableLinkTaskEntity tableLinkTask) throws DataStreamException {
        linkTaskHandler.tableLinkTaskByService(tableLinkTask);
    }
}
