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
package com.itman.datastream.common.api;


import com.itman.datastream.common.constant.FileTypeEnum;
import com.itman.datastream.common.constant.MQTypeEnum;
import com.itman.datastream.common.errcode.DataStreamErrorCode;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSourceFactory {
    private final List<IDatabaseAdapter> dataBaseAdapter;
    private final List<ITableMetaApi> tableMetaApi;
    private final List<IFileApi> fileApi;
    private final List<IMQAdapterApi> mqAdapterApi;

    public IDatabaseAdapter matchDataBase(Integer dataSourceType) throws DataStreamException {
        int i = 0;
        return dataBaseAdapter.stream().filter(x -> x.chooseDS(dataSourceType)).findFirst().orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_MATCH_DATA_BASE_OBJECT_ERROR));
    }

    public ITableMetaApi matchTableMeta(Integer dataSourceType) throws DataStreamException {
        int i = 0;
        return tableMetaApi.stream().filter(x -> x.chooseDS(dataSourceType)).findFirst().orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_MATCH_DATA_BASE_OBJECT_ERROR));
    }

    public IFileApi matchFileFormat(Integer dataSourceType) throws DataStreamException {
        return fileApi.stream().filter(x -> x.chooseFile(FileTypeEnum.of(dataSourceType))).findFirst().orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_MATCH_DATA_BASE_OBJECT_ERROR));
    }

    public IMQAdapterApi matchMQ(Integer dataSourceType) throws DataStreamException {
        return mqAdapterApi.stream().filter(x -> x.chooseMQ(MQTypeEnum.of(dataSourceType))).findFirst().orElseThrow(() -> new DataStreamException(DataStreamErrorCode.PARAM_MATCH_DATA_BASE_OBJECT_ERROR));
    }
}
