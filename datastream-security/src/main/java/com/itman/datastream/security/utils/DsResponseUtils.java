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
package com.itman.datastream.security.utils;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class DsResponseUtils {
    private DsResponseUtils() {
    }

    public static void write(String resultCode, String resultMsg, HttpServletResponse response) throws IOException {
        write(resultCode, resultMsg, null, response);
    }

    public static void write(String resultCode, String resultMsg, Object data, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        JSONObject resObj = new JSONObject();
        resObj.put("resultCode", resultCode);
        resObj.put("resultMsg", resultMsg);
        if (null != data) {
            resObj.put("data", data);
        }
        out.write(resObj.toJSONString());
        out.flush();
    }
}
