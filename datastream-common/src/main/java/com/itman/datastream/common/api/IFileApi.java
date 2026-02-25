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
import com.itman.datastream.common.entity.FileFormatEntity;
import com.itman.datastream.common.errcode.DataStreamException;

import java.util.List;
import java.util.Map;

public interface IFileApi {
    Boolean chooseFile(FileTypeEnum fileTypeEnum);

    List<Map> parseFileLineData(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, final Integer sourcePropertiesSelectCount) throws DataStreamException;

    /**
     * 统计数据行记录数
     *
     * @param objectFileName
     * @param localPath
     * @return
     * @throws DataStreamException
     */
    Integer statFileLineCount(final String objectFileName, final String localPath) throws DataStreamException;

    void releaseFileResource(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException;
    void releaseFileResource(final Long taskId, final Long fileFormatId) throws DataStreamException;

    void checkFileLineData(final String filePath, final String fileName, final FileFormatEntity fileFormat) throws DataStreamException;

    /**
     * 文件特殊行在行头占位，如果行头多个特殊行必须连续
     * @param taskId
     * @param objectFileName
     * @param fileFormat
     * @throws DataStreamException
     */
    void specialHeadDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException;

    void bodyDataWriteIntoFile(final Long taskId, final String objectFileName, List<List<String>> dataObjectList, Map<String, Long> dataSumFieldMap, final FileFormatEntity fileFormat) throws DataStreamException;

    void specialEndDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException;

    void flushResource(final Long taskId, final Long fileFormatId) throws DataStreamException;

    void finishFile(final Long taskId, String localPath, final String objectFileName) throws DataStreamException;
}
