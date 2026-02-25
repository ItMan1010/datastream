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

import com.itman.datastream.admin.controller.domain.request.DataSearchRequest;
import com.itman.datastream.admin.controller.domain.response.DataSearchResponse;
import com.itman.datastream.admin.handler.DataBaseHandler;
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

import java.util.Objects;

import static com.itman.datastream.common.errcode.DataStreamErrorCode.PARAM_DATA_SEARCH_CONDITION_FORMAT_ERROR;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class DataSearchController {
    private final DataBaseHandler dataBaseHandler;

    @PostMapping(path = "/dataSearch", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DataSearchResponse> dataSearch(@RequestBody DataSearchRequest dataSearchRequest) {
        DataSearchResponse dataSearchResponse = new DataSearchResponse();
        try {
            if (!Objects.isNull(dataSearchRequest.getQueryCondition()) && dataSearchRequest.getQueryCondition().contains(";")) {
                throw new DataStreamException(PARAM_DATA_SEARCH_CONDITION_FORMAT_ERROR);
            }

            dataBaseHandler.dataSearch(dataSearchRequest, dataSearchResponse);
        } catch (DataStreamException aie) {
            dataSearchResponse.setErrorCode(aie.getErrCode());
            dataSearchResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            dataSearchResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            dataSearchResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(dataSearchResponse, HttpStatus.OK);
    }
}
