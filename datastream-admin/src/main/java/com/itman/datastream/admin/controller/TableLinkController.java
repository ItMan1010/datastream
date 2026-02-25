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
import com.itman.datastream.admin.handler.TableLinkHandler;
import com.itman.datastream.admin.service.ITableLinkService;
import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@RestController
@RequestMapping("/api/link")
@RequiredArgsConstructor
public class TableLinkController {
    private final ITableLinkService tableLinkService;
    private final TableLinkHandler tableLinkHandler;
    private final IMetaService metaService;

    private void checkTableLinkTask(Long tableLinkId) throws DataStreamException {
        List<TableLinkTaskEntity> tableLinkTaskList = metaService.queryTableLinkTaskByTableLinkId(tableLinkId);
        if (!CollectionUtils.isEmpty(tableLinkTaskList)) {
            throw new DataStreamException(OPER_LINK_TASK_IS_RUN_ERROR.getCode(), "流程实例{" + tableLinkTaskList.get(0).getLinkTaskId() + "}正在运行");
        }
    }

    /**
     * http://localhost:9199/datastream/link/queryTableLink
     *
     * @param queryTableLinkRequest
     * @return
     */
    @PostMapping(path = "/queryTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTableLinkResponse> queryTableLink(@RequestBody QueryTableLinkRequest queryTableLinkRequest) {
        QueryTableLinkResponse queryTableLinkResponse = new QueryTableLinkResponse();
        try {
            queryTableLinkResponse.setTotal(tableLinkService.queryTableLinkCount(queryTableLinkRequest.getQueryFlag(), queryTableLinkRequest.getQueryValue()));
            if (queryTableLinkResponse.getTotal() > 0) {
                queryTableLinkResponse.setTableLinkList(tableLinkService.queryTableLink(queryTableLinkRequest.getQueryFlag(), queryTableLinkRequest.getQueryValue(), queryTableLinkRequest.getPage(), queryTableLinkRequest.getCount()));
            }
        } catch (DataStreamException aie) {
            queryTableLinkResponse.setErrorCode(aie.getErrCode());
            queryTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            queryTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(queryTableLinkResponse, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/link/queryLinkDetail
     *
     * @param queryLinkDetailRequest
     * @return
     */
    @PostMapping(path = "/queryLinkDetail", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryLinkDetailResponse> queryLinkDetail(@RequestBody QueryLinkDetailRequest queryLinkDetailRequest) {
        QueryLinkDetailResponse queryLinkDetailResponse = new QueryLinkDetailResponse();
        try {
            List<TableLinkEntity> tableLinkList = tableLinkService.queryTableLink(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID, queryLinkDetailRequest.getTableLinkId().toString(), 1, 10);
            if (CollectionUtils.isEmpty(tableLinkList)) {
                throw new DataStreamException(OPER_FLOW_DEFINE_NOT_EXISTS_ERROR);
            }
            queryLinkDetailResponse.setTableLinkId(queryLinkDetailRequest.getTableLinkId());
            queryLinkDetailResponse.setTableLinkName(tableLinkList.get(0).getTableLinkName());
            queryLinkDetailResponse.setTableLinkDes(tableLinkList.get(0).getTableLinkDes());
            queryLinkDetailResponse.setLinkNode(tableLinkService.queryTableLink(queryLinkDetailRequest.getTableLinkId()));
        } catch (DataStreamException aie) {
            queryLinkDetailResponse.setErrorCode(aie.getErrCode());
            queryLinkDetailResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            queryLinkDetailResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryLinkDetailResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryLinkDetailResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "新增操作", description = "'tableLinkName:'+#addLinkDefineRequest.tableLinkName")
    @PostMapping(path = "/addTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddTableLinkResponse> addTableLink(@RequestBody AddTableLinkRequest addTableLinkRequest) {
        AddTableLinkResponse addTableLinkResponse = new AddTableLinkResponse();

        try {
            if (addTableLinkRequest.getLinkNode() != null) {
                addTableLinkRequest.getLinkNode().setParentFieldName(Optional.ofNullable(addTableLinkRequest.getLinkNode().getParentFieldName()).orElse(FLOW_ROOT_PARENT_FIELD_BUSINESS_ID));
            }

            addTableLinkResponse.setTableLinkId(tableLinkService.addTableLink(addTableLinkRequest.getTableLinkName(), addTableLinkRequest.getTableLinkDes(), addTableLinkRequest.getLinkNode()));
        } catch (DataStreamException aie) {
            addTableLinkResponse.setErrorCode(aie.getErrCode());
            addTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            addTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            addTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(addTableLinkResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "修改操作", description = "'tableLinkId:'+#modifyLinkDefineRequest.tableLinkId")
    @PostMapping(path = "/modifyTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyTableLinkResponse> modifyTableLink(@RequestBody ModifyTableLinkRequest modifyTableLinkRequest) {
        ModifyTableLinkResponse modifyTableLinkResponse = new ModifyTableLinkResponse();
        try {
            if (modifyTableLinkRequest.getLinkNode() != null) {
                modifyTableLinkRequest.getLinkNode().setParentFieldName(Optional.ofNullable(modifyTableLinkRequest.getLinkNode().getParentFieldName()).orElse(FLOW_ROOT_PARENT_FIELD_BUSINESS_ID));
            }

            checkTableLinkTask(modifyTableLinkRequest.getTableLinkId());

            modifyTableLinkResponse.setTableLinkId(tableLinkService.modifyTableLink(modifyTableLinkRequest.getTableLinkId(), modifyTableLinkRequest.getTableLinkName(), modifyTableLinkRequest.getTableLinkDes(), modifyTableLinkRequest.getLinkNode()));
        } catch (DataStreamException aie) {
            modifyTableLinkResponse.setErrorCode(aie.getErrCode());
            modifyTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            modifyTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            modifyTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(modifyTableLinkResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "删除操作", description = "'tableLinkId:'+#delTableLinkRequest.tableLinkId")
    @PostMapping(path = "/delTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelTableLinkResponse> delTableLink(@RequestBody DelTableLinkRequest delTableLinkRequest) {
        DelTableLinkResponse delTableLinkResponse = new DelTableLinkResponse();
        try {
            checkTableLinkTask(delTableLinkRequest.getTableLinkId());

            delTableLinkRequest.setTableLinkId(tableLinkService.delTableLink(delTableLinkRequest.getTableLinkId()));
        } catch (DataStreamException aie) {
            delTableLinkResponse.setErrorCode(aie.getErrCode());
            delTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            delTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            delTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(delTableLinkResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "上下线数据源操作", description = "'tableLinkId:'+#onOffTableLinkRequest.tableLinkId+',state:'+#onOffFlowDefineRequest.state")
    @PostMapping(path = "/onOffTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OnOffTableLinkResponse> onOffTableLink(@RequestBody OnOffTableLinkRequest onOffTableLinkRequest) {
        OnOffTableLinkResponse onOffTableLinkResponse = new OnOffTableLinkResponse();
        try {
            checkTableLinkTask(onOffTableLinkRequest.getTableLinkId());

            onOffTableLinkResponse.setTableLinkId(tableLinkService.onOffTableLink(onOffTableLinkRequest.getTableLinkId(), onOffTableLinkRequest.getState()));
        } catch (DataStreamException aie) {
            onOffTableLinkResponse.setErrorCode(aie.getErrCode());
            onOffTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            onOffTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            onOffTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(onOffTableLinkResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "测试流程操作", description = "'tableLinkName:'+#testTableLinkRequest.tableLinkName")
    @PostMapping(path = "/testTableLink", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TestTableLinkResponse> testTableLink(@RequestBody TestTableLinkRequest testTableLinkRequest) {
        TestTableLinkResponse testTableLinkResponse = new TestTableLinkResponse();
        try {
            tableLinkHandler.testTableLink(testTableLinkRequest.getDataBaseId(), testTableLinkRequest.getLinkNode());
        } catch (DataStreamException aie) {
            testTableLinkResponse.setErrorCode(aie.getErrCode());
            testTableLinkResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            testTableLinkResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            testTableLinkResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(testTableLinkResponse, HttpStatus.OK);
    }
}
