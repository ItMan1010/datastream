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
package com.itman.datastream.admin.handler;


import com.itman.datastream.admin.service.IMetaService;
import com.itman.datastream.admin.service.ITableLinkService;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.CommUtils;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.route.RegisterDataBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.parseSchemaNameJdbcUrl;

@Slf4j
@Component
@RequiredArgsConstructor
public class TableLinkHandler {
    private final DataStreamDao dataStreamDao;
    private final ITableLinkService tableLinkService;
    private final IMetaService metaService;
    private final RegisterDataBase registerDataBase;


    public void testTableLink(Long dataBaseId, LinkNodeEntity linkNode) throws DataStreamException {
        List<LinkNodeEntity> linkNodeList = new ArrayList<>();
        traverseTableLinkTreeDFS(false, -1L, linkNode, linkNodeList);

        DataBaseEntity dataBase = judgeDataBaseById(dataBaseId);
        dataBase.setDataPoolCount(1);
        dataBase.setKeyName(SOURCE_DATA_TEST_FLOW_KEY_NAME);
        dataBase.setSchemaName(parseSchemaNameJdbcUrl(dataBase.getUrl()));
        if (!dataBase.getDataBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) {
            dataBase.setUrl(CommUtils.appendUrlParam(dataBase.getUrl(), "socketTimeout", "5000"));
        }

        List<DataBaseEntity> dataBaseList = new ArrayList<>();
        dataBaseList.add(dataBase);
        registerDataBase.registerDataSources(dataBaseList, null);

        Map<Long, List<TableColumnEntity>> tableColumnBeanListMap = new HashMap<>();

        for (LinkNodeEntity iterator : linkNodeList) {
            //根据表名查询查询字段
            String tableName = iterator.getTableName();
            List<TableColumnEntity> tableColumnEntityList = tableLinkService.getTableColumns(dataBaseId, dataBase, tableName);
            if (CollectionUtils.isEmpty(tableColumnEntityList)) {
                String errorInfo = "表{" + tableName + "}在数据库不存在";
                throw new DataStreamException(OPER_QUERY_TABLE_FROM_DB_ERROR.getCode(), errorInfo);
            }

            boolean haveFlag = tableColumnEntityList.stream().anyMatch(column -> column.getColumnName().equalsIgnoreCase(iterator.getFieldName()));

            if (!haveFlag) {
                throw new DataStreamException(OPER_TABLE_FILED_NAME_EQUAL_FAIL_ERROR.getCode(), "表{" + iterator.getTableName() + "}匹配字段(" + iterator.getFieldName() + ")失败!");
            }

            tableColumnBeanListMap.put(iterator.getLinkNodeId(), tableColumnEntityList);
        }

        for (LinkNodeEntity iterator : linkNodeList) {
            if (!iterator.getParentLinkNodeId().equals(-1L)) {
                if (!tableColumnBeanListMap.containsKey(iterator.getParentLinkNodeId())) {
                    throw new DataStreamException(OPER_QUERY_PARENT_FLOW_NODE_ERROR);
                }

                boolean haveFlag = tableColumnBeanListMap.get(iterator.getParentLinkNodeId()).stream().anyMatch(column -> column.getColumnName().equalsIgnoreCase(iterator.getParentFieldName()));

                if (!haveFlag) {
                    throw new DataStreamException(OPER_TABLE_FILED_NAME_EQUAL_FAIL_ERROR.getCode(), "表(" + iterator.getTableName() + ")父字段(" + iterator.getParentFieldName() + ")匹配上级节点表失败!");
                }
            }
        }
    }


    private void traverseTableLinkTreeDFS(Boolean seqFlag, Long parentLinkNodeId, LinkNodeEntity linkNode, List<LinkNodeEntity> linkNodeList) throws DataStreamException {
        if (linkNode == null) return;

        linkNode.setLinkNodeId(seqFlag ? dataStreamDao.querySequence(SEQ_LINK_NODE_ID) : CommUtils.generateUniqueLong());
        linkNode.setParentLinkNodeId(parentLinkNodeId);
        linkNodeList.add(linkNode);

        if (!CollectionUtils.isEmpty(linkNode.getLinkNodeList())) {
            linkNode.getLinkNodeList().stream().filter(Objects::nonNull).forEach(x -> {
                try {
                    traverseTableLinkTreeDFS(seqFlag, linkNode.getLinkNodeId(), x, linkNodeList);
                } catch (DataStreamException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public DataBaseEntity judgeDataBaseById(Long dataBaseId) throws DataStreamException {
        List<DataBaseEntity> dataBaseList = metaService.queryDataBase(DATA_BASE_QUERY_FLAG_ID, dataBaseId, null, 1, 10, null);
        if (CollectionUtils.isEmpty(dataBaseList)) {
            throw new DataStreamException(OPER_DATA_SOURCE_NOT_EXISTS_ERROR);
        }
        return dataBaseList.get(0);
    }
}
