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

import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

public interface ITableMetaApi {
    Boolean chooseDS(Integer dataSourceType);

    List<TableColumnEntity> getTableColumns(Integer dataSourceType, String schemaName, String userName, String tableName) throws DataStreamException;

    List<TableInfoEntity> getTableInfo(Integer dataSourceType, String schemaName) throws DataStreamException;

    TableInfoEntity fetchTableMetadata(String schemaName, String tableName) throws DataStreamException;
}
