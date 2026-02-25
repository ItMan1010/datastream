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

import com.itman.datastream.admin.controller.domain.request.StatSystemInfoRequest;
import com.itman.datastream.admin.controller.domain.response.StatSystemInfoResponse;
import com.itman.datastream.admin.handler.DataBaseHandler;
import com.itman.datastream.common.entity.StatSystemInfoEntity;
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

import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/stat")
@RequiredArgsConstructor
public class StatSystemController {
    private final DataBaseHandler dataBaseHandler;

    @PostMapping(path = "/statSystemInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<StatSystemInfoResponse> statSystemInfo(@RequestBody StatSystemInfoRequest statSystemInfoRequest) {
        StatSystemInfoResponse statSystemInfoResponse = new StatSystemInfoResponse();

        try {
            StatSystemInfoEntity statSystemInfoEntity = new StatSystemInfoEntity();
            dataBaseHandler.statSystemInfo(statSystemInfoRequest.getDays(), statSystemInfoEntity);
            statSystemInfoResponse.setStatSystemInfoEntity(statSystemInfoEntity);
        } catch (DataStreamException aie) {
            statSystemInfoResponse.setErrorCode(aie.getErrCode());
            statSystemInfoResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            statSystemInfoResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            statSystemInfoResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(statSystemInfoResponse, HttpStatus.OK);
    }
}
