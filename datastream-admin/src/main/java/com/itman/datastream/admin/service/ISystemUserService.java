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
package com.itman.datastream.admin.service;

import com.itman.datastream.common.entity.SystemUserEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

/**
 * 系统用户管理服务。
 */
public interface ISystemUserService {

    Integer getUserCount(Integer queryFlag, String queryValue) throws DataStreamException;

    List<SystemUserEntity> queryUserByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException;

    SystemUserEntity getUserById(Long systemUserId) throws DataStreamException;

    List<Long> getRoleIdsByUserId(Long systemUserId) throws DataStreamException;

    Long addUser(SystemUserEntity user, List<Long> roleIds) throws DataStreamException;

    void modifyUser(SystemUserEntity user, List<Long> roleIds) throws DataStreamException;

    void updateUserState(Long systemUserId, Integer state) throws DataStreamException;

    void resetPassword(Long systemUserId, String password) throws DataStreamException;

    void delUser(Long systemUserId) throws DataStreamException;
}