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
package com.itman.datastream.admin.controller;

import com.itman.datastream.admin.controller.domain.request.*;
import com.itman.datastream.admin.controller.domain.response.*;
import com.itman.datastream.admin.handler.*;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {
    private final DataMoveHandler dataMoveHandler;
    private final LinkTaskHandler linkTaskHandler;
    private final DataCdcHandler dataCdcHandler;
    private final DataCheckHandler dataCheckHandler;
    private final DataMQHandler dataMQHandler;


    @LogOperate(operateType = 2, moduleName = "创建迁移任务操作", description = "'sourceObjectName:'+#createMoveTaskRequest.sourceObjectName")
    @PostMapping(path = "/createMoveTask", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CreateMoveTaskResponse> createMoveTask(@RequestBody CreateMoveTaskRequest createMoveTaskRequest) {
        CreateMoveTaskResponse createMoveTaskResponse = new CreateMoveTaskResponse();

        try {
            CreateTaskInstanceEntity createTaskInstance = new CreateTaskInstanceEntity();
            BeanUtils.copyProperties(createMoveTaskRequest, createTaskInstance);
            dataMoveHandler.createTaskInstance(createTaskInstance);
        } catch (DataStreamException aie) {
            createMoveTaskResponse.setErrorCode(aie.getErrCode());
            createMoveTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            createMoveTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            createMoveTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(createMoveTaskResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTaskProgress", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTaskProgressResponse> queryTaskProgress(@RequestBody QueryTaskProgressRequest queryTaskProgressRequest) {
        QueryTaskProgressResponse queryTaskProgressResponse = new QueryTaskProgressResponse();
        try {
            List<DataMoveTaskEntity> taskList = dataMoveHandler.queryTaskByTaskId(queryTaskProgressRequest.getTaskId());
            if (CollectionUtils.isEmpty(taskList)) {
                throw new DataStreamException(OPER_QUERY_TASK_BY_TASKID_ERROR);
            }
            if (taskList.get(0).getTaskType().equals(DATA_STREAM_TASK_TYPE_TABLE_MOVE)) {
                List<MoveTableEntity> moveTableList = dataMoveHandler.queryMoveTable(queryTaskProgressRequest.getTaskId());
                List<MoveTableEntity> moveTableListDone = moveTableList.stream().filter(x -> (x.getState().equals(DATA_STREAM_TASK_STATE_RUNNING) || x.getState().equals(DATA_STREAM_TASK_STATE_ERROR) || x.getState().equals(DATA_STREAM_TASK_STATE_FINISH))).collect(Collectors.toList());
                List<MoveTableEntity> moveTableListSuccess = moveTableList.stream().filter(x -> x.getState().equals(DATA_STREAM_TASK_STATE_FINISH)).collect(Collectors.toList());
                queryTaskProgressResponse.setTableMoveCount(moveTableList.size());
                queryTaskProgressResponse.setTableMoveDoneCount(moveTableListDone.size());
                queryTaskProgressResponse.setTableMoveActualCount(moveTableListSuccess.size());
            } else {
                List<DataMoveProgressEntity> dataMoveProgressesList = dataMoveHandler.queryTaskProgress(taskList.get(0));
                if (!CollectionUtils.isEmpty(dataMoveProgressesList)) {
                    queryTaskProgressResponse.setCount(dataMoveProgressesList.size());
                    queryTaskProgressResponse.setDataMoveProgressList(dataMoveProgressesList);
                    queryTaskProgressResponse.setSourceObjectCount(dataMoveHandler.querySourceCount(queryTaskProgressRequest.getTaskId()));
                }
                if (taskList.get(0).getTaskType().equals(DATA_STREAM_TASK_TYPE_DATA_CHECK)) {
                    queryTaskProgressResponse.setDataCheckCount(dataCheckHandler.getDataCheckCount(queryTaskProgressRequest.getTaskId()));
                }
            }

            queryTaskProgressResponse.setTaskExecuteList(dataMoveHandler.queryTaskExecute(queryTaskProgressRequest.getTaskId()));
        } catch (DataStreamException aie) {
            queryTaskProgressResponse.setErrorCode(aie.getErrCode());
            queryTaskProgressResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryTaskProgressResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryTaskProgressResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryTaskProgressResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryDataMoveTaskList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryDataMoveTaskResponse> queryDataMoveTaskList(@RequestBody QueryDataMoveTaskRequest queryDataMoveTaskRequest) {
        QueryDataMoveTaskResponse queryDataMoveTaskResponse = new QueryDataMoveTaskResponse();
        try {
            queryDataMoveTaskResponse.setTotal(dataMoveHandler.queryDataMoveTaskCount(queryDataMoveTaskRequest));
            if (!Objects.isNull(queryDataMoveTaskResponse.getTotal())) {
                queryDataMoveTaskResponse.setDataMoveTaskList(dataMoveHandler.queryDataMoveTaskList(queryDataMoveTaskRequest));
            }

        } catch (DataStreamException aie) {
            queryDataMoveTaskResponse.setErrorCode(aie.getErrCode());
            queryDataMoveTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryDataMoveTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryDataMoveTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryDataMoveTaskResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTableMoveList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTableMoveResponse> queryTableMoveList(@RequestBody QueryTableMoveRequest queryTableMoveRequest) {
        QueryTableMoveResponse response = new QueryTableMoveResponse();
        try {
            response.setMoveTableList(dataMoveHandler.queryMoveTable(queryTableMoveRequest.getTaskId()));
        } catch (DataStreamException aie) {
            response.setErrorCode(aie.getErrCode());
            response.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/queryDataMoveInfoList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryDataMoveInfoResponse> queryDataMoveInfoList(@RequestBody QueryDataMoveInfoRequest queryDataMoveInfoRequest) {
        QueryDataMoveInfoResponse queryDataMoveInfoResponse = new QueryDataMoveInfoResponse();
        try {
            List<DataMoveTaskEntity> taskList = dataMoveHandler.queryTaskByTaskId(queryDataMoveInfoRequest.getTaskId());
            if (CollectionUtils.isEmpty(taskList)) {
                throw new DataStreamException(OPER_QUERY_TASK_BY_TASKID_ERROR);
            }

            List<DataMoveInfoEntity> dataMoveInfoList = null;
            if (taskList.get(0).getState().equals(DATA_STREAM_TASK_STATE_RUNNING)) {
                dataMoveInfoList = dataMoveHandler.queryDataMoveInfoListFromMem(queryDataMoveInfoRequest);
            } else {
                dataMoveInfoList = dataMoveHandler.queryDataMoveInfoList(queryDataMoveInfoRequest);
            }

            if (!CollectionUtils.isEmpty(dataMoveInfoList)) {
                queryDataMoveInfoResponse.setDataMoveInfoList(dataMoveInfoList);
                queryDataMoveInfoResponse.setCount(dataMoveInfoList.size());
            }

            DataMoveTaskEntity dataMoveTask = new DataMoveTaskEntity();
            dataMoveTask.setTaskId(queryDataMoveInfoRequest.getTaskId());
            dataMoveHandler.loadTaskExtendParameters(dataMoveTask);

            queryDataMoveInfoResponse.setQueueMaxSize(dataMoveHandler.getDataStreamQueueMaxSize(dataMoveTask.getSourcePropertiesSelectCount(), dataMoveTask.getDataStreamQueueChannel()));
            queryDataMoveInfoResponse.setQueueRunningSize(dataMoveHandler.getDataStreamQueueRunningSize(queryDataMoveInfoRequest.getTaskId()));
            queryDataMoveInfoResponse.setDataSendMode(dataMoveTask.getSourcePropertiesSendMode());
            queryDataMoveInfoResponse.setQueueNumber(dataMoveTask.getDataStreamQueueChannel());
        } catch (DataStreamException aie) {
            queryDataMoveInfoResponse.setErrorCode(aie.getErrCode());
            queryDataMoveInfoResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryDataMoveInfoResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryDataMoveInfoResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryDataMoveInfoResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "任务控制操作", description = "'linkTaskId:'+#operateDataMoveTaskRequest.linkTaskId+',operate:'+#operateDataMoveTaskRequest.operate")
    @PostMapping(path = "/operateDataMoveTask", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OperateDataMoveTaskResponse> operateDataMoveTask(@RequestBody OperateDataMoveTaskRequest operateDataMoveTaskRequest) {
        OperateDataMoveTaskResponse operateDataMoveTaskResponse = new OperateDataMoveTaskResponse();
        try {
            dataMoveHandler.operateDataMoveTask(operateDataMoveTaskRequest.getTaskId(), operateDataMoveTaskRequest.getOperate());
        } catch (DataStreamException aie) {
            operateDataMoveTaskResponse.setErrorCode(aie.getErrCode());
            operateDataMoveTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            operateDataMoveTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            operateDataMoveTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(operateDataMoveTaskResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "新建表链接任务", description = "'tableLinkId:'+#createTableLinkTaskRequest.tableLinkId+',businessId:'+#createTableLinkTaskRequest.businessId")
    @PostMapping(path = "/createTableLinkTask", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<CreateTableLinkTaskResponse> createTableLinkTask(@RequestBody CreateTableLinkTaskRequest createTableLinkTaskRequest) {
        CreateTableLinkTaskResponse createTableLinkTaskResponse = new CreateTableLinkTaskResponse();

        try {
            linkTaskHandler.createTableLinkTask(createTableLinkTaskRequest);
        } catch (DataStreamException aie) {
            createTableLinkTaskResponse.setErrorCode(aie.getErrCode());
            createTableLinkTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            createTableLinkTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            createTableLinkTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(createTableLinkTaskResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "表链接任务操作", description = "'linkTaskId:'+#operateTableLinkTaskRequest.linkTaskId+',operate:'+#operateTableLinkTaskRequest.operate")
    @PostMapping(path = "/operateTableLinkTask", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OperateTableLinkTaskResponse> operateTableLinkTask(@RequestBody OperateTableLinkTaskRequest operateTableLinkTaskRequest) {
        OperateTableLinkTaskResponse operateTableLinkTaskResponse = new OperateTableLinkTaskResponse();
        try {
            linkTaskHandler.operateTableLinkTask(operateTableLinkTaskRequest.getLinkTaskId(), operateTableLinkTaskRequest.getOperate());
        } catch (DataStreamException aie) {
            operateTableLinkTaskResponse.setErrorCode(aie.getErrCode());
            operateTableLinkTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            operateTableLinkTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            operateTableLinkTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(operateTableLinkTaskResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTableLinkTaskList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTableLinkTaskResponse> queryTableLinkTaskList(@RequestBody QueryTableLinkTaskRequest queryTableLinkTaskRequest) {
        QueryTableLinkTaskResponse queryTableLinkTaskResponse = new QueryTableLinkTaskResponse();
        try {
            queryTableLinkTaskResponse.setTotal(linkTaskHandler.queryTableLinkTaskCount(queryTableLinkTaskRequest));

            if (queryTableLinkTaskResponse.getTotal() > 0) {
                List<TableLinkTaskEntity> tableLinkTaskList = linkTaskHandler.queryTableLinkTaskList(queryTableLinkTaskRequest);
                if (!CollectionUtils.isEmpty(tableLinkTaskList)) {
                    queryTableLinkTaskResponse.setTableLinkTaskList(tableLinkTaskList);
                }
            }
        } catch (DataStreamException aie) {
            queryTableLinkTaskResponse.setErrorCode(aie.getErrCode());
            queryTableLinkTaskResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryTableLinkTaskResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryTableLinkTaskResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryTableLinkTaskResponse, HttpStatus.OK);
    }
}
