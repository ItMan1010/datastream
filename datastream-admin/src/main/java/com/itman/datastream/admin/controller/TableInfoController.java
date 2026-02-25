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

import com.itman.datastream.admin.controller.domain.request.QueryTableColumnInfoRequest;
import com.itman.datastream.admin.controller.domain.request.QueryTableListRequest;
import com.itman.datastream.admin.controller.domain.response.QueryTableColumnInfoResponse;
import com.itman.datastream.admin.controller.domain.response.QueryTableListResponse;
import com.itman.datastream.admin.handler.TableInfoHandler;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.entity.TableInfoEntity;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/table")
@RequiredArgsConstructor
public class TableInfoController {
    private final TableInfoHandler tableInfoHandler;

    @PostMapping(path = "/tableList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTableListResponse> getTableList(@RequestBody QueryTableListRequest queryTableListRequest) {
        QueryTableListResponse response = new QueryTableListResponse();
        try {
            // 获取表列表数据并设置到响应中
            List<TableInfoEntity> tableList = tableInfoHandler.getTableList(queryTableListRequest.getDataBaseId());
            response.setTableInfoEntityList(tableList);
        } catch (DataStreamException aie) {
            response.setErrorCode(aie.getErrCode());
            response.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * http://localhost:9199/datastream/table/getTableColumnInfo
     */
    @PostMapping(path = "/getTableColumnInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryTableColumnInfoResponse> getTableColumnInfo(@RequestBody QueryTableColumnInfoRequest queryTableColumnInfoRequest) {
        QueryTableColumnInfoResponse response = new QueryTableColumnInfoResponse();
        try {
            List<TableColumnEntity> tableColumnList = tableInfoHandler.getTableColumnInfo(queryTableColumnInfoRequest.getDataSourceId(), queryTableColumnInfoRequest.getTableName());
            response.setTableColumnList(tableColumnList);
        } catch (DataStreamException aie) {
            response.setErrorCode(aie.getErrCode());
            response.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
