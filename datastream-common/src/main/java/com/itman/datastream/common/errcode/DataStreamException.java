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
package com.itman.datastream.common.errcode;


public class DataStreamException extends Exception {
    private final String errCode;
    private final String errMsg;

    public DataStreamException(String errCode, String errMsg) {
        super(errMsg);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public DataStreamException(String errCode, String errMsg, Throwable cause) {
        super(cause);
        this.errCode = errCode;
        this.errMsg = errMsg;
    }

    public DataStreamException(IErrorCode errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.errCode = errorCodeEnum.getCode();
        this.errMsg = errorCodeEnum.getMessage();
    }

    public DataStreamException(IErrorCode errorCodeEnum, Throwable cause) {
        super(cause);
        this.errCode = errorCodeEnum.getCode();
        this.errMsg = errorCodeEnum.getMessage();
    }

    public DataStreamException(IErrorCode errorCodeEnum, String... args) {
        super(errorCodeEnum.getMessage());
        this.errCode = errorCodeEnum.getCode();
        StringBuilder argsStringBuffer = new StringBuilder(",[");
        String[] var4 = args;
        int var5 = args.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            String arg = var4[var6];
            argsStringBuffer.append(arg == null ? "null" : arg);
        }

        argsStringBuffer.append("]");
        this.errMsg = errorCodeEnum.getMessage() + argsStringBuffer;
    }

    public String getErrCode() {
        return this.errCode;
    }

    public String getErrMsg() {
        return this.errMsg;
    }
}