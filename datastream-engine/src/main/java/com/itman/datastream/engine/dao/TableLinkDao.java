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
package com.itman.datastream.engine.dao;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.entity.TableLinkEntity;
import com.itman.datastream.common.entity.LinkNodeEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.mapper.TableLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID;
import static com.itman.datastream.common.constant.DataStreamConstant.TABLE_LINK_QUERY_FLAG_STATE;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TableLinkDao {
    public final TableLinkMapper tableLinkMapper;
    private final DataStreamConfig dataStreamConfig;


    public Integer queryTableLinkCount(Integer queryFlag, String queryValue, String systemUserCode) throws DataStreamException {
        try {
            return tableLinkMapper.queryTableLinkCount(dataStreamConfig.getMetaTeledbType(), queryFlag, queryValue, (queryFlag.equals(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID) || queryFlag.equals(TABLE_LINK_QUERY_FLAG_STATE)) ? Long.valueOf(queryValue) : null, systemUserCode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TABLE_LINK_COUNT_ERROR);
        }
    }

    public List<TableLinkEntity> queryTableLink(Integer queryFlag, String queryValue, String sqlLimit, String systemUserCode) throws DataStreamException {
        try {
            return tableLinkMapper.queryTableLink(dataStreamConfig.getMetaTeledbType(), queryFlag, queryValue, (queryFlag.equals(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID) || queryFlag.equals(TABLE_LINK_QUERY_FLAG_STATE)) ? Long.valueOf(queryValue) : null, sqlLimit, systemUserCode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TABLE_LINK_ERROR);
        }
    }

    public List<TableLinkEntity> queryTableLinkLikeOracle(Integer pageBeginRow, Integer pageEndRow, Integer queryFlag, String queryValue, String systemUserCode) throws DataStreamException {
        try {
            return tableLinkMapper.queryTableLinkLikeOracle(pageBeginRow, pageEndRow, queryFlag, queryValue, (queryFlag.equals(TABLE_LINK_QUERY_FLAG_TABLE_LINK_ID) || queryFlag.equals(TABLE_LINK_QUERY_FLAG_STATE)) ? Long.valueOf(queryValue) : null, systemUserCode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TABLE_LINK_ERROR);
        }
    }

    public Integer insertTableLink(String sysdate, TableLinkEntity tableLink) throws DataStreamException {
        try {
            return tableLinkMapper.insertTableLink(sysdate, tableLink);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_TABLE_LINK_ERROR);
        }
    }

    public Integer insertLinkNode(String sysdate, LinkNodeEntity linkNode) throws DataStreamException {
        try {
            return tableLinkMapper.insertLinkNode(sysdate, linkNode);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_INSERT_LINK_NODE_ERROR);
        }
    }

    public Integer updateTableLinkInfo(String sysdate, Long tableLinkId, String linkName, String linkDes) throws DataStreamException {
        try {
            return tableLinkMapper.updateTableLinkInfo(sysdate, tableLinkId, linkName, linkDes);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_TABLE_LINK_ERROR);
        }
    }

    public Integer deleteTableLinkNode(Long tableLinkId) throws DataStreamException {
        try {
            return tableLinkMapper.deleteTableLinkNode(tableLinkId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_DELETE_TABLE_LINK_NODE_ERROR);
        }
    }

    public Integer updateTableLinkState(String sysdate, Long tableLinkId, Integer state) throws DataStreamException {
        try {
            return tableLinkMapper.updateTableLinkState(sysdate, tableLinkId, state);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_UPDATE_TABLE_LINK_STATE_ERROR);
        }
    }

    public List<LinkNodeEntity> queryTableLinkNode(Long tableLinkId) throws DataStreamException {
        try {
            return tableLinkMapper.queryTableLinkNode(dataStreamConfig.getMetaTeledbType(), tableLinkId);
        } catch (Exception e) {
            log.error("error", e);
            throw new DataStreamException(DAO_QUERY_TABLE_LINK_ERROR);
        }
    }
}
