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
package com.itman.datastream.connectors.file.impl;

import com.itman.datastream.common.constant.FileTypeEnum;
import com.itman.datastream.common.entity.FileFieldEntity;
import com.itman.datastream.common.entity.FileFormatEntity;
import com.itman.datastream.common.entity.FileLineDataEntity;
import com.itman.datastream.common.entity.FileSpecialEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.CommUtils;
import com.itman.datastream.connectors.file.AbstractFile;
import com.itman.datastream.common.api.IFileApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.FileTypeEnum.TEXT;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Component
public class TextFileApiImpl extends AbstractFile implements IFileApi {
    @Override
    public Boolean chooseFile(FileTypeEnum fileTypeEnum) {
        return fileTypeEnum.equals(TEXT);
    }

    private static final ConcurrentHashMap<String, BufferedReader> bufferedReaderStaticMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, BufferedWriter> bufferedWriterStaticMap = new ConcurrentHashMap<>();

    private static final String specialHeadLineFixContent = "                                                    \n";

    public Integer statFileLineCount(final String objectFileName, final String localPath) throws DataStreamException {
        Integer lineNumber = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(localPath + "/" + objectFileName))) {
            String lineContent = reader.readLine();
            while (!StringUtils.isEmpty(lineContent)) {
                lineNumber++;
                lineContent = reader.readLine();
            }
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException(OPER_PARSE_FILE_LINE_DATA_ERROR);
        }
        return lineNumber;
    }

    @Override
    public List<Map> parseFileLineData(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, final Integer sourcePropertiesSelectCount) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        Object taskLock = super.parseFileLineDataLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            return parseFileLineDataLock(taskId, objectFileName, fileFormat, sourcePropertiesSelectCount);
        }
    }

    public List<Map> parseFileLineDataLock(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, final Integer sourcePropertiesSelectCount) throws DataStreamException {
        List<Map> dataMapList = new ArrayList<>();
        BufferedReader bufferedReader = null;

        try {
            String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());

            if (bufferedReaderStaticMap.containsKey(stringKey)) {
                bufferedReader = bufferedReaderStaticMap.get(stringKey);
            } else {
                bufferedReader = new BufferedReader(new FileReader(fileFormat.getLocalPath() + "/" + objectFileName));
                bufferedReaderStaticMap.put(stringKey, bufferedReader);
                lineNumberStaticMap.put(stringKey, 0);
            }

            Integer lineNumber = lineNumberStaticMap.get(stringKey);
            String lineContent = null;
            while (true) {
                lineContent = bufferedReader.readLine();
                if (StringUtils.isEmpty(lineContent)) {
                    break;
                }

                lineNumber++;
                lineNumberStaticMap.put(stringKey, lineNumber);

                FileLineDataEntity fileLineData = super.matchFileLineDataByFormat(lineNumber, lineContent, fileFormat);
                dataMapList.add(fileLineData.getLineDataMap());
                if (sourcePropertiesSelectCount.equals(dataMapList.size())) {
                    break;
                }
            }
        } catch (DataStreamException dse) {
            log.error("dse={}", dse);
            throw new DataStreamException(dse.getErrCode(), dse.getErrMsg());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException(OPER_PARSE_FILE_LINE_DATA_ERROR);
        }

        return dataMapList;
    }

    @Override
    public void releaseFileResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        releaseBufferedReaderResource(taskId, fileFormatId);

        releaseBufferedWriterResource(taskId, fileFormatId);
    }


    @Override
    public void releaseFileResource(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        specialEndDataWriteIntoFile(taskId, objectFileName, fileFormat);

        releaseFileResource(taskId, fileFormat.getFileFormatId());

        finishFile(taskId, fileFormat.getLocalPath(), objectFileName);
    }

    private void releaseBufferedReaderResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);

        if (bufferedReaderStaticMap.containsKey(stringKey)) {
            try {
                if (bufferedReaderStaticMap.get(stringKey) != null) {
                    bufferedReaderStaticMap.get(stringKey).close();
                }
            } catch (IOException e) {
                log.error("e={}", e);
                throw new DataStreamException(OPER_RELEASE_BUFFERED_READER_RESOURCE_ERROR);
            }
            bufferedReaderStaticMap.remove(stringKey);
        }

        releaseReaderResource(taskId, fileFormatId);
    }

    /**
     * 文件格式数据稽核
     *
     * @param filePath
     * @param fileName
     * @param fileFormat
     * @throws DataStreamException
     */
    @Override
    public void checkFileLineData(final String filePath, final String fileName, final FileFormatEntity fileFormat) throws DataStreamException {
        Integer lineNumber = 0;
        Integer bodyLineSum = 0;
        List<FileLineDataEntity> fileLineSpecialDataList = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath + "/" + fileName))) {
            String lineContent = reader.readLine();
            while (!StringUtils.isEmpty(lineContent)) {
                lineNumber++;

                bodyLineSum = super.checkFileLineData(bodyLineSum, lineNumber, lineContent, fileLineSpecialDataList, fileFormat);

                lineContent = reader.readLine();
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DataStreamException dke) {
            log.error("dke={}", dke);
            throw new DataStreamException("doBusinessCheck", dke.getErrMsg());
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException("doBusinessCheck", "文件格式稽核失败:" + e.getMessage());
        }
    }

    @Override
    public void specialHeadDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return;
        }

        List<FileSpecialEntity> fileSpecialListTemp = fileFormat.getFileSpecialList().stream().filter(x -> !x.getFixLinePosition().equals(-1)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(fileSpecialListTemp)) {
            return;
        }

        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        String filePath = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;
        Object taskLock = bodyDataWriteIntoFileLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            List<String> dataObjectListTmp = new ArrayList<>();
            for (int size = 0; size < fileSpecialListTemp.size(); size++) {
                StringBuilder lineContent = new StringBuilder();
                //使用空格占位
                lineContent.append(specialHeadLineFixContent);
                dataObjectListTmp.add(lineContent.toString());
            }

            for (String iterator : dataObjectListTmp) {
                bufferedWriterMethodByAppend(taskId, fileFormat.getFileFormatId(), filePath, iterator);
            }
        }
    }

    @Override
    public void bodyDataWriteIntoFile(final Long taskId, final String objectFileName, List<List<String>> dataObjectList, Map<String, Long> dataSumFieldMap, final FileFormatEntity fileFormat) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        String filePath = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;
        Object taskLock = bodyDataWriteIntoFileLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            List<String> dataObjectListTmp = new ArrayList<>();
            String splitFlag = CommUtils.getSplitFlag(fileFormat.getFileBody().getSplitFlag());
            for (List<String> iterator : dataObjectList) {
                StringBuilder lineContent = new StringBuilder();
                lineContent.append(String.join(splitFlag, iterator));
                lineContent.append(splitFlag);
                lineContent.append("\n");
                dataObjectListTmp.add(lineContent.toString());
            }

            for (String iterator : dataObjectListTmp) {
                bufferedWriterMethodByAppend(taskId, fileFormat.getFileFormatId(), filePath, iterator);
            }

            mergeAndSumFieldValues(stringKey, dataObjectListTmp.size(), dataSumFieldMap);
        }
    }

    @Override
    public void flushResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);
        Object taskLock = bodyDataWriteIntoFileLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            try {
                if (bufferedWriterStaticMap.containsKey(stringKey)) {
                    BufferedWriter bufferedWriter = bufferedWriterStaticMap.get(stringKey);
                    bufferedWriter.flush();
                }
            } catch (Exception e) {
                log.error("e={}", e);
                throw new DataStreamException(OPER_BUFFERED_WRITER_METHOD_BY_APPEND_ERROR);
            }
        }
    }

    private void bufferedWriterMethodByAppend(final Long taskId, Long fileFormatId, String filepath, String content) throws DataStreamException {
        BufferedWriter bufferedWriter;
        try {
            String stringKey = genStringKey(taskId, fileFormatId);
            if (bufferedWriterStaticMap.containsKey(stringKey)) {
                bufferedWriter = bufferedWriterStaticMap.get(stringKey);
            } else {

                // 使用 OutputStreamWriter 指定编码，并设置为追加模式
                FileOutputStream fileOutputStream = new FileOutputStream(filepath, true); // true 表示追加模式
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8"); // 如 "UTF-8"

                bufferedWriter = new BufferedWriter(outputStreamWriter);
                bufferedWriterStaticMap.put(stringKey, bufferedWriter);
            }

            bufferedWriter.write(content);
            //ToDo 如果每一笔都要直接入文件，就在这里flush
//            bufferedWriter.flush();
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException(OPER_BUFFERED_WRITER_METHOD_BY_APPEND_ERROR);
        }
    }

    private void releaseBufferedWriterResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);
        if (bufferedWriterStaticMap.containsKey(stringKey)) {
            try {
                if (bufferedWriterStaticMap.get(stringKey) != null) {
                    bufferedWriterStaticMap.get(stringKey).close();
                }
            } catch (IOException e) {
                log.error("e={}", e);
                throw new DataStreamException(OPER_RELEASE_BUFFERED_WRITER_RESOURCE_ERROR);
            }
            bufferedWriterStaticMap.remove(stringKey);
        }

        releaseWriterResource(taskId, fileFormatId);
    }

    @Override
    public void specialEndDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return;
        }

        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        String filePathName = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;
        Map<String, Long> dataFieldSumMap = dataWriteIntoFileLineFieldSumStaticMap.get(stringKey);

        List<FileSpecialEntity> fileSpecialEndList = fileFormat.getFileSpecialList().stream().filter(x -> x.getFixLinePosition().equals(-1)).collect(Collectors.toList());
        for (FileSpecialEntity iterator : fileSpecialEndList) {
            List<String> dataObjectList = new ArrayList<>();

            for (FileFieldEntity iterator2 : iterator.getFileFieldList()) {
                if (iterator2.getSumLineFlag() != null && iterator2.getSumLineFlag().equals(1) && dataWriteIntoFileLineSumStaticMap.containsKey(stringKey)) {
                    dataObjectList.add(dataWriteIntoFileLineSumStaticMap.get(stringKey).toString());
                } else if (!StringUtils.isEmpty(iterator2.getSumFieldName()) && dataFieldSumMap != null && dataFieldSumMap.containsKey(iterator2.getSumFieldName())) {
                    dataObjectList.add(dataFieldSumMap.get(iterator2.getSumFieldName()).toString());
                }
            }

            StringBuilder lineContent = new StringBuilder();
            String splitFlag = CommUtils.getSplitFlag(iterator.getSplitFlag());
            lineContent.append(String.join(splitFlag, dataObjectList));
            lineContent.append(splitFlag);
            lineContent.append("\n");
            bufferedWriterMethodByAppend(taskId, fileFormat.getFileFormatId(), filePathName, lineContent.toString());
        }

        //强制同步写入文件
        flushResource(taskId, fileFormat.getFileFormatId());

        List<FileSpecialEntity> fileSpecialHeadList = fileFormat.getFileSpecialList().stream().filter(x -> !x.getFixLinePosition().equals(-1)).collect(Collectors.toList());
        for (FileSpecialEntity iterator : fileSpecialHeadList) {
            List<String> dataObjectList = new ArrayList<>();

            for (FileFieldEntity iterator2 : iterator.getFileFieldList()) {
                if (iterator2.getSumLineFlag() != null && iterator2.getSumLineFlag().equals(1) && dataWriteIntoFileLineSumStaticMap.containsKey(stringKey)) {
                    dataObjectList.add(dataWriteIntoFileLineSumStaticMap.get(stringKey).toString());
                } else if (!StringUtils.isEmpty(iterator2.getSumFieldName()) && dataFieldSumMap != null && dataFieldSumMap.containsKey(iterator2.getSumFieldName())) {
                    dataObjectList.add(dataFieldSumMap.get(iterator2.getSumFieldName()).toString());
                }
            }

            StringBuilder lineContent = new StringBuilder();
            String splitFlag = CommUtils.getSplitFlag(iterator.getSplitFlag());
            lineContent.append(String.join(splitFlag, dataObjectList));
            lineContent.append(splitFlag);
            overwriteAtPosition(filePathName, iterator.getFixLinePosition(), lineContent.toString());
        }
    }

    private void overwriteAtPosition(final String filePathName, Integer fixLinePosition, String content) {
        try (RandomAccessFile raf = new RandomAccessFile(filePathName, "rw")) {
            int positionToOverwrite = specialHeadLineFixContent.getBytes("UTF-8").length * (fixLinePosition - 1);

            // 1. 定位指针
            raf.seek(positionToOverwrite);
            // 2. 执行覆盖写入
            raf.write(content.getBytes()); // 或 raf.writeBytes(newText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
