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
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.engine.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.DAO_QUERY_TABLE_LINK_COUNT_ERROR;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FileDao {
    public final FileMapper fileMapper;
    private final DataStreamConfig dataStreamConfig;


    public FileBodyEntity selectFileBodyById(Long FileBodyEntityId) {
        return fileMapper.selectFileBodyById(FileBodyEntityId);
    }


    public List<FileBodyEntity> selectFileBodyByFileFormatId(Long fileFormatId) {
        return fileMapper.selectFileBodyByFileFormatId(fileFormatId);
    }

    public List<FileSpecialEntity> selectFileSpecialByFileFormatId(Long fileFormatId) {
        return fileMapper.selectFileSpecialByFileFormatId(fileFormatId);
    }


    public List<FileFieldEntity> selectFileField(Integer belongFlag, Long belongId) {
        return fileMapper.selectFileField(belongFlag, belongId);
    }


    public List<FileFieldEntity> selectFileFieldByFileFormatId(Long fileFormatId) {
        return fileMapper.selectFileFieldByFileFormatId(fileFormatId);
    }


    public List<FileFieldEntity> selectFileFieldById(Long fileFormatId) {
        return fileMapper.selectFileFieldById(fileFormatId);
    }


    public FileFormatEntity selectFileFormat(Long fileFormatId) {
        return fileMapper.selectFileFormat(fileFormatId);
    }


    public List<FileFilterEntity> selectFileFilter(Long fileFormatId) {
        return fileMapper.selectFileFilter(fileFormatId);
    }

    public Integer selectFileFormatCount(Integer queryFlag, String queryValue) {
        return fileMapper.selectFileFormatCount(dataStreamConfig.getMetaTeledbType(), queryFlag, queryValue);
    }

    public List<FileFormatEntity> selectFileFormatByPage(Integer queryFlag, String queryValue, String sqlLimit) {
        return fileMapper.selectFileFormatByPage(dataStreamConfig.getMetaTeledbType(), queryFlag, queryValue, sqlLimit);
    }


    public Integer deleteFileFormatById(Long fileFormatId) {
        return fileMapper.deleteFileFormatById(fileFormatId);
    }


    public Integer deleteFileSpecialByFileFormatId(Long fileFormatId) {
        return fileMapper.deleteFileSpecialByFileFormatId(fileFormatId);
    }

    public Integer deleteFileSpecialById(Long fileSpecialId) {
        return fileMapper.deleteFileSpecialById(fileSpecialId);
    }


    public Integer deleteFileBodyByFileFormatId(Long fileFormatId) {
        return fileMapper.deleteFileBodyByFileFormatId(fileFormatId);
    }


    public Integer deleteFileFilterByFileFormatId(Long fileFormatId) {
        return fileMapper.deleteFileFilterByFileFormatId(fileFormatId);
    }


    public Integer deleteFileFieldByFileFormatId(Long fileFormatId) {
        return fileMapper.deleteFileFieldByFileFormatId(fileFormatId);
    }


    public Integer insertFileFormat(FileFormatEntity fileFormat, String sysdate) {
        return fileMapper.insertFileFormat(fileFormat, sysdate);
    }


    public Integer updateFile(FileFormatEntity fileFormat) {
        return fileMapper.updateFile(fileFormat);
    }


    public Integer insertFileBody(FileBodyEntity FileBodyEntity, String sysdate) {
        return fileMapper.insertFileBody(FileBodyEntity, sysdate);
    }


    public Integer updateFileBody(FileBodyEntity FileBodyEntity) {
        return fileMapper.updateFileBody(FileBodyEntity);
    }


    public Integer insertFileFieldList(List<FileFieldEntity> FileFieldEntityList, String sysdate) {
        return fileMapper.insertFileFieldList(FileFieldEntityList, sysdate);
    }


    public Integer deleteFileField(Long FileFieldEntityId) {
        return fileMapper.deleteFileField(FileFieldEntityId);
    }


    public Integer updateFileField(FileFieldEntity FileFieldEntity) {
        return fileMapper.updateFileField(FileFieldEntity);
    }


    public Integer insertFileSpecial(FileSpecialEntity fileSpecial, String sysdate) {
        return fileMapper.insertFileSpecial(fileSpecial, sysdate);
    }


    public Integer updateFileSpecial(FileSpecialEntity fileSpecial) {
        return fileMapper.updateFileSpecial(fileSpecial);
    }


    public Integer updateFileFormatOnLineFlagById(Long fileFormatId, Integer onLineFlag) {
        return fileMapper.updateFileFormatOnLineFlagById(fileFormatId, onLineFlag);
    }
}
