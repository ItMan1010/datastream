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

import com.itman.datastream.admin.controller.domain.request.QueryJobLogbackRequest;
import com.itman.datastream.admin.controller.domain.request.QuerySystemLogRequest;
import com.itman.datastream.admin.controller.domain.response.QueryJobLogbackResponse;
import com.itman.datastream.admin.controller.domain.response.QuerySystemLogResponse;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.systemlog.ISystemLogService;
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
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class SystemLogController {
    private final ISystemLogService systemLogService;

    @PostMapping(path = "/queryJobLogback", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryJobLogbackResponse> queryJobLogback(@RequestBody QueryJobLogbackRequest queryJobLogbackRequest) {
        QueryJobLogbackResponse queryJobLogbackResponse = new QueryJobLogbackResponse();
        try {
            queryJobLogbackResponse.setJobLogbackList(systemLogService.getJobLogback(queryJobLogbackRequest.getJobType(), queryJobLogbackRequest.getJobId(), 1, 10));
            queryJobLogbackResponse.setTotal(queryJobLogbackResponse.getJobLogbackList().size());
        } catch (DataStreamException aie) {
            queryJobLogbackResponse.setErrorCode(aie.getErrCode());
            queryJobLogbackResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            queryJobLogbackResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryJobLogbackResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        if (log.isDebugEnabled()) {
            log.debug("queryJobLogbackResponse={}", queryJobLogbackResponse);
        }
        return new ResponseEntity<>(queryJobLogbackResponse, HttpStatus.OK);
    }


    @PostMapping(path = "/querySystemLog", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QuerySystemLogResponse> querySystemLog(@RequestBody QuerySystemLogRequest querySystemLogRequest) {
        QuerySystemLogResponse querySystemLogResponse = new QuerySystemLogResponse();
        try {
            querySystemLogResponse.setTotal(systemLogService.getSystemLogCount(querySystemLogRequest.getType()));
            if (querySystemLogResponse.getTotal() > 0) {
                querySystemLogResponse.setCanalSystemLogList(systemLogService.getSystemLog(querySystemLogRequest.getType(), querySystemLogRequest.getPage(), querySystemLogRequest.getCount()));
            }
        } catch (DataStreamException aie) {
            querySystemLogResponse.setErrorCode(aie.getErrCode());
            querySystemLogResponse.setErrorMsg(aie.getErrMsg());
            log.error("SystemException=", aie);
        } catch (Exception e) {
            querySystemLogResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            querySystemLogResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(querySystemLogResponse, HttpStatus.OK);
    }

}
