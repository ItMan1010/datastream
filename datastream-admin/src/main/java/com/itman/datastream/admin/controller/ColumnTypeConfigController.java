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

import com.itman.datastream.admin.controller.domain.request.AddTypeDefineRequest;
import com.itman.datastream.admin.controller.domain.request.AddTypeMapRequest;
import com.itman.datastream.admin.controller.domain.request.DelTypeDefineRequest;
import com.itman.datastream.admin.controller.domain.request.DelTypeMapRequest;
import com.itman.datastream.admin.controller.domain.request.ModifyTypeDefineRequest;
import com.itman.datastream.admin.controller.domain.request.ModifyTypeMapRequest;
import com.itman.datastream.admin.controller.domain.request.QueryTypeDefineRowsRequest;
import com.itman.datastream.admin.controller.domain.request.QueryTypeMapRowsRequest;
import com.itman.datastream.admin.controller.domain.request.TypeDefineIdRequest;
import com.itman.datastream.admin.controller.domain.request.TypeMapIdRequest;
import com.itman.datastream.admin.controller.domain.response.AddTypeDefineResponse;
import com.itman.datastream.admin.controller.domain.response.AddTypeMapResponse;
import com.itman.datastream.admin.controller.domain.response.DelTypeDefineResponse;
import com.itman.datastream.admin.controller.domain.response.DelTypeMapResponse;
import com.itman.datastream.admin.controller.domain.response.ModifyTypeDefineResponse;
import com.itman.datastream.admin.controller.domain.response.ModifyTypeMapResponse;
import com.itman.datastream.admin.controller.domain.response.QueryAllTypeDefineResponse;
import com.itman.datastream.admin.controller.domain.response.QueryTypeDefineRowsResponse;
import com.itman.datastream.admin.controller.domain.response.QueryTypeMapRowsResponse;
import com.itman.datastream.admin.controller.domain.response.TypeDefineInfoResponse;
import com.itman.datastream.admin.controller.domain.response.TypeMapInfoResponse;
import com.itman.datastream.admin.service.IColumnTypeConfigService;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/columnTypeConfig")
@RequiredArgsConstructor
public class ColumnTypeConfigController {

    private final IColumnTypeConfigService columnTypeConfigService;

    @PostMapping(path = "/queryTypeDefineRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTypeDefineRowsResponse> queryTypeDefineRows(@RequestBody QueryTypeDefineRowsRequest request) {
        QueryTypeDefineRowsResponse response = new QueryTypeDefineRowsResponse();
        try {
            Integer recordCount = columnTypeConfigService.getTypeDefineCount(request.getQueryFlag(), request.getQueryValue());
            response.setTotal(recordCount);
            if (recordCount > 0) {
                response.setTypeDefineList(columnTypeConfigService.queryTypeDefineByPage(request.getQueryFlag(), request.getQueryValue(), request.getPage(), request.getCount()));
            }
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/queryAllTypeDefine", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryAllTypeDefineResponse> queryAllTypeDefine() {
        QueryAllTypeDefineResponse response = new QueryAllTypeDefineResponse();
        try {
            response.setTypeDefineList(columnTypeConfigService.queryAllTypeDefine());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTypeDefineInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TypeDefineInfoResponse> queryTypeDefineInfo(@RequestBody TypeDefineIdRequest request) {
        TypeDefineInfoResponse response = new TypeDefineInfoResponse();
        try {
            response.setTypeDefine(columnTypeConfigService.getTypeDefineById(request.getColumnTypeDefineId()));
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "新增类型定义", description = "'columnTypeName:'+#request.typeDefine.columnTypeName")
    @PostMapping(path = "/addTypeDefine", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddTypeDefineResponse> addTypeDefine(@RequestBody AddTypeDefineRequest request) {
        AddTypeDefineResponse response = new AddTypeDefineResponse();
        try {
            Long columnTypeDefineId = columnTypeConfigService.addTypeDefine(request.getTypeDefine());
            response.setColumnTypeDefineId(columnTypeDefineId);
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "修改类型定义", description = "'columnTypeDefineId:'+#request.typeDefine.columnTypeDefineId")
    @PostMapping(path = "/modifyTypeDefine", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyTypeDefineResponse> modifyTypeDefine(@RequestBody ModifyTypeDefineRequest request) {
        ModifyTypeDefineResponse response = new ModifyTypeDefineResponse();
        try {
            columnTypeConfigService.modifyTypeDefine(request.getTypeDefine());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "删除类型定义", description = "'columnTypeDefineId:'+#request.columnTypeDefineId")
    @PostMapping(path = "/delTypeDefine", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelTypeDefineResponse> delTypeDefine(@RequestBody DelTypeDefineRequest request) {
        DelTypeDefineResponse response = new DelTypeDefineResponse();
        try {
            columnTypeConfigService.delTypeDefine(request.getColumnTypeDefineId());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTypeMapRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTypeMapRowsResponse> queryTypeMapRows(@RequestBody QueryTypeMapRowsRequest request) {
        QueryTypeMapRowsResponse response = new QueryTypeMapRowsResponse();
        try {
            Integer recordCount = columnTypeConfigService.getTypeMapCount(request.getQueryFlag(), request.getQueryValue());
            response.setTotal(recordCount);
            if (recordCount > 0) {
                response.setTypeMapList(columnTypeConfigService.queryTypeMapByPage(request.getQueryFlag(), request.getQueryValue(), request.getPage(), request.getCount()));
            }
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(path = "/queryTypeMapInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TypeMapInfoResponse> queryTypeMapInfo(@RequestBody TypeMapIdRequest request) {
        TypeMapInfoResponse response = new TypeMapInfoResponse();
        try {
            response.setTypeMap(columnTypeConfigService.getTypeMapById(request.getColumnTypeMapId()));
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "新增类型映射", description = "'columnTypeNameA:'+#request.typeMap.columnTypeNameA")
    @PostMapping(path = "/addTypeMap", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddTypeMapResponse> addTypeMap(@RequestBody AddTypeMapRequest request) {
        AddTypeMapResponse response = new AddTypeMapResponse();
        try {
            Long columnTypeMapId = columnTypeConfigService.addTypeMap(request.getTypeMap());
            response.setColumnTypeMapId(columnTypeMapId);
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "修改类型映射", description = "'columnTypeMapId:'+#request.typeMap.columnTypeMapId")
    @PostMapping(path = "/modifyTypeMap", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyTypeMapResponse> modifyTypeMap(@RequestBody ModifyTypeMapRequest request) {
        ModifyTypeMapResponse response = new ModifyTypeMapResponse();
        try {
            columnTypeConfigService.modifyTypeMap(request.getTypeMap());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "删除类型映射", description = "'columnTypeMapId:'+#request.columnTypeMapId")
    @PostMapping(path = "/delTypeMap", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelTypeMapResponse> delTypeMap(@RequestBody DelTypeMapRequest request) {
        DelTypeMapResponse response = new DelTypeMapResponse();
        try {
            columnTypeConfigService.delTypeMap(request.getColumnTypeMapId());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}