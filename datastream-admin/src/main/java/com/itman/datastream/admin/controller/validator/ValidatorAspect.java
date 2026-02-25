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
package com.itman.datastream.admin.controller.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itman.datastream.admin.controller.domain.request.*;
import com.itman.datastream.admin.controller.domain.response.AbstractResponse;
import com.itman.datastream.common.errcode.IErrorCode;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Aspect
@Component
public class ValidatorAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String POINT_CUT = "execution(* com.itman.datastream.admin.controller.*.*(..))";

    @Pointcut(POINT_CUT)
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                checkParams(arg);
            }

            return joinPoint.proceed();
        } catch (DataStreamException e) {
            AbstractResponse response = new AbstractResponse();
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    void checkParams(Object arg) throws DataStreamException, JsonProcessingException {
        if(log.isInfoEnabled()){
            String requestJson = objectMapper.writeValueAsString(arg);
            log.info("request_____: {}", requestJson);
        }

        if (arg instanceof QueryTableLinkRequest) {
            QueryTableLinkRequest queryTableLinkRequest = (QueryTableLinkRequest) arg;
            Optional.ofNullable(queryTableLinkRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_FLOW_DEFINE_QUERY_FLAG_NULL_ERROR));
            Optional.ofNullable(queryTableLinkRequest.getPage()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_FLOW_DEFINE_PAGE_NULL_ERROR));
            Optional.ofNullable(queryTableLinkRequest.getCount()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_FLOW_DEFINE_COUNT_NULL_ERROR));
            if (!Arrays.asList(TABLE_LINK_QUERY_FLAG_ALL, TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID,
                    TABLE_LINK_QUERY_FLAG_LINK_NAME, TABLE_LINK_QUERY_FLAG_TABLE_NAME,
                    TABLE_LINK_QUERY_FLAG_STATE).contains(queryTableLinkRequest.getQueryFlag())) {
                throw new DataStreamException(PARAM_QUERY_FLOW_DEFINE_QUERY_FLAG_NOT_EQUAL_ERROR);
            }
        } else if (arg instanceof QueryLinkDetailRequest) {
            QueryLinkDetailRequest queryLinkDetailRequest = (QueryLinkDetailRequest) arg;
            Optional.ofNullable(queryLinkDetailRequest.getTableLinkId()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_FLOW_DETAIL_FLOW_DEFINE_ID_NULL_ERROR));
        } else if (arg instanceof AddTableLinkRequest) {
            AddTableLinkRequest addTableLinkRequest = (AddTableLinkRequest) arg;
            Optional.ofNullable(addTableLinkRequest.getTableLinkName()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_FLOW_DETAIL_FLOW_NAME_NULL_ERROR));
        } else if (arg instanceof ModifyTableLinkRequest) {
            ModifyTableLinkRequest modifyTableLinkRequest = (ModifyTableLinkRequest) arg;
            Optional.ofNullable(modifyTableLinkRequest.getTableLinkId()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_FLOW_DEFINE_FLOW_DEFINE_ID_NULL_ERROR));
            Optional.ofNullable(modifyTableLinkRequest.getTableLinkName()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_FLOW_DEFINE_FLOW_NAME_NULL_ERROR));
        } else if (arg instanceof DelTableLinkRequest) {
            DelTableLinkRequest delTableLinkRequest = (DelTableLinkRequest) arg;
            Optional.ofNullable(delTableLinkRequest.getTableLinkId()).orElseThrow(() -> new DataStreamException(PARAM_DEL_FLOW_DEFINE_FLOW_DEFINE_ID_NULL_ERROR));
        } else if (arg instanceof OnOffTableLinkRequest) {
            OnOffTableLinkRequest onOffTableLinkRequest = (OnOffTableLinkRequest) arg;
            Optional.ofNullable(onOffTableLinkRequest.getTableLinkId()).orElseThrow(() -> new DataStreamException(PARAM_ONOFF_FLOW_DEFINE_FLOW_DEFINE_ID_NULL_ERROR));
            Optional.ofNullable(onOffTableLinkRequest.getState()).orElseThrow(() -> new DataStreamException(PARAM_ONOFF_FLOW_DEFINE_STATE_NULL_ERROR));
        } else if (arg instanceof TestTableLinkRequest) {
            TestTableLinkRequest testTableLinkRequest = (TestTableLinkRequest) arg;
            Optional.ofNullable(testTableLinkRequest.getDataBaseId()).orElseThrow(() -> new DataStreamException(PARAM_TEST_FLOW_DEFINE_FLOW_DEFINE_ID_NULL_ERROR));
            Optional.ofNullable(testTableLinkRequest.getTableLinkName()).orElseThrow(() -> new DataStreamException(PARAM_TEST_FLOW_DEFINE_FLOW_NAME_NULL_ERROR));
            Optional.ofNullable(testTableLinkRequest.getLinkNode()).orElseThrow(() -> new DataStreamException(PARAM_TEST_FLOW_DEFINE_FLOW_NODE_NULL_ERROR));
        } else if (arg instanceof QueryDataBaseRowsRequest) {
            QueryDataBaseRowsRequest queryDataBaseRowsRequest = (QueryDataBaseRowsRequest) arg;
            Optional.ofNullable(queryDataBaseRowsRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_DATA_SOURCE_QUERY_FLAG_NULL_ERROR));
            Optional.ofNullable(queryDataBaseRowsRequest.getPage()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_DATA_SOURCE_PAGE_NULL_ERROR));
            Optional.ofNullable(queryDataBaseRowsRequest.getCount()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_DATA_SOURCE_COUNT_NULL_ERROR));
        } else if (arg instanceof AddDataBaseRequest) {
            AddDataBaseRequest addDataBaseRequest = (AddDataBaseRequest) arg;
            Optional.ofNullable(addDataBaseRequest.getDataBaseType()).orElseThrow(() -> new DataStreamException(PARAM_ADD_DATA_SOURCE_TYPE_NULL_ERROR));
            Optional.ofNullable(addDataBaseRequest.getDataBaseName()).orElseThrow(() -> new DataStreamException(PARAM_ADD_DATA_SOURCE_NAME_NULL_ERROR));
            Optional.ofNullable(addDataBaseRequest.getUrl()).orElseThrow(() -> new DataStreamException(PARAM_ADD_DATA_SOURCE_URL_NULL_ERROR));
            Optional.ofNullable(addDataBaseRequest.getUserName()).orElseThrow(() -> new DataStreamException(PARAM_ADD_DATA_SOURCE_USERNAME_NULL_ERROR));
            Optional.ofNullable(addDataBaseRequest.getPassWord()).orElseThrow(() -> new DataStreamException(PARAM_ADD_DATA_SOURCE_PASSWORD_NULL_ERROR));
        } else if (arg instanceof ModifyDataBaseRequest) {
            ModifyDataBaseRequest modifyDataBaseRequest = (ModifyDataBaseRequest) arg;
            Optional.ofNullable(modifyDataBaseRequest.getDataBaseType()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_DATA_SOURCE_TYPE_NULL_ERROR));
            Optional.ofNullable(modifyDataBaseRequest.getDataBaseName()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_DATA_SOURCE_NAME_NULL_ERROR));
            Optional.ofNullable(modifyDataBaseRequest.getUrl()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_DATA_SOURCE_URL_NULL_ERROR));
            Optional.ofNullable(modifyDataBaseRequest.getUserName()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_DATA_SOURCE_USERNAME_NULL_ERROR));
            Optional.ofNullable(modifyDataBaseRequest.getPassWord()).orElseThrow(() -> new DataStreamException(PARAM_MODIFY_DATA_SOURCE_PASSWORD_NULL_ERROR));
        } else if (arg instanceof DelDataBaseRequest) {
            DelDataBaseRequest delDataBaseRequest = (DelDataBaseRequest) arg;
            Optional.ofNullable(delDataBaseRequest.getDataBaseId()).orElseThrow(() -> new DataStreamException(PARAM_DEL_DATA_SOURCE_ID_NULL_ERROR));
        } else if (arg instanceof OnOffDataBaseRequest) {
            OnOffDataBaseRequest onOffDataBaseRequest = (OnOffDataBaseRequest) arg;
            Optional.ofNullable(onOffDataBaseRequest.getDataBaseId()).orElseThrow(() -> new DataStreamException(PARAM_DONOFF_DATA_SOURCE_ID_NULL_ERROR));
            Optional.ofNullable(onOffDataBaseRequest.getState()).orElseThrow(() -> new DataStreamException(PARAM_ONOFF_DATA_SOURCE_STATE_NULL_ERROR));
        } else if (arg instanceof TestDataBaseRequest) {
            TestDataBaseRequest testDataBaseRequest = (TestDataBaseRequest) arg;
            Optional.ofNullable(testDataBaseRequest.getDataBaseType()).orElseThrow(() -> new DataStreamException(PARAM_TEST_DATA_SOURCE_TYPE_NULL_ERROR));
            Optional.ofNullable(testDataBaseRequest.getDataBaseName()).orElseThrow(() -> new DataStreamException(PARAM_TEST_DATA_SOURCE_NAME_NULL_ERROR));
            Optional.ofNullable(testDataBaseRequest.getUrl()).orElseThrow(() -> new DataStreamException(PARAM_TEST_DATA_SOURCE_URL_NULL_ERROR));
            Optional.ofNullable(testDataBaseRequest.getUserName()).orElseThrow(() -> new DataStreamException(PARAM_TEST_DATA_SOURCE_USERNAME_NULL_ERROR));
            Optional.ofNullable(testDataBaseRequest.getPassWord()).orElseThrow(() -> new DataStreamException(PARAM_TEST_DATA_SOURCE_PASSWORD_NULL_ERROR));
        } else if (arg instanceof StatSystemInfoRequest) {
            StatSystemInfoRequest statSystemInfoRequest = (StatSystemInfoRequest) arg;
            Optional.ofNullable(statSystemInfoRequest.getDays()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_INFO_DAYS_NULL_ERROR));
        } else if (arg instanceof DataSearchRequest) {
            DataSearchRequest dataSearchRequest = (DataSearchRequest) arg;
            Optional.ofNullable(dataSearchRequest.getFlag()).orElseThrow(() -> new DataStreamException(PARAM_DATA_SEARCH_FLAG_NULL_ERROR));
            Optional.ofNullable(dataSearchRequest.getDataSourceId()).orElseThrow(() -> new DataStreamException(PARAM_DATA_SEARCH_ID_NULL_ERROR));
            Optional.ofNullable(dataSearchRequest.getTableName()).orElseThrow(() -> new DataStreamException(PARAM_DATA_SEARCH_TABLE_NAME_NULL_ERROR));
            Optional.ofNullable(dataSearchRequest.getPage()).orElseThrow(() -> new DataStreamException(PARAM_DATA_SEARCH_PAGE_NULL_ERROR));
            Optional.ofNullable(dataSearchRequest.getCount()).orElseThrow(() -> new DataStreamException(PARAM_DATA_SEARCH_COUNT_NULL_ERROR));
        } else if (arg instanceof QuerySystemLogRequest) {
            QuerySystemLogRequest querySystemLogRequest = (QuerySystemLogRequest) arg;
            Optional.ofNullable(querySystemLogRequest.getType()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_SYSTEM_LOG_TYPE_NULL_ERROR));
            Optional.ofNullable(querySystemLogRequest.getPage()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_SYSTEM_LOG_PAGE_NULL_ERROR));
            Optional.ofNullable(querySystemLogRequest.getCount()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_SYSTEM_LOG_COUNT_NULL_ERROR));
        } else if (arg instanceof QueryJobLogbackRequest) {
            QueryJobLogbackRequest queryJobLogbackRequest = (QueryJobLogbackRequest) arg;
            Optional.ofNullable(queryJobLogbackRequest.getJobType()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_JOB_LOGBACK_TYPE_NULL_ERROR));
            Optional.ofNullable(queryJobLogbackRequest.getJobId()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_JOB_LOGBACK_PAGE_NULL_ERROR));
        } else if (arg instanceof CreateMoveTaskRequest) {
            CreateMoveTaskRequest createMoveTaskRequest = (CreateMoveTaskRequest) arg;
            Optional.ofNullable(createMoveTaskRequest.getSourceObjectName()).orElseThrow(() -> new DataStreamException(PARAM_TASK_CREATE_TABLE_NULL_ERROR));
            Optional.ofNullable(createMoveTaskRequest.getSourceObjectId()).orElseThrow(() -> new DataStreamException(PARAM_TASK_CREATE_SOURCE_ID_NULL_ERROR));
            Optional.ofNullable(createMoveTaskRequest.getTargetObjectId()).orElseThrow(() -> new DataStreamException(PARAM_TASK_CREATE_TARGET_ID_NULL_ERROR));
            if (!Objects.isNull(createMoveTaskRequest.getSourceObjectCondition()) && createMoveTaskRequest.getSourceObjectCondition().contains(";")) {
                throw new DataStreamException(PARAM_TASK_CREATE_CONDITION_FORMAT_ERROR);
            }
        } else if (arg instanceof QueryTaskProgressRequest) {
            QueryTaskProgressRequest queryTaskProgressRequest = (QueryTaskProgressRequest) arg;
            Optional.ofNullable(queryTaskProgressRequest.getTaskId()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_PROGRESS_TASK_ID_NULL_ERROR));
        } else if (arg instanceof QueryDataMoveTaskRequest) {
            QueryDataMoveTaskRequest queryDataMoveTaskRequest = (QueryDataMoveTaskRequest) arg;
            Optional.ofNullable(queryDataMoveTaskRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_TASK_FLAG_NULL_ERROR));
        } else if (arg instanceof QueryDataMoveInfoRequest) {
            QueryDataMoveInfoRequest queryDataMoveInfoRequest = (QueryDataMoveInfoRequest) arg;
            Optional.ofNullable(queryDataMoveInfoRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_INFO_FLAG_NULL_ERROR));
        } else if (arg instanceof OperateDataMoveTaskRequest) {
            OperateDataMoveTaskRequest operateDataMoveTaskRequest = (OperateDataMoveTaskRequest) arg;
            Optional.ofNullable(operateDataMoveTaskRequest.getTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_OPERATE_INFO_TASK_ID_NULL_ERROR));
            Optional.ofNullable(operateDataMoveTaskRequest.getOperate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_OPERATE_INFO_TYPE_NULL_ERROR));
        } else if (arg instanceof CreateTableLinkTaskRequest) {
            CreateTableLinkTaskRequest createTableLinkTaskRequest = (CreateTableLinkTaskRequest) arg;
            Optional.ofNullable(createTableLinkTaskRequest.getSourceDataBaseId()).orElseThrow(() -> new DataStreamException(PARAM_TASK_BACK_SOURCE_ID_NULL_ERROR));
            Optional.ofNullable(createTableLinkTaskRequest.getTargetDataBaseId()).orElseThrow(() -> new DataStreamException(PARAM_TASK_BACK_TARGET_ID_NULL_ERROR));
        } else if (arg instanceof OperateTableLinkTaskRequest) {
            OperateTableLinkTaskRequest operateTableLinkTaskRequest = (OperateTableLinkTaskRequest) arg;
            Optional.ofNullable(operateTableLinkTaskRequest.getLinkTaskId()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_OPERATE_INFO_TASK_ID_NULL_ERROR));
            Optional.ofNullable(operateTableLinkTaskRequest.getOperate()).orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_OPERATE_INFO_TYPE_NULL_ERROR));
        } else if (arg instanceof QueryTableLinkTaskRequest) {
            QueryTableLinkTaskRequest queryTableLinkTaskRequest = (QueryTableLinkTaskRequest) arg;
            Optional.ofNullable(queryTableLinkTaskRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_BACK_TASK_FLAG_NULL_ERROR));
        } else if (arg instanceof QueryTableSimpleInfoRequest) {
            //todo
        } else if (arg instanceof CreateBatchTaskRequest) {
            CreateBatchTaskRequest createBatchTaskRequest = (CreateBatchTaskRequest) arg;
            Optional.ofNullable(createBatchTaskRequest.getCanalBatchTask()).orElseThrow(() -> new DataStreamException(PARAM_CREATE_BATCH_TASK_OBJECT_NULL_ERROR));
            Optional.ofNullable(createBatchTaskRequest.getCanalBatchTask().getSourceDataSourceId()).orElseThrow(() -> new DataStreamException(PARAM_CREATE_BATCH_TASK_SOURCE_ID_NULL_ERROR));
            Optional.ofNullable(createBatchTaskRequest.getCanalBatchTask().getTargetDataSourceId()).orElseThrow(() -> new DataStreamException(PARAM_CREATE_BATCH_TASK_TARGET_ID_NULL_ERROR));
        } else if (arg instanceof QueryBatchTaskRequest) {
            QueryBatchTaskRequest queryBatchTaskRequest = (QueryBatchTaskRequest) arg;
            Optional.ofNullable(queryBatchTaskRequest.getQueryFlag()).orElseThrow(() -> new DataStreamException(PARAM_QUERY_BATCH_TASK_QUERY_FLAG_NULL_ERROR));
        }
    }

    private <T> void validateRequest(T request, IErrorCode... errorCodes) throws DataStreamException {
        for (IErrorCode errorMessage : errorCodes) {
            checkNotNull(request, errorMessage);
        }
    }

    private <T> void checkNotNull(T value, IErrorCode errorCode) throws DataStreamException {
        Optional.ofNullable(value).orElseThrow(() -> new DataStreamException(errorCode));
    }
}
