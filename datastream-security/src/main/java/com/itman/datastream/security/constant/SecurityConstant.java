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
package com.itman.datastream.security.constant;

public class SecurityConstant {
    private SecurityConstant() {
    }

    public static final String SUCCESS_CODE = "0";
    public static final String NULL_VALUE = "null";
    public static final String TOKEN_PARAMS_NAME = "token";
    public static final String ANONYMOUS_USERNAME = "task-manage-anonymous-user-98124";
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer";
    public static final String SECRET = "jwt818";
    public static final String ISS = "xxxx";
    public static final String ROLE_CLAIMS = "rol";
    public static final long EXPIRATION = 1800L;
    public static final String PASSWORD_SALT = "task-manage-3826";
    public static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";
    public static final String ROLE_PREFIX = "ROLE_";
    public static final String PERM_PREFIX = "PERM_";
}
