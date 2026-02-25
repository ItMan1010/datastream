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
import com.itman.datastream.admin.handler.DataBaseHandler;
import com.itman.datastream.common.utils.AESUtils;
import com.itman.datastream.common.utils.CommUtils;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.*;
import java.util.function.Consumer;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.parseJdbcUrl;

@Slf4j
@RestController
@RequestMapping("/api/database")
@RequiredArgsConstructor
public class DataBaseController {
    private final DataBaseHandler dataBaseHandler;

    private void checkDataBaseType(Integer dataBaseType, String url) throws DataStreamException {
        if (!CommUtils.isDataBaseDataSource(dataBaseType)) {
            throw new DataStreamException(PARAM_DATA_BASE_TYPE_NOT_EQUAL_ERROR);
        }

        String dataBaseTypeName = parseJdbcUrl(url);
        if (dataBaseTypeName == null) {
            throw new DataStreamException(PARAM_DATA_BASE_URL_PARSE_ERROR);
        }
        log.info("dataBaseTypeName:{},url={}", dataBaseTypeName,url);

        if (!dataBaseTypeName.equalsIgnoreCase(CommUtils.jdbcUrlDataBaseTypeMap.get(dataBaseType))) {
            throw new DataStreamException(PARAM_DATA_BASE_PARSE_NOT_EQUAL_ERROR);
        }
    }

    @LogOperate(operateType = 2, moduleName = "修改数据源操作", description = "'dataBaseId:'+#modifyDataBaseRequest.dataBaseId")
    @PostMapping(path = "/modifyDataBase", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyDataBaseResponse> modifyDataBase(@RequestBody ModifyDataBaseRequest modifyDataBaseRequest) {
        ModifyDataBaseResponse modifyDataBaseResponse = new ModifyDataBaseResponse();
        try {
            checkDataBaseType(modifyDataBaseRequest.getDataBaseType(), modifyDataBaseRequest.getUrl());

            DataBaseEntity dataBase = new DataBaseEntity();
            BeanUtils.copyProperties(modifyDataBaseRequest, dataBase);

            modifyDataBaseResponse.setDataSourceId(dataBaseHandler.updateDataBase(dataBase));
        } catch (DataStreamException aie) {
            modifyDataBaseResponse.setErrorCode(aie.getErrCode());
            modifyDataBaseResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            modifyDataBaseResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            modifyDataBaseResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(modifyDataBaseResponse, HttpStatus.OK);
    }


    @LogOperate(operateType = 2, moduleName = "测试数据连接操作", description = "'dataBaseName:'+#testDataBaseRequest.dataBaseName")
    @PostMapping(path = "/testDataBase", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TestDataBaseResponse> testDataBase(@RequestBody TestDataBaseRequest testDataBaseRequest) {
        TestDataBaseResponse testDataBaseResponse = new TestDataBaseResponse();
        try {
            checkDataBaseType(testDataBaseRequest.getDataBaseType(), testDataBaseRequest.getUrl());

            DataBaseEntity dataBase = new DataBaseEntity();
            BeanUtils.copyProperties(testDataBaseRequest, dataBase);
            if (!testDataBaseRequest.getDataBaseType().equals(DATA_SOURCE_TYPE_ORACLE) && !dataBase.getUrl().contains("?")) {
                dataBase.setUrl(dataBase.getUrl() + "?");
            }

            if (!testDataBaseRequest.getDataBaseType().equals(DATA_SOURCE_TYPE_ORACLE) && !dataBase.getUrl().contains("socketTimeout")) {
                dataBase.setUrl(dataBase.getUrl() + ((dataBase.getUrl().charAt(dataBase.getUrl().length() - 1) != '?' && !dataBase.getUrl().contains("&")) ? "&" : "") + "socketTimeout=5000");
            }

            if (log.isDebugEnabled()) {
                log.debug("testDataBase_Url={}", dataBase.getUrl());
            }

            dataBaseHandler.testDataDase(dataBase);
        } catch (DataStreamException aie) {
            testDataBaseResponse.setErrorCode(aie.getErrCode());
            testDataBaseResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            testDataBaseResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            testDataBaseResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(testDataBaseResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryDataBaseRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryDataBaseRowsResponse> queryDataBaseRows(@RequestBody QueryDataBaseRowsRequest queryDataBaseRowsRequest) {
        QueryDataBaseRowsResponse queryDataBaseRowsResponse = new QueryDataBaseRowsResponse();
        try {
            Long queryValue = null;
            switch (queryDataBaseRowsRequest.getQueryFlag()) {
                case DATA_BASE_QUERY_FLAG_TYPE:
                    queryValue = (long) queryDataBaseRowsRequest.getDataBaseType();
                    break;
                case DATA_BASE_QUERY_FLAG_ID:
                    queryValue = queryDataBaseRowsRequest.getDataBaseId();
                    break;
            }

            queryDataBaseRowsResponse.setDataBaseList(new ArrayList<>());
            Integer recordCount = dataBaseHandler.getDataBaseCount(queryDataBaseRowsRequest.getQueryFlag(), queryValue, queryDataBaseRowsRequest.getState());
            queryDataBaseRowsResponse.setTotal(recordCount);
            if (recordCount > 0) {
                queryDataBaseRowsResponse.setDataBaseList(dataBaseHandler.queryDataBase(queryDataBaseRowsRequest.getQueryFlag(), queryValue, queryDataBaseRowsRequest.getState(), queryDataBaseRowsRequest.getPage(), queryDataBaseRowsRequest.getCount()));
                Consumer<DataBaseEntity> canalDataSourceConsumer = x -> x.setPassWordLength(AESUtils.decrypt(x.getPassWord()).length());
                queryDataBaseRowsResponse.getDataBaseList().forEach(canalDataSourceConsumer);
            }
        } catch (DataStreamException aie) {
            queryDataBaseRowsResponse.setErrorCode(aie.getErrCode());
            queryDataBaseRowsResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            queryDataBaseRowsResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryDataBaseRowsResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryDataBaseRowsResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "新增数据源操作", description = "'dataBaseName:'+#addDataBaseRequest.dataBaseName")
    @PostMapping(path = "/addDataBase", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddDataBaseResponse> addDataBase(@RequestBody AddDataBaseRequest addDataBaseRequest) {
        AddDataBaseResponse addDataBaseResponse = new AddDataBaseResponse();

        try {
            checkDataBaseType(addDataBaseRequest.getDataBaseType(), addDataBaseRequest.getUrl());

            DataBaseEntity dataBase = new DataBaseEntity();
            BeanUtils.copyProperties(addDataBaseRequest, dataBase);

            addDataBaseResponse.setDataBaseId(dataBaseHandler.insertDataBase(dataBase));
        } catch (DataStreamException aie) {
            addDataBaseResponse.setErrorCode(aie.getErrCode());
            addDataBaseResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            addDataBaseResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            addDataBaseResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("addDataBaseResponse={}", addDataBaseResponse);
        }
        return new ResponseEntity<>(addDataBaseResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "删除数据源操作", description = "'dataBaseId:'+#delDataBaseRequest.dataBaseId")
    @PostMapping(path = "/delDataBase", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelDataBaseResponse> delDataBase(@RequestBody DelDataBaseRequest delDataBaseRequest) {
        DelDataBaseResponse delDataBaseResponse = new DelDataBaseResponse();
        try {
            delDataBaseResponse.setDataBaseId(dataBaseHandler.delDataBase(delDataBaseRequest.getDataBaseId()));
        } catch (DataStreamException aie) {
            delDataBaseResponse.setErrorCode(aie.getErrCode());
            delDataBaseResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            delDataBaseResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            delDataBaseResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(delDataBaseResponse, HttpStatus.OK);
    }

    @LogOperate(operateType = 2, moduleName = "上下线数据源操作", description = "'dataSourceId:'+#onOffDataBaseRequest.dataBaseId+',state:'+#onOffDataBaseRequest.state")
    @PostMapping(path = "/onOffDataBase", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OnOffDataBaseResponse> onOffDataBase(@RequestBody OnOffDataBaseRequest onOffDataBaseRequest) {
        OnOffDataBaseResponse onOffDataBaseResponse = new OnOffDataBaseResponse();
        try {
            onOffDataBaseResponse.setDataBaseId(dataBaseHandler.onOffDataBase(onOffDataBaseRequest.getDataBaseId(), onOffDataBaseRequest.getState()));
        } catch (DataStreamException aie) {
            onOffDataBaseResponse.setErrorCode(aie.getErrCode());
            onOffDataBaseResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            onOffDataBaseResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            onOffDataBaseResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(onOffDataBaseResponse, HttpStatus.OK);
    }

}
