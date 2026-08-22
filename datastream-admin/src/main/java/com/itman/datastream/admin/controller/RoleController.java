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

import com.itman.datastream.admin.controller.domain.request.AddRoleRequest;
import com.itman.datastream.admin.controller.domain.request.AssignRolePermissionsRequest;
import com.itman.datastream.admin.controller.domain.request.ModifyRoleRequest;
import com.itman.datastream.admin.controller.domain.request.QueryRoleRowsRequest;
import com.itman.datastream.admin.controller.domain.request.RoleIdRequest;
import com.itman.datastream.admin.controller.domain.response.AddRoleResponse;
import com.itman.datastream.admin.controller.domain.response.AssignRolePermissionsResponse;
import com.itman.datastream.admin.controller.domain.response.DelRoleResponse;
import com.itman.datastream.admin.controller.domain.response.ModifyRoleResponse;
import com.itman.datastream.admin.controller.domain.response.QueryAllRoleResponse;
import com.itman.datastream.admin.controller.domain.response.QueryRoleRowsResponse;
import com.itman.datastream.admin.controller.domain.response.RoleInfoResponse;
import com.itman.datastream.admin.service.IRoleService;
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
@RequestMapping("/api/role")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class RoleController {

    private final IRoleService roleService;

    @PostMapping(path = "/queryRoleRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryRoleRowsResponse> queryRoleRows(@RequestBody QueryRoleRowsRequest request) {
        QueryRoleRowsResponse response = new QueryRoleRowsResponse();
        try {
            Integer recordCount = roleService.getRoleCount(request.getQueryFlag(), request.getQueryValue());
            response.setTotal(recordCount);
            if (recordCount > 0) {
                response.setRoleList(roleService.queryRoleByPage(request.getQueryFlag(), request.getQueryValue(), request.getPage(), request.getCount()));
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

    @PostMapping(path = "/queryAllRole", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryAllRoleResponse> queryAllRole() {
        QueryAllRoleResponse response = new QueryAllRoleResponse();
        try {
            response.setRoleList(roleService.queryAllRole());
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

    @PostMapping(path = "/queryRoleInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<RoleInfoResponse> queryRoleInfo(@RequestBody RoleIdRequest request) {
        RoleInfoResponse response = new RoleInfoResponse();
        try {
            response.setRole(roleService.getRoleById(request.getRoleId()));
            response.setPermissionIds(roleService.getPermissionIdsByRoleId(request.getRoleId()));
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

    @LogOperate(operateType = 2, moduleName = "新增角色", description = "'roleCode:'+#request.role.roleCode")
    @PostMapping(path = "/addRole", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddRoleResponse> addRole(@RequestBody AddRoleRequest request) {
        AddRoleResponse response = new AddRoleResponse();
        try {
            Long roleId = roleService.addRole(request.getRole());
            response.setRoleId(roleId);
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

    @LogOperate(operateType = 2, moduleName = "修改角色", description = "'roleId:'+#request.role.roleId")
    @PostMapping(path = "/modifyRole", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyRoleResponse> modifyRole(@RequestBody ModifyRoleRequest request) {
        ModifyRoleResponse response = new ModifyRoleResponse();
        try {
            roleService.modifyRole(request.getRole());
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

    @LogOperate(operateType = 2, moduleName = "删除角色", description = "'roleId:'+#request.roleId")
    @PostMapping(path = "/delRole", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<DelRoleResponse> delRole(@RequestBody RoleIdRequest request) {
        DelRoleResponse response = new DelRoleResponse();
        try {
            roleService.delRole(request.getRoleId());
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

    @LogOperate(operateType = 2, moduleName = "角色授权", description = "'roleId:'+#request.roleId")
    @PostMapping(path = "/assignRolePermissions", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AssignRolePermissionsResponse> assignRolePermissions(@RequestBody AssignRolePermissionsRequest request) {
        AssignRolePermissionsResponse response = new AssignRolePermissionsResponse();
        try {
            roleService.assignRolePermissions(request.getRoleId(), request.getPermissionIds());
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