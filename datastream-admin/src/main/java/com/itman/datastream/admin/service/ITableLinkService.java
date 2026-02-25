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

import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableColumnEntity;
import com.itman.datastream.common.entity.TableLinkEntity;
import com.itman.datastream.common.entity.LinkNodeEntity;
import com.itman.datastream.engine.route.RouteSource;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.SOURCE_DATA_TEST_FLOW_KEY_NAME;


public interface ITableLinkService {
    Integer queryTableLinkCount(Integer queryFlag, String queryValue) throws DataStreamException;

    List<TableLinkEntity> queryTableLink(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException;

    Long addTableLink(String tableLinkName, String tableLinkDes, LinkNodeEntity linkNode) throws DataStreamException;

    Long modifyTableLink(Long tableLinkId, String tableLinkName, String tableLinkDes, LinkNodeEntity linkNode) throws DataStreamException;

    Long delTableLink(Long tableLinkId) throws DataStreamException;

    Long onOffTableLink(Long tableLinkId, Integer state) throws DataStreamException;

    @RouteSource(SOURCE_DATA_TEST_FLOW_KEY_NAME)
    List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException;

    LinkNodeEntity queryTableLink(Long tableLinkId) throws DataStreamException;

    List<LinkNodeEntity> queryTableLinkNodeList(Long tableLinkId) throws DataStreamException;
}