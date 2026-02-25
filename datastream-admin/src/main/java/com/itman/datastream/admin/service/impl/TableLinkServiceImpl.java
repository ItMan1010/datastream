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
package com.itman.datastream.admin.service.impl;

import com.itman.datastream.admin.service.ITableLinkService;
import com.itman.datastream.common.entity.DataBaseEntity;
import com.itman.datastream.common.entity.TableLinkEntity;
import com.itman.datastream.common.entity.LinkNodeEntity;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.CommUtils;
import com.itman.datastream.engine.dao.TableLinkDao;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.entity.TableColumnEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;


@Slf4j
@Service
@RequiredArgsConstructor
public class TableLinkServiceImpl implements ITableLinkService {
    private final DataStreamConfig dataStreamConfig;
    private final TableLinkDao tableLinkDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamDao dataStreamDao;

    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    public Integer queryTableLinkCount(Integer queryFlag, String queryValue) throws DataStreamException {
        queryValue = (queryFlag.equals(TABLE_LINK_QUERY_FLAG_LINK_NAME) || queryFlag.equals(TABLE_LINK_QUERY_FLAG_TABLE_NAME)) ? "%" + queryValue + "%" : queryValue;
        return tableLinkDao.queryTableLinkCount(queryFlag, queryValue);
    }

    @Override
    public List<TableLinkEntity> queryTableLink(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        queryValue = (queryFlag.equals(TABLE_LINK_QUERY_FLAG_LINK_NAME) || queryFlag.equals(TABLE_LINK_QUERY_FLAG_TABLE_NAME)) ? "%" + queryValue + "%" : queryValue;
        return (!dataStreamConfig.getMetaDbBaseType().equals(DATA_SOURCE_TYPE_ORACLE)) ?
                tableLinkDao.queryTableLink(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count)) :
                tableLinkDao.queryTableLinkLikeOracle(genPageRow(page, count), (genPageRow(page, count) + count), queryFlag, queryValue);
    }

    @Transactional(rollbackFor = DataStreamException.class)
    public Long addTableLink(String tableLinkName, String tableLinkDes, LinkNodeEntity linkNode) throws DataStreamException {
        TableLinkEntity tableLink = new TableLinkEntity();
        tableLink.setTableLinkId(dataStreamDao.querySequence(SEQ_TABLE_LINK_ID));
        tableLink.setTableLinkName(tableLinkName);
        tableLink.setTableLinkDes(tableLinkDes);
        tableLink.setState(COMMON_STATE_OFFLINE);
        if (tableLinkDao.insertTableLink(getDataBaseObject().makeSqlSystemDate(), tableLink).equals(0)) {
            throw new DataStreamException(OPER_INSERT_TABLE_LINK_ERROR);
        }

        //插入节点
        insertTableLinkNodes(tableLink.getTableLinkId(), linkNode);

        return tableLink.getTableLinkId();
    }

    private void traverseTableLinkTreeDFS(Boolean seqFlag, Long parentLinkNodeId, LinkNodeEntity linkNode, List<LinkNodeEntity> linkNodeList) throws DataStreamException {
        if (linkNode == null) return;

        linkNode.setLinkNodeId(seqFlag ? dataStreamDao.querySequence(SEQ_LINK_NODE_ID) : CommUtils.generateUniqueLong());
        linkNode.setParentLinkNodeId(parentLinkNodeId);
        linkNodeList.add(linkNode);

        if (!CollectionUtils.isEmpty(linkNode.getLinkNodeList())) {
            linkNode.getLinkNodeList().stream()
                    .filter(Objects::nonNull)
                    .forEach(x -> {
                        try {
                            traverseTableLinkTreeDFS(seqFlag, linkNode.getLinkNodeId(), x, linkNodeList);
                        } catch (DataStreamException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }


    @Transactional(rollbackFor = DataStreamException.class)
    public Long modifyTableLink(Long tableLinkId, String tableLinkName, String tableLinkDes, LinkNodeEntity linkNode) throws DataStreamException {
        checkFlowDefine(tableLinkId);

        if (tableLinkDao.updateTableLinkInfo(getDataBaseObject().makeSqlSystemDate(), tableLinkId, tableLinkName, tableLinkDes).equals(0)) {
            throw new DataStreamException(OPER_INSERT_TABLE_LINK_ERROR);
        }

        //删除节点
        tableLinkDao.deleteTableLinkNode(tableLinkId);

        //插入节点
        insertTableLinkNodes(tableLinkId, linkNode);

        return tableLinkId;
    }

    private void insertTableLinkNodes(Long tableLinkId, LinkNodeEntity linkNode) throws DataStreamException {
        if (linkNode != null) {
            List<LinkNodeEntity> linkNodeList = new ArrayList<>();
            traverseTableLinkTreeDFS(true, -1L, linkNode, linkNodeList);

            for (LinkNodeEntity iterator : linkNodeList) {
                iterator.setTableLinkId(tableLinkId);
                if (tableLinkDao.insertLinkNode(getDataBaseObject().makeSqlSystemDate(), iterator).equals(0)) {
                    throw new DataStreamException(OPER_INSERT_FLOW_NODE_ERROR);
                }
            }
        }
    }

    public Long delTableLink(Long tableLinkId) throws DataStreamException {
        checkFlowDefine(tableLinkId);

        if (tableLinkDao.updateTableLinkState(getDataBaseObject().makeSqlSystemDate(), tableLinkId, COMMON_STATE_DELETED).equals(0)) {
            throw new DataStreamException(OPER_UPDATE_FLOW_DEFINE_STATE_ERROR);
        }

        return tableLinkId;
    }

    private void checkFlowDefine(Long flowDefineId) throws DataStreamException {
        List<TableLinkEntity> canalTableLinkDefineEntityList = queryTableLink(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID, flowDefineId.toString(), 1, 10);
        if (CollectionUtils.isEmpty(canalTableLinkDefineEntityList)) {
            throw new DataStreamException(OPER_FLOW_DEFINE_NOT_EXISTS_ERROR);
        }

        if (!canalTableLinkDefineEntityList.get(0).getState().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_DATA_BASE_STATE_NOT_OFF_ERROR);
        }
    }


    public Long onOffTableLink(Long tableLinkId, Integer state) throws DataStreamException {
        List<TableLinkEntity> canalTableLinkDefineEntityList = queryTableLink(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID, tableLinkId.toString(), 1, 10);
        if (CollectionUtils.isEmpty(canalTableLinkDefineEntityList)) {
            throw new DataStreamException(OPER_FLOW_DEFINE_NOT_EXISTS_ERROR);
        }

        if (state.equals(COMMON_STATE_ONLINE) && !canalTableLinkDefineEntityList.get(0).getState().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_DATA_BASE_STATE_NOT_OFF_ERROR);
        } else if (state.equals(COMMON_STATE_OFFLINE) && !canalTableLinkDefineEntityList.get(0).getState().equals(COMMON_STATE_ONLINE)) {
            throw new DataStreamException(OPER_DATA_SOURCE_STATE_NOT_ON_ERROR);
        }

        if (tableLinkDao.updateTableLinkState(getDataBaseObject().makeSqlSystemDate(), tableLinkId, state).equals(0)) {
            throw new DataStreamException(OPER_UPDATE_FLOW_DEFINE_STATE_ERROR);
        }

        return tableLinkId;
    }

    public List<TableColumnEntity> getTableColumns(Long dataSourceId, DataBaseEntity dataSource, String tableName) throws DataStreamException {
        return dataSourceFactory.matchTableMeta(dataSource.getDataBaseType()).getTableColumns(dataSource.getDataBaseType(), dataSource.getSchemaName(), dataSource.getUserName(), tableName);

    }

    @Override
    public LinkNodeEntity queryTableLink(Long flowDefineId) throws DataStreamException {
        List<LinkNodeEntity> canalLinkNodeEntityList = tableLinkDao.queryTableLinkNode(flowDefineId);
        LinkNodeEntity canalFlowNodeRoot = null;
        if (!CollectionUtils.isEmpty(canalLinkNodeEntityList)) {
            Map<Long, LinkNodeEntity> canalFlowNodeMap = canalLinkNodeEntityList.stream()
                    .collect(Collectors.toMap(
                            LinkNodeEntity::getLinkNodeId,
                            node -> node
                    ));

            for (LinkNodeEntity iterator : canalLinkNodeEntityList) {
                if (iterator.getParentLinkNodeId().equals(-1L)) {
                    canalFlowNodeRoot = iterator;
                } else {
                    if (!canalFlowNodeMap.containsKey(iterator.getParentLinkNodeId())) {
                        throw new DataStreamException(OPER_FLOW_FIND_FLOW_NODE_ERROR);
                    }

                    if (canalFlowNodeMap.get(iterator.getParentLinkNodeId()).getLinkNodeList() == null) {
                        canalFlowNodeMap.get(iterator.getParentLinkNodeId()).setLinkNodeList(new ArrayList<>());
                    }
                    canalFlowNodeMap.get(iterator.getParentLinkNodeId()).getLinkNodeList().add(iterator);
                }
            }

        }
        return canalFlowNodeRoot;
    }

    @Override
    public List<LinkNodeEntity> queryTableLinkNodeList(Long flowDefineId) throws DataStreamException {
        return tableLinkDao.queryTableLinkNode(flowDefineId);
    }
}
