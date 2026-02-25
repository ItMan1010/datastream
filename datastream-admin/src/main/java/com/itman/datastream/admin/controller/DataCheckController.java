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

import com.itman.datastream.admin.controller.domain.request.QueryDataCheckListRequest;
import com.itman.datastream.admin.controller.domain.request.RepairDataCheckRequest;
import com.itman.datastream.admin.controller.domain.response.AbstractResponse;
import com.itman.datastream.admin.controller.domain.response.QueryDataCheckListResponse;
import com.itman.datastream.admin.handler.DataCheckHandler;
import com.itman.datastream.common.entity.DataCheckEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.DATA_CHECK_STATE_MODIFY_SUCCESS;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/check")
@RequiredArgsConstructor
public class DataCheckController {
    private final DataCheckHandler dataCheckHandler;

    @PostMapping(path = "/queryDataCheckList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryDataCheckListResponse> queryDataCheckList(@RequestBody QueryDataCheckListRequest queryDataCheckListRequest) {
        QueryDataCheckListResponse response = new QueryDataCheckListResponse();
        try {
            List<DataCheckEntity> dataCheckList = dataCheckHandler.getDataCheck(queryDataCheckListRequest.getTaskId());
            response.setDataCheckList(dataCheckList);
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

    @PostMapping(path = "/repairDataCheck", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AbstractResponse> repairDataCheck(@RequestBody RepairDataCheckRequest repairDataCheckRequest) {
        AbstractResponse response = new AbstractResponse();
        try {
            dataCheckHandler.repairDataCheck(repairDataCheckRequest.getCheckType(), repairDataCheckRequest.getTaskId(), repairDataCheckRequest.getDataCheckId());
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
