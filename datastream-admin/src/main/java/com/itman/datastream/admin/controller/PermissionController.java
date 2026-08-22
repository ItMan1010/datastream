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

import com.itman.datastream.admin.controller.domain.request.AddPermissionRequest;
import com.itman.datastream.admin.controller.domain.request.ModifyPermissionRequest;
import com.itman.datastream.admin.controller.domain.request.PermissionIdRequest;
import com.itman.datastream.admin.controller.domain.response.AddPermissionResponse;
import com.itman.datastream.admin.controller.domain.response.DelPermissionResponse;
import com.itman.datastream.admin.controller.domain.response.ModifyPermissionResponse;
import com.itman.datastream.admin.controller.domain.response.QueryDataPermissionListResponse;
import com.itman.datastream.admin.controller.domain.response.QueryMenuTreeResponse;
import com.itman.datastream.admin.service.IPermissionService;
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
@RequestMapping("/api/permission")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class PermissionController {

    private final IPermissionService permissionService;

    @PostMapping(path = "/queryMenuTree", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryMenuTreeResponse> queryMenuTree() {
        QueryMenuTreeResponse response = new QueryMenuTreeResponse();
        try {
            response.setMenuList(permissionService.queryMenuTree());
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

    @PostMapping(path = "/queryDataPermissionList", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryDataPermissionListResponse> queryDataPermissionList() {
        QueryDataPermissionListResponse response = new QueryDataPermissionListResponse();
        try {
            response.setDataPermissionList(permissionService.queryDataPermissionList());
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

    @LogOperate(operateType = 2, moduleName = "新增权限资源", description = "'permissionCode:'+#request.permission.permissionCode")
    @PostMapping(path = "/addPermission", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddPermissionResponse> addPermission(@RequestBody AddPermissionRequest request) {
        AddPermissionResponse response = new AddPermissionResponse();
        try {
            Long permissionId = permissionService.addPermission(request.getPermission());
            response.setPermissionId(permissionId);
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

    @LogOperate(operateType = 2, moduleName = "修改权限资源", description = "'permissionId:'+#request.permission.permissionId")
    @PostMapping(path = "/modifyPermission", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyPermissionResponse> modifyPermission(@RequestBody ModifyPermissionRequest request) {
        ModifyPermissionResponse response = new ModifyPermissionResponse();
        try {
            permissionService.modifyPermission(request.getPermission());
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

    @LogOperate(operateType = 2, moduleName = "删除权限资源", description = "'permissionId:'+#request.permissionId")
    @PostMapping(path = "/delPermission", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelPermissionResponse> delPermission(@RequestBody PermissionIdRequest request) {
        DelPermissionResponse response = new DelPermissionResponse();
        try {
            permissionService.delPermission(request.getPermissionId());
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