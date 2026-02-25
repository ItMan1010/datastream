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
package com.itman.datastream.security.handler;

import com.itman.datastream.security.annotation.LogOperate;
import org.springframework.stereotype.Component;

@Component
public class LogoutLogOperate {
    @LogOperate(operateType = 1, moduleName = "用户退出", username = "#username", description = "'帐号退出：'+#username")
    public String logoutUsername(String username) {
        return username;
    }
}
