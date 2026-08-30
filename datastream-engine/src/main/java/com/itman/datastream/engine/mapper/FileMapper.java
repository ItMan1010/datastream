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

import com.itman.datastream.common.entity.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

@Mapper
public interface FileMapper {
    FileBodyEntity selectFileBodyById(@Param("FileBodyEntityId") Long FileBodyEntityId) throws DataAccessException;

    List<FileBodyEntity> selectFileBodyByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileSpecialEntity> selectFileSpecialByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileFieldEntity> selectFileField(@Param("belongFlag") Integer belongFlag, @Param("belongId") Long belongId) throws DataAccessException;

    List<FileFieldEntity> selectFileFieldByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileFieldEntity> selectFileFieldById(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    FileFormatEntity selectFileFormat(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    List<FileFilterEntity> selectFileFilter(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer selectFileFormatCount(@Param("dbType") Integer dbType, @Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("systemUserCode") String systemUserCode);

    List<FileFormatEntity> selectFileFormatByPage(@Param("dbType") Integer dbType, @Param("queryFlag") Integer queryFlag, @Param("queryValue") String queryValue, @Param("sqlLimit") String sqlLimit, @Param("systemUserCode") String systemUserCode) throws DataAccessException;

    Integer deleteFileFormatById(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileSpecialByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileSpecialById(@Param("fileSpecialId") Long fileSpecialId) throws DataAccessException;

    Integer deleteFileBodyByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileFilterByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer deleteFileFieldByFileFormatId(@Param("fileFormatId") Long fileFormatId) throws DataAccessException;

    Integer insertFileFormat(@Param("fileFormat") FileFormatEntity fileFormat, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFile(@Param("fileFormat") FileFormatEntity fileFormat) throws DataAccessException;

    Integer insertFileBody(@Param("fileBody") FileBodyEntity fileBody, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFileBody(@Param("fileBody") FileBodyEntity fileBody) throws DataAccessException;

    Integer insertFileFieldList(@Param("fileFieldList") List<FileFieldEntity> fileFieldList, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer deleteFileField(@Param("FileFieldId") Long FileFieldId) throws DataAccessException;

    Integer updateFileField(@Param("fileField") FileFieldEntity fileField) throws DataAccessException;

    Integer insertFileSpecial(@Param("fileSpecial") FileSpecialEntity fileSpecial, @Param("sysdate") String sysdate) throws DataAccessException;

    Integer updateFileSpecial(@Param("fileSpecial") FileSpecialEntity fileSpecial) throws DataAccessException;

    Integer updateFileFormatOnLineFlagById(@Param("fileFormatId") Long fileFormatId, @Param("onLineFlag") Integer onLineFlag) throws DataAccessException;

}
