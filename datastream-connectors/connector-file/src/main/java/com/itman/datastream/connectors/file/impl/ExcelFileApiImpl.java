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

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.write.handler.RowWriteHandler;
import com.itman.datastream.common.constant.FileTypeEnum;
import com.itman.datastream.common.entity.FileFieldEntity;
import com.itman.datastream.common.entity.FileFormatEntity;
import com.itman.datastream.common.entity.FileLineDataEntity;
import com.itman.datastream.common.entity.FileSpecialEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.connectors.file.AbstractFile;
import com.itman.datastream.common.api.IFileApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.FileTypeEnum.EXCEL;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;

@Slf4j
@Component
public class ExcelFileApiImpl extends AbstractFile implements IFileApi {
    @Override
    public Boolean chooseFile(FileTypeEnum fileTypeEnum) {
        return fileTypeEnum.equals(EXCEL);
    }

    private static final ConcurrentHashMap<String, Workbook> workbookReaderStaticMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String ,List<List<String>>> cacheDataStaticMap = new ConcurrentHashMap<>();


    public Integer statFileLineCount(final String objectFileName, final String localPath) throws DataStreamException {
        Integer lineNumber = 0;
        try (InputStream inputStream = new FileInputStream(localPath + "/" + objectFileName); Workbook workbook = new XSSFWorkbook(inputStream)) {
            lineNumber = workbook.getSheetAt(0).getPhysicalNumberOfRows();
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException(OPER_PARSE_FILE_LINE_DATA_ERROR);
        }
        return lineNumber;
    }

    @Override
    public void checkFileLineData(final String filePath, final String fileName, final FileFormatEntity fileFormat) throws DataStreamException {
        String pathFile = filePath + "/" + fileName;
        String fileNameSuffixes = pathFile.substring(pathFile.lastIndexOf(".") + 1);
        if (fileNameSuffixes.equals("xls")) {
            checkXlsFileLineData(filePath, fileName, fileFormat);
        } else if (fileNameSuffixes.equals("xlsx")) {
            checkXlsxFileLineData(filePath, fileName, fileFormat);
        }
    }

    public void checkXlsFileLineData(final String filePath, final String fileName, final FileFormatEntity fileFormat) throws DataStreamException {
        Integer bodyLineSum = 0;
        List<FileLineDataEntity> fileLineSpecialDataList = new ArrayList<>();
        String pathFile = filePath + "/" + fileName;

        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new HSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                bodyLineSum = super.checkFileLineData(bodyLineSum, i + 1, changRowToString(sheet.getRow(i)), fileLineSpecialDataList, fileFormat);
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DataStreamException e) {
            log.error("e={}", e);
            throw new DataStreamException("xxx", "yyy");
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException("xxx", "yyy");
        }
    }

    public void checkXlsxFileLineData(final String filePath, final String fileName, final FileFormatEntity fileFormat) throws DataStreamException {
        Integer bodyLineSum = 0;
        List<FileLineDataEntity> fileLineSpecialDataList = new ArrayList<>();
        String pathFile = filePath + "/" + fileName;

        try (InputStream inputStream = new FileInputStream(pathFile); Workbook workbook = new XSSFWorkbook(inputStream)) {
            for (int i = 0; i < workbook.getSheetAt(0).getPhysicalNumberOfRows(); i++) {
                bodyLineSum = super.checkFileLineData(bodyLineSum, i + 1, changRowToString(workbook.getSheetAt(0).getRow(i)), fileLineSpecialDataList, fileFormat);
            }

            if (!CollectionUtils.isEmpty(fileLineSpecialDataList)) {
                //校验文件中文件头尾送的正文行数和实际计算总行数稽核
                super.checkFileLineSpecialData(bodyLineSum, fileLineSpecialDataList, fileFormat);
            }
        } catch (DataStreamException e) {
            log.error("e={}", e);
            throw new DataStreamException("xxx", "yyy");
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException("xxx", "yyy");
        }
    }

    private String changRowToString(Row row) {
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < row.getPhysicalNumberOfCells(); index++) {
            Cell cell = row.getCell(index);
            switch (cell.getCellType()) {
                case STRING:
                    line.append(cell.getStringCellValue());
                    break;
                case NUMERIC:
                    line.append(cell.getNumericCellValue());
                    break;
                case BOOLEAN:
                    line.append(cell.getBooleanCellValue());
                    break;
                default:
            }
            line.append("|");
        }
        return line.toString();
    }

    @Override
    public void specialHeadDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        if(CollectionUtils.isEmpty(fileFormat.getFileSpecialList())){
            return;
        }

        List<FileSpecialEntity> fileSpecialListTemp = fileFormat.getFileSpecialList().stream().filter(x->!x.getFixLinePosition().equals(-1)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(fileSpecialListTemp)){
            return;
        }

        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        String filePath = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;
        Object taskLock = bodyDataWriteIntoFileLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            List<String> dataObjectList = new ArrayList<>();
            dataObjectList.add("***");
            List<List<String>> dataObjectListTmp = new ArrayList<>();
            dataObjectListTmp.add(dataObjectList);
            //bufferedWriterMethodByAppend(linkTaskId, objectFileName, fileFormat, dataObjectListTmp);
            writeCacheData(stringKey, dataObjectListTmp);
        }
    }

    private void writeCacheData(String stringKey, List<List<String>> dataObjectList){
        if(cacheDataStaticMap.containsKey(stringKey)){
            cacheDataStaticMap.get(stringKey).addAll(dataObjectList);
        }else{
            cacheDataStaticMap.put(stringKey, dataObjectList);
        }
    }

    @Override
    public void bodyDataWriteIntoFile(final Long taskId, final String objectFileName, List<List<String>> dataObjectList, Map<String, Long> dataSumFieldMap, final FileFormatEntity fileFormat) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());

        String filePath = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;
        Object taskLock = bodyDataWriteIntoFileLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
//            try {
//                EasyExcelFactory.write(filePath, null).sheet("sheet").doWrite(dataObjectList);
//            } catch (Exception e) {
//                log.error("e={}", e);
//                throw new DataStreamException(OPER_BUFFERED_WRITER_METHOD_BY_APPEND_ERROR);
//            }

            writeCacheData(stringKey, dataObjectList);

            mergeAndSumFieldValues(stringKey, dataObjectList.size(), dataSumFieldMap);
        }
    }

    public void flushResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        //todo
    }

    @Override
    public List<Map> parseFileLineData(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, final Integer sourcePropertiesSelectCount) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        Object taskLock = parseFileLineDataLockStaticMap.computeIfAbsent(stringKey, k -> new Object());
        synchronized (taskLock) {
            return parseFileLineDataLock(taskId, objectFileName, fileFormat, sourcePropertiesSelectCount);
        }
    }


    public List<Map> parseFileLineDataLock(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, final Integer sourcePropertiesSelectCount) throws DataStreamException {
        List<Map> dataMapList = new ArrayList<>();

        try {
            String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
            String pathFile = fileFormat.getLocalPath() + "/" + objectFileName;
            String fileNameSuffixes = pathFile.substring(pathFile.lastIndexOf(".") + 1);
            Workbook workbook = null;
            if (workbookReaderStaticMap.containsKey(stringKey)) {
                workbook = workbookReaderStaticMap.get(stringKey);
            } else {
                if (fileNameSuffixes.equals("xls")) {
                    workbook = new HSSFWorkbook(new FileInputStream(pathFile));
                } else if (fileNameSuffixes.equals("xlsx")) {
                    workbook = new XSSFWorkbook(new FileInputStream(pathFile));
                }
                workbookReaderStaticMap.put(stringKey, workbook);
                lineNumberStaticMap.put(stringKey, 0);
            }

            Sheet sheet = workbook.getSheetAt(0);
            Integer maxlineNumber = sheet.getPhysicalNumberOfRows();

            Integer lineNumber = lineNumberStaticMap.get(stringKey);
            String lineContent = null;
            while (true) {
                if(lineNumber >= maxlineNumber){
                    break;
                }
                lineContent = changRowToString(sheet.getRow(lineNumber));
                if (StringUtils.isEmpty(lineContent)) {
                    break;
                }

                lineNumber++;

                FileLineDataEntity fileLineData = super.matchFileLineDataByFormat(lineNumber, lineContent, fileFormat);
                dataMapList.add(fileLineData.getLineDataMap());
                if (sourcePropertiesSelectCount.equals(dataMapList.size())) {
                    break;
                }
            }
            lineNumberStaticMap.put(stringKey, lineNumber);
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

    private void releaseBufferedReaderResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);

        if (workbookReaderStaticMap.containsKey(stringKey)) {
            try {
                if (workbookReaderStaticMap.get(stringKey) != null) {
                    workbookReaderStaticMap.get(stringKey).close();
                }
            } catch (IOException e) {
                log.error("e={}", e);
                throw new DataStreamException(OPER_RELEASE_BUFFERED_READER_RESOURCE_ERROR);
            }
            workbookReaderStaticMap.remove(stringKey);
        }

        releaseReaderResource(taskId, fileFormatId);
    }

    private void releaseBufferedWriterResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        releaseWriterResource(taskId, fileFormatId);
    }

    @Override
    public void specialEndDataWriteIntoFile(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return;
        }

        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
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

            List<List<String>> dataObjectListTmp = new ArrayList<>();
            dataObjectListTmp.add(dataObjectList);
//            bufferedWriterMethodByAppend(linkTaskId, objectFileName, fileFormat, dataObjectListTmp);
            writeCacheData(stringKey, dataObjectListTmp);
        }

        //强制同步写入文件
//        flushResource(linkTaskId, fileFormat.getFileFormatId());

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

            overwriteAtPosition(taskId, objectFileName, fileFormat, iterator.getFixLinePosition(), dataObjectList);
        }
    }

    @Override
    public void releaseFileResource(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat) throws DataStreamException {
        specialEndDataWriteIntoFile(taskId, objectFileName, fileFormat);

        releaseFileResource(taskId, fileFormat.getFileFormatId());

        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        if (cacheDataStaticMap.containsKey(stringKey)) {
            //先批量写入文件
            bufferedWriterMethodByAppend(taskId, objectFileName, fileFormat, cacheDataStaticMap.get(stringKey));

            cacheDataStaticMap.remove(stringKey);
        }

        finishFile(taskId, fileFormat.getLocalPath(), objectFileName);
    }

    private void bufferedWriterMethodByAppend(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, List<List<String>> dataObjectList) throws DataStreamException {
        try {
            String filePath = fileFormat.getLocalPath() + genFilePrefix(taskId) + objectFileName;

            EasyExcelFactory.write(filePath, null).sheet("sheet").doWrite(dataObjectList);
        } catch (Exception e) {
            log.error("e={}", e);
            throw new DataStreamException(OPER_BUFFERED_WRITER_METHOD_BY_APPEND_ERROR);
        }
    }
    private void overwriteAtPosition(final Long taskId, final String objectFileName, final FileFormatEntity fileFormat, Integer fixLinePosition, List<String> dataObjectList) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormat.getFileFormatId());
        if(cacheDataStaticMap.containsKey(stringKey)){
            cacheDataStaticMap.get(stringKey).set((fixLinePosition-1), dataObjectList);
        }
    }
}
