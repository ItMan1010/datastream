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

import com.itman.datastream.admin.controller.domain.request.AddUserRequest;
import com.itman.datastream.admin.controller.domain.request.ModifyUserRequest;
import com.itman.datastream.admin.controller.domain.request.QueryUserRowsRequest;
import com.itman.datastream.admin.controller.domain.request.ResetUserPasswordRequest;
import com.itman.datastream.admin.controller.domain.request.SystemUserIdRequest;
import com.itman.datastream.admin.controller.domain.request.UpdateUserStateRequest;
import com.itman.datastream.admin.controller.domain.response.AddUserResponse;
import com.itman.datastream.admin.controller.domain.response.DelUserResponse;
import com.itman.datastream.admin.controller.domain.response.ModifyUserResponse;
import com.itman.datastream.admin.controller.domain.response.QueryUserRowsResponse;
import com.itman.datastream.admin.controller.domain.response.ResetUserPasswordResponse;
import com.itman.datastream.admin.controller.domain.response.UpdateUserStateResponse;
import com.itman.datastream.admin.controller.domain.response.UserInfoResponse;
import com.itman.datastream.admin.service.ISystemUserService;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.security.annotation.LogOperate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.itman.datastream.common.errcode.DataStreamErrorCode.UNKNOWN_ERROR;

@Slf4j
@RestController
@RequestMapping("/api/systemUser")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class SystemUserController {

    private final ISystemUserService systemUserService;

    @PostMapping(path = "/queryUserRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryUserRowsResponse> queryUserRows(@RequestBody QueryUserRowsRequest request) {
        QueryUserRowsResponse response = new QueryUserRowsResponse();
        try {
            Integer recordCount = systemUserService.getUserCount(request.getQueryFlag(), request.getQueryValue());
            response.setTotal(recordCount);
            if (recordCount > 0) {
                response.setUserList(systemUserService.queryUserByPage(request.getQueryFlag(), request.getQueryValue(), request.getPage(), request.getCount()));
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

    @PostMapping(path = "/queryUserInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UserInfoResponse> queryUserInfo(@RequestBody SystemUserIdRequest request) {
        UserInfoResponse response = new UserInfoResponse();
        try {
            response.setUser(systemUserService.getUserById(request.getSystemUserId()));
            response.setRoleIds(systemUserService.getRoleIdsByUserId(request.getSystemUserId()));
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

    @LogOperate(operateType = 2, moduleName = "新增系统用户", description = "'systemUserCode:'+#request.user.systemUserCode")
    @PostMapping(path = "/addUser", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddUserResponse> addUser(@RequestBody AddUserRequest request) {
        AddUserResponse response = new AddUserResponse();
        try {
            Long systemUserId = systemUserService.addUser(request.getUser(), request.getRoleIds());
            response.setSystemUserId(systemUserId);
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

    @LogOperate(operateType = 2, moduleName = "修改系统用户", description = "'systemUserId:'+#request.user.systemUserId")
    @PostMapping(path = "/modifyUser", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyUserResponse> modifyUser(@RequestBody ModifyUserRequest request) {
        ModifyUserResponse response = new ModifyUserResponse();
        try {
            systemUserService.modifyUser(request.getUser(), request.getRoleIds());
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

    @LogOperate(operateType = 2, moduleName = "修改系统用户状态", description = "'systemUserId:'+#request.systemUserId+',state:'+#request.state")
    @PostMapping(path = "/updateUserState", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<UpdateUserStateResponse> updateUserState(@RequestBody UpdateUserStateRequest request) {
        UpdateUserStateResponse response = new UpdateUserStateResponse();
        try {
            systemUserService.updateUserState(request.getSystemUserId(), request.getState());
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

    @LogOperate(operateType = 2, moduleName = "重置系统用户密码", description = "'systemUserId:'+#request.systemUserId")
    @PostMapping(path = "/resetPassword", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ResetUserPasswordResponse> resetPassword(@RequestBody ResetUserPasswordRequest request) {
        ResetUserPasswordResponse response = new ResetUserPasswordResponse();
        try {
            systemUserService.resetPassword(request.getSystemUserId(), request.getPassword());
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

    @LogOperate(operateType = 2, moduleName = "删除系统用户", description = "'systemUserId:'+#request.systemUserId")
    @PostMapping(path = "/delUser", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelUserResponse> delUser(@RequestBody SystemUserIdRequest request) {
        DelUserResponse response = new DelUserResponse();
        try {
            systemUserService.delUser(request.getSystemUserId());
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