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
package com.itman.datastream.engine.mapper;

import com.itman.datastream.common.entity.TableLinkEntity;
import com.itman.datastream.common.entity.LinkNodeEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;


@Mapper
public interface TableLinkMapper {
    Integer queryTableLinkCount(@Param("dbType") Integer dbType, @Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("queryValue2") Long queryValue2);

    List<TableLinkEntity> queryTableLink(@Param("dbType") Integer dbType, @Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("queryValue2") Long queryValue2, @Param("sqlLimit") String sqlLimit);

    List<TableLinkEntity> queryTableLinkLikeOracle(@Param("pageBeginRow") Integer pageBeginRow, @Param("pageEndRow") Integer pageEndRow, @Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("queryValue2") Long queryValue2);

    Integer insertTableLink(@Param("sysdate") String sysdate, @Param("tableLink") TableLinkEntity tableLink) throws DataAccessException;

    Integer insertLinkNode(@Param("sysdate") String sysdate, @Param("linkNode") LinkNodeEntity linkNode) throws DataAccessException;

    Integer updateTableLinkInfo(@Param("sysdate") String sysdate, @Param("tableLinkId") Long tableLinkId, @Param("linkName") String linkName, @Param("linkDes") String linkDes) throws DataAccessException;

    Integer deleteTableLinkNode(@Param("tableLinkId") Long tableLinkId) throws DataAccessException;

    Integer updateTableLinkState(@Param("sysdate") String sysdate, @Param("tableLinkId") Long tableLinkId, @Param("state") Integer state) throws DataAccessException;

    List<LinkNodeEntity> queryTableLinkNode(@Param("dbType") Integer dbType, @Param("tableLinkId") Long tableLinkId);
}
