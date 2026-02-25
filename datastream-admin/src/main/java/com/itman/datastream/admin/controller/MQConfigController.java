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
import com.itman.datastream.admin.service.IMQConfigService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IMQAdapterApi;
import com.itman.datastream.common.entity.MQConfigEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.constant.DataStreamConstant.COMMON_STATE_OFFLINE;
import static com.itman.datastream.common.constant.DataStreamConstant.DATA_STREAM_ACTION_OFFLINE;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;


@Slf4j
@RestController
@RequestMapping("/api/mq")
@RequiredArgsConstructor
public class MQConfigController {

    private final IMQConfigService mqConfigService;
    private final DataSourceFactory dataSourceFactory;

    public IMQAdapterApi matchMQ(Integer dataSourceType) throws DataStreamException {
        return this.dataSourceFactory.matchMQ(dataSourceType);
    }

    @PostMapping(path = "/queryMqRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryMqRowsResponse> queryMqRows(@RequestBody QueryMqRowsRequest request) {
        QueryMqRowsResponse response = new QueryMqRowsResponse();
        try {
            response.setMqConfigList(new ArrayList<>());
            Integer recordCount = mqConfigService.getConfigCount(request.getQueryFlag(), request.getQueryValue());
            response.setTotal(recordCount);
            if (recordCount > 0) {
                response.setMqConfigList(mqConfigService.queryConfigByPage(request.getQueryFlag(), request.getQueryValue(), request.getPage(), request.getCount()));
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

    @PostMapping(path = "/queryMqInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryMqInfoResponse> queryMqInfo(@RequestBody QueryMqInfoRequest request) {
        QueryMqInfoResponse response = new QueryMqInfoResponse();
        try {
            MQConfigEntity mqConfig = mqConfigService.getConfigById(request.getMqConfigId());
            response.setMqConfig(mqConfig);
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


    @LogOperate(operateType = 2, moduleName = "新增MQ配置", description = "'mqConfigName:'+#request.mqConfig.mqConfigName")
    @PostMapping(path = "/addMqConfig", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddMqConfigResponse> addMqConfig(@RequestBody AddMqConfigRequest request) {
        AddMqConfigResponse response = new AddMqConfigResponse();
        try {
            MQConfigEntity mqConfig = request.getMqConfig();
            validateMqConfig(mqConfig);

            Long mqConfigId = mqConfigService.insertConfig(mqConfig);
            response.setMqConfigId(mqConfigId);
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

    @LogOperate(operateType = 2, moduleName = "修改MQ配置", description = "'mqConfigId:'+#request.mqConfig.mqConfigId")
    @PostMapping(path = "/modifyMqConfig", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyMqConfigResponse> modifyMqConfig(@RequestBody ModifyMqConfigRequest request) {
        ModifyMqConfigResponse response = new ModifyMqConfigResponse();
        try {
            MQConfigEntity mqConfig = request.getMqConfig();
            validateMqConfig(mqConfig);
            if (mqConfig.getMqConfigId() == null) {
                throw new DataStreamException("MQ_0013", "实例标识不能为空");
            }

            MQConfigEntity mqConfigTemp = mqConfigService.getConfigById(mqConfig.getMqConfigId());
            if (mqConfigTemp.getOnLineFlag().equals(COMMON_STATE_ONLINE)) {
                throw new DataStreamException("MQ_0013", "在线状态不能删除操作");
            }

            mqConfigService.updateConfig(mqConfig);
            response.setMqConfigId(mqConfig.getMqConfigId());
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


    @LogOperate(operateType = 2, moduleName = "删除MQ配置", description = "'mqConfigId:'+#request.mqConfigId")
    @PostMapping(path = "/delMqConfig", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelMqConfigResponse> delMqConfig(@RequestBody DelMqConfigRequest request) {
        DelMqConfigResponse response = new DelMqConfigResponse();
        try {
            MQConfigEntity mqConfig = mqConfigService.getConfigById(request.getMqConfigId());
            if (mqConfig.getOnLineFlag().equals(COMMON_STATE_ONLINE)) {
                throw new DataStreamException("MQ_0013", "在线状态不能删除操作");
            }

            mqConfigService.deleteConfig(request.getMqConfigId());
            response.setMqConfigId(request.getMqConfigId());
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

    @LogOperate(operateType = 2, moduleName = "测试MQ连接", description = "'mqConfigId:'+#request.mqConfigId")
    @PostMapping(path = "/testMqConfig", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<TestMqConfigResponse> testMqConfig(@RequestBody TestMqConfigRequest request) {
        TestMqConfigResponse response = new TestMqConfigResponse();
        try {
            MQConfigEntity mqConfig = mqConfigService.getConfigById(request.getMqConfigId());
            if (mqConfig == null) {
                throw new DataStreamException("MQ_001", "MQ配置不存在");
            }

            IMQAdapterApi mqAdapterApi = matchMQ(mqConfig.getMqType());
            mqAdapterApi.testMqConnection(mqConfig.getBootstrapServers());
        } catch (DataStreamException e) {
            response.setErrorCode(e.getErrCode());
            response.setErrorMsg(e.getErrMsg());
            log.error("DataStreamException=", e);
        } catch (Exception e) {
            response.setErrorCode(UNKNOWN_ERROR.getCode());
            response.setErrorMsg("MQ连接测试失败：" + e.getMessage());
            log.error("Exception=", e);
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    private void validateMqConfig(MQConfigEntity mqConfig) throws DataStreamException {
        if (mqConfig == null) {
            throw new DataStreamException("MQ_002", "MQ配置不能为空");
        }
        if (StringUtils.isEmpty(mqConfig.getMqConfigName())) {
            throw new DataStreamException("MQ_003", "实例名称不能为空");
        }
        if (StringUtils.isEmpty(mqConfig.getBootstrapServers())) {
            throw new DataStreamException("MQ_004", "服务地址不能为空");
        }
        if (mqConfig.getMessageFormat() == null) {
            throw new DataStreamException("MQ_005", "报文格式不能为空");
        }
        // 分隔符格式时，校验分隔符
        if (mqConfig.getMessageFormat() == 2 && StringUtils.isEmpty(mqConfig.getDelimiter())) {
            throw new DataStreamException("MQ_006", "分隔符格式时分隔符不能为空");
        }
    }

    @PostMapping(path = "/operateMqConfig", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AbstractResponse> operateMqConfig(@RequestBody OperateMqConfigRequest operateMqConfigRequest) {
        AbstractResponse operateMqConfigResponse = new AbstractResponse();
        try {
            Integer action = operateMqConfigRequest.getAction();
            Long mqConfigId = operateMqConfigRequest.getMqConfigId();

            if (action.equals(DATA_STREAM_ACTION_ONLINE)) {
                //上线
                mqConfigService.updateConfigOnLineFlagById(mqConfigId, COMMON_STATE_ONLINE);
            } else if (action.equals(DATA_STREAM_ACTION_OFFLINE)) {
                //todo 下线：先校验，校验关联的任务配置表是否都是下线状态
                mqConfigService.updateConfigOnLineFlagById(mqConfigId, COMMON_STATE_OFFLINE);
            }
        } catch (DataStreamException aie) {
            operateMqConfigResponse.setErrorCode(aie.getErrCode());
            operateMqConfigResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            operateMqConfigResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            operateMqConfigResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(operateMqConfigResponse, HttpStatus.OK);
    }
}

