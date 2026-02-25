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
package com.itman.datastream.connectors.file;

import com.google.common.base.Joiner;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.CommUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;


@Slf4j
@Component
public abstract class AbstractFile {
    protected static final ConcurrentHashMap<String, Integer> lineNumberStaticMap = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<String, Object> parseFileLineDataLockStaticMap = new ConcurrentHashMap<>();
    protected static final String FILE_TEMP_PREFIX = "/tmp_";
    protected static final ConcurrentHashMap<String, Object> bodyDataWriteIntoFileLockStaticMap = new ConcurrentHashMap<>();
    //记录数据写入数据体行数
    protected static final ConcurrentHashMap<String, Integer> dataWriteIntoFileLineSumStaticMap = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<String, Map<String, Long>> dataWriteIntoFileLineFieldSumStaticMap = new ConcurrentHashMap<>();


    protected String genStringKey(final Long taskId, final Long fileFormatId) {
        return taskId + "#" + fileFormatId;
    }


    protected String genFilePrefix(final Long taskId) {
        return FILE_TEMP_PREFIX + taskId + ".";
    }


    public void parseFileLineToMapCache(final FileTaskEntity fileTask, final Integer lineNumber, final String lineContent, final FileFormatEntity fileFormat) throws DataStreamException {
        //解析文件行变成字段map
        FileLineDataEntity fileLineData = matchFileLineDataByFormat(lineNumber, lineContent, fileFormat);
        if (fileLineData.getLineDataType().equals(FILE_LINE_BODY) && !CollectionUtils.isEmpty(fileLineData.getLineDataMap())) {
            //生成参与比较的key值
            List<String> mapKeyList = new ArrayList<>();
            for (FileTaskFieldRelaEntity iterator : fileTask.getFileTaskFieldRelaList()) {
                if (iterator.getCompareFlag() != null && iterator.getCompareFlag().equals(COMPARE_ABLE_FLAG_YES) && fileLineData.getLineDataMap().containsKey(iterator.getBObjectFieldId())) {
                    mapKeyList.add(fileLineData.getLineDataMap().get(iterator.getBObjectFieldId()));
                }
            }

            Optional.ofNullable(mapKeyList).orElseThrow(() -> new DataStreamException("parseFileLineToMapCache", "文件数据匹配比较key失败"));
//            ObjectQueue.putDataIntoMap(genMapKey(mapKeyList), fileLineData.getLineDataMap());
        }
    }

    private static String genMapKey(List<String> mapKeyList) {
        return MAP_KEY_PREFIX + Joiner.on(",").join(mapKeyList);
    }

    public void parseFileLineDataToQueue(final Integer lineNumber, final String lineContent, final FileTaskEntity task, final Map<Long, String> fieldIdNameMap) throws DataStreamException {
        //lineContent文件行数据要解析成标准数据格式,如果后续为了提高效率可以通过异步事件方式解析同步队列
        QueueData queueData = doTaskFieldRelaIdMapped(lineNumber, lineContent, task, fieldIdNameMap);
        if (!Objects.isNull(queueData)) {
            List<QueueData> queueDataList = new ArrayList<>();
            queueDataList.add(queueData);
            ObjectQueue.putDataIntoQueue(queueDataList);
        }
    }

    private QueueData doTaskFieldRelaIdMapped(final Integer lineNumber, final String lineContent, final FileTaskEntity task, final Map<Long, String> fieldIdNameMap) throws DataStreamException {
        FileFormatEntity fileFormatA = (FileFormatEntity) task.getAObject();
        Map<Long, String> objectDataMapped = new HashMap<>();
        Map<String, String> fieldNameValueMap = new HashMap<>();
        List<String> mapKeyList = new ArrayList<>();

        FileLineDataEntity fileLineData = matchFileLineDataByFormat(lineNumber, lineContent, fileFormatA);
//        if (fileLineData.getLineDataType().equals(FILE_LINE_BODY) && !CollectionUtils.isEmpty(fileLineData.getLineDataMap())) {
//            //进行过滤处理
//            if (Boolean.TRUE.equals(filterFileData(fileLineData.getLineDataMap(), fileFormatA))) {
//                return null;
//            }
//
//            Iterator<Map.Entry<Long, String>> iterator = fileLineData.getLineDataMap().entrySet().iterator();
//            Map.Entry<Long, String> entry;
//            while (iterator.hasNext()) {
//                entry = iterator.next();
//                Long key = entry.getKey();
//                List<FileTaskFieldRelaEntity> taskFieldRelaList = task.getFileTaskFieldRelaList().
//                        stream().filter(x -> x.getAObjectFieldId().equals(key)).collect(Collectors.toList());
//                if (!CollectionUtils.isEmpty(taskFieldRelaList)) {
//                    objectDataMapped.put(taskFieldRelaList.get(0).getTaskFieldRelaId(), entry.getValue());
//                    fieldNameValueMap.put(fieldIdNameMap.get(key), entry.getValue());
//                    if (taskFieldRelaList.get(0).getCompareFlag() != null && taskFieldRelaList.get(0).getCompareFlag().equals(COMPARE_ABLE_FLAG_YES)) {
//                        mapKeyList.add(entry.getValue());
//                    }
//                }
//            }
//        }

        if (!CollectionUtils.isEmpty(objectDataMapped)) {
            QueueData dataObject = new QueueData();
            dataObject.setDataMap(objectDataMapped);
            dataObject.setFieldNameValueMap(fieldNameValueMap);
            dataObject.setComparedKey(genMapKey(mapKeyList));
            return dataObject;
        }

        return null;
    }


    /**
     * 根据文件行数据去匹配出文件数据
     *
     * @param lineNumber  文件行数
     * @param lineContent 文件行内容
     * @param fileFormat  文件格式
     * @return 文件正文行数据
     * @throws DataStreamException
     */
    protected FileLineDataEntity matchFileLineDataByFormat(final Integer lineNumber, final String lineContent, final FileFormatEntity fileFormat) throws DataStreamException {
        FileLineDataEntity fileLineData = new FileLineDataEntity();
        //step 1: match Line,这步一定放在最前面
        if (matchFileLineDataByFormatStepOne(lineNumber, lineContent, fileFormat, fileLineData)) {
            return fileLineData;
        }

        //step 2: match body
        if (matchFileLineDataByFormatStepTwo(lineNumber, lineContent, fileFormat, fileLineData)) {
            return fileLineData;
        }

        //step 3: match Line -1
        if (matchFileLineDataByFormatStepThree(lineContent, fileFormat, fileLineData)) {
            return fileLineData;
        }

        //step 4: 一个没匹配到是否报错
        log.error("文件行数数据匹配文件定义格式失败::lineNumber={},lineContent={}", lineNumber, lineContent);
        throw new DataStreamException(OPER_MATCH_FILE_LINE_DATA_BY_FORMAT_ERROR);
    }

    private Boolean matchFileLineDataByFormatStepOne(final Integer lineNumber, final String lineContent, final FileFormatEntity fileFormat, FileLineDataEntity fileLineData) throws DataStreamException {
        Boolean returnValue = false;
        if (!Objects.isNull(fileFormat.getFileBody()) && !Objects.isNull(fileFormat.getFileBody().getFixBeginLine())) {
            if (lineNumber >= fileFormat.getFileBody().getFixBeginLine()) {
                fileLineData.setLineDataType(FILE_LINE_BODY);
                fileLineData.setLineDataMap(parseLineFields(fileFormat.getFileBody().getSplitFlag(), lineContent, fileFormat.getFileBody().getFileFieldList()));
                returnValue = true;
            }
        } else if (!Objects.isNull(fileFormat.getFileBody())) {
            fileLineData.setLineDataType(FILE_LINE_BODY);
            fileLineData.setLineDataMap(parseLineFields(fileFormat.getFileBody().getSplitFlag(), lineContent, fileFormat.getFileBody().getFileFieldList()));
            returnValue = true;
        }
        return returnValue;
    }

    private Boolean matchFileLineDataByFormatStepTwo(final Integer lineNumber, final String lineContent, final FileFormatEntity fileFormat, FileLineDataEntity fileData) throws DataStreamException {
        Boolean returnValue = false;

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
                if (iterator.getFixLinePosition().equals(lineNumber)) {
                    fileData.setLineDataType(FILE_LINE_SPECIAL);
                    fileData.setLineDataMap(parseLineFields(iterator.getSplitFlag(), lineContent, iterator.getFileFieldList()));
                    returnValue = true;
                }
            }
        }
        return returnValue;
    }

    private Boolean matchFileLineDataByFormatStepThree(final String lineContent, final FileFormatEntity fileFormat, FileLineDataEntity fileData) throws DataStreamException {
        Boolean returnValue = false;

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
                if (iterator.getFixLinePosition().equals(-1)) {
                    fileData.setLineDataType(FILE_LINE_SPECIAL);
                    fileData.setLineDataMap(parseLineFields(iterator.getSplitFlag(), lineContent, iterator.getFileFieldList()));
                    returnValue = true;
                }
            }
        }

        return returnValue;
    }


    private Map<String, String> parseLineFields(Integer splitFlag, String lineContent, List<FileFieldEntity> fileFieldList) throws DataStreamException {
        List<String> splitData = new ArrayList<>();
        switch (splitFlag) {
            case SPLIT_FLAG_FIX_WIDTH:
                splitData.addAll(getLineFieldValueByFixWidth(lineContent, fileFieldList));
                break;
            case SPLIT_FLAG_VERTICAL_LINE:
                splitData.addAll(getLineFieldsBySplitFlag(lineContent, "\\|"));
                break;
            case SPLIT_FLAG_COMMA:
                splitData.addAll(getLineFieldsBySplitFlag(lineContent, ","));
                break;
            case SPLIT_FLAG_AND:
                splitData.addAll(getLineFieldsBySplitFlag(lineContent, "\\&"));
                break;
            default:
                break;
        }

        if (fileFieldList.size() != splitData.size()) {
            log.error("fileFieldList.size()={},splitData.size()={},lineContent={}", fileFieldList.size(), splitData.size(), lineContent);
            throw new DataStreamException(OPER_GET_LINE_FIELDS_ERROR.getCode(), "行数数据" + lineContent + ",字段定义个数【" + fileFieldList.size() + "】,行数据个数【" + splitData.size() + "】不匹配错误!");
        }

        //fileFieldList一定保证字段定义顺序
        Map<String, String> dataMap = new HashMap<>(fileFieldList.size());
        for (int i = 0; i < fileFieldList.size(); i++) {
            dataMap.put(fileFieldList.get(i).getFieldName(), splitData.get(i));
        }
        return dataMap;
    }

    private List<String> getLineFieldsBySplitFlag(String lineContent, String splitFlag) {
        if (lineContent.substring(0, 1).equals("|")) {
            return Arrays.asList(lineContent.substring(1, lineContent.length()).split(splitFlag));
        } else {
            return Arrays.asList(lineContent.split(splitFlag));
        }
    }

    /**
     * 根据文件过滤规则进行数据过滤处理
     *
     * @param dataFileLineMap
     * @param fileFormat
     * @return Boolean
     * @throws DataStreamException
     */
    private Boolean filterFileData(Map<Long, String> dataFileLineMap, final FileFormatEntity fileFormat) throws DataStreamException {
        if (!CollectionUtils.isEmpty(dataFileLineMap) && !CollectionUtils.isEmpty(fileFormat.getFileFilterMap())) {
            Iterator<Map.Entry<Integer, List<FileFilterEntity>>> iterator = fileFormat.getFileFilterMap().entrySet().iterator();
            Map.Entry<Integer, List<FileFilterEntity>> entry;
            while (iterator.hasNext()) {
                entry = iterator.next();
                //完全匹配，则true
                if (filterFileDataByFilter(dataFileLineMap, entry.getValue()).equals(entry.getValue().size())) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private Integer filterFileDataByFilter(Map<Long, String> dataFileLineMap, final List<FileFilterEntity> FileFilterList) throws DataStreamException {
        Integer equalFlag = 0;
        for (FileFilterEntity fileFilterIterator : FileFilterList) {
            if (!dataFileLineMap.containsKey(fileFilterIterator.getFileFieldId())) {
                throw new DataStreamException("filterFileDataByFilter", "filterFileDataByFilter: dataFileLineMap containsKey fail !");
            }

            if (filterFileDataByFilter(fileFilterIterator, dataFileLineMap)) {
                equalFlag++;
            }
        }
        return equalFlag;
    }

    private Boolean filterFileDataByFilter(FileFilterEntity fileFilter, Map<Long, String> dataFileLineMap) {
        Boolean returnValue = false;
        switch (fileFilter.getSymbolId()) {
            case SYMBOL_ID_EQUAL:
                if (fileFilter.getFileFieldValue().equals(dataFileLineMap.get(fileFilter.getFileFieldId()))) {
                    returnValue = true;
                }
                break;
            case SYMBOL_ID_MORE:
                if (Long.parseLong(fileFilter.getFileFieldValue()) > Long.parseLong(dataFileLineMap.get(fileFilter.getFileFieldId()))) {
                    returnValue = true;
                }
                break;
            case SYMBOL_ID_LESS:
                if (Long.parseLong(fileFilter.getFileFieldValue()) < Long.parseLong(dataFileLineMap.get(fileFilter.getFileFieldId()))) {
                    returnValue = true;
                }
                break;
            case SYMBOL_ID_MORE_EQUAL:
                if (Long.parseLong(fileFilter.getFileFieldValue()) >= Long.parseLong(dataFileLineMap.get(fileFilter.getFileFieldId()))) {
                    returnValue = true;
                }
                break;
            case SYMBOL_ID_LESS_EQUAL:
                if (Long.parseLong(fileFilter.getFileFieldValue()) <= Long.parseLong(dataFileLineMap.get(fileFilter.getFileFieldId()))) {
                    returnValue = true;
                }
                break;
            default:
                break;
        }
        return returnValue;
    }

    private List<String> getLineFieldValueByFixWidth(String lineContent, List<FileFieldEntity> fileFieldList) {
        List<String> splitFieldValue = new ArrayList<>();
        int position = 0;
        for (FileFieldEntity iterator : fileFieldList) {
            splitFieldValue.add(Optional.ofNullable(lineContent.substring(position, position + iterator.getFixWidth()).trim()).orElse("null"));
            position += iterator.getFixWidth();
        }
        return splitFieldValue;
    }

    public Map<Long, String> doFieldIdNameMap(final Object objectFormat) {
        FileFormatEntity fileFormat = (FileFormatEntity) objectFormat;
        Map<Long, String> fieldIdNameMap = new HashMap<>(fileFormat.getFileBody().getFileFieldList().size());
        fileFormat.getFileBody().getFileFieldList().forEach(x -> fieldIdNameMap.put(x.getFileFieldId(), x.getFieldName()));
        return fieldIdNameMap;
    }


    public Integer checkFileLineData(Integer bodyLineSum, Integer lineNumber, String lineContent, List<FileLineDataEntity> fileLineSpecialDataList, final FileFormatEntity fileFormat) throws DataStreamException {
        FileLineDataEntity fileLineData = matchFileLineDataByFormat(lineNumber, lineContent, fileFormat);
        if (fileLineData.getLineDataType().equals(FILE_LINE_BODY)) {
            bodyLineSum++;

            //判断是否存在需要累加稽核字段
            if (!CollectionUtils.isEmpty(fileFormat.getBodyHaveSumFieldMap())) {
                Iterator<Map.Entry<Long, Long>> iterator = fileFormat.getBodyHaveSumFieldMap().entrySet().iterator();
                Map.Entry<Long, Long> entry;
                while (iterator.hasNext()) {
                    entry = iterator.next();
                    if (!fileLineData.getLineDataMap().containsKey(entry.getKey())) {
                        throw new DataStreamException("checkFileLineData", "can not get data by key from getLineDataMap");
                    }
                    entry.setValue(entry.getValue() + Long.parseLong(fileLineData.getLineDataMap().get(entry.getKey())));
                }
            }
        } else if (fileLineData.getLineDataType().equals(FILE_LINE_SPECIAL)) {
            fileLineData.setLineNumber(lineNumber);
            fileLineSpecialDataList.add(fileLineData);
        }
        return bodyLineSum;
    }

    /**
     * 校验文件中文件头尾送的正文行数和实际计算总行数稽核
     *
     * @param bodyLineSum             文件正文记录总数
     * @param fileLineSpecialDataList 文件特殊行对应数据信息
     * @param fileFormat              文件结构对象
     */
    public void checkFileLineSpecialData(Integer bodyLineSum, final List<FileLineDataEntity> fileLineSpecialDataList, final FileFormatEntity fileFormat) throws DataStreamException {
        //校验文件中文件头尾送的正文行数和实际计算总行数稽核
        for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
            for (FileFieldEntity fileFieldIterator : iterator.getFileFieldList()) {
                //稽核记录总数
                if (!Objects.isNull(fileFieldIterator.getSumLineFlag()) && fileFieldIterator.getSumLineFlag().equals(1)) {
                    checkFileLineSpecialDataForSumRecord(fileFieldIterator, bodyLineSum, fileLineSpecialDataList);
                }

                //稽核指定字段汇总总数
                if (!Objects.isNull(fileFieldIterator.getSumFieldName())) {
                    checkFileLineSpecialDataForOtherSumField(fileFieldIterator, fileLineSpecialDataList, fileFormat);
                }
            }
        }
    }

    private void checkFileLineSpecialDataForSumRecord(FileFieldEntity specialFileField, Integer bodyLineSum, final List<FileLineDataEntity> fileLineSpecialDataList) throws DataStreamException {
        //稽核记录总数
        for (FileLineDataEntity fileDataIterator : fileLineSpecialDataList) {
            if (fileDataIterator.getLineDataMap().containsKey(specialFileField.getFileFieldId())) {
                Integer specialLineSum = Integer.parseInt(fileDataIterator.getLineDataMap().get(specialFileField.getFileFieldId()));
                if (!bodyLineSum.equals(specialLineSum)) {
                    throw new DataStreamException("checkFileLineSpecialDataForSumRecord", String.format("文件行数校验数量不一致：累加行体总数=%s,文件告知行数=%s", bodyLineSum, specialLineSum));
                }
            }
        }
    }

    private void checkFileLineSpecialDataForOtherSumField(FileFieldEntity specialFileField, final List<FileLineDataEntity> fileLineSpecialDataList, final FileFormatEntity fileFormat) throws DataStreamException {
        //获取关联body字段ID
        long bodyFileFieldId = -1L;
        for (FileFieldEntity iterator : fileFormat.getFileBody().getFileFieldList()) {
            if (iterator.getFieldName().equalsIgnoreCase(specialFileField.getSumFieldName())) {
                bodyFileFieldId = iterator.getFileFieldId();
                break;
            }
        }

        //稽核指定字段汇总总数
        for (FileLineDataEntity fileDataIterator : fileLineSpecialDataList) {
            if (fileDataIterator.getLineDataMap().containsKey(specialFileField.getFileFieldId()) && fileFormat.getBodyHaveSumFieldMap().containsKey(bodyFileFieldId)) {
                Long fileSumFieldValue = Long.parseLong(fileDataIterator.getLineDataMap().get(specialFileField.getFileFieldId()));
                Long specialSumFieldValue = fileFormat.getBodyHaveSumFieldMap().get(bodyFileFieldId);

                if (!fileSumFieldValue.equals(specialSumFieldValue)) {
                    String result = String.format("校验失败:字段【%s】数值不一致：累加总数=%d,告知数值=%d!", specialFileField.getSumFieldName(), fileSumFieldValue, specialSumFieldValue);
                    throw new DataStreamException("checkFileLineSpecialDataForOtherSumField", result);
                }
            }
        }
    }

    protected void isWorkThreadFail() throws DataStreamException {
        if (ObjectQueue.isWorkThreadFail()) {
            throw new DataStreamException("doBusinessFromQueue", "被通知中止退出处理");
        }
    }

    public void deleteFile(final String localPath, final String fileName) throws DataStreamException {
        String pathFileName = localPath + "/" + fileName;
        File deleteFile = new File(pathFileName);
        if (deleteFile.exists()) {
            if (!deleteFile.delete()) {
                log.error("文件删除失败:deleteFile:{}", pathFileName);
                throw new DataStreamException("deleteFile", "删除文件失败,文件:" + pathFileName);
            }
        }
    }

    public void renameFile(final String filePath, String oldFileName, String newFileName) {
        File oldFile = new File(filePath + "/" + oldFileName);
        if (oldFile.exists()) {
            File newFile = new File(filePath + "/" + newFileName);
            if (!oldFile.renameTo(newFile)) {
                log.error("文件重命名失败:oldName:{}=>newName:{}", oldFileName, newFileName);
            }
        }
    }

    public void finishFile(final Long taskId, String localPath, final String objectFileName) throws DataStreamException {

        //修改临时名称为正式名称
        renameFile(localPath, genFilePrefix(taskId) + objectFileName, objectFileName);
    }

    protected void releaseReaderResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);
        if (lineNumberStaticMap.containsKey(stringKey)) {
            lineNumberStaticMap.remove(stringKey);
        }

        if (parseFileLineDataLockStaticMap.containsKey(stringKey)) {
            parseFileLineDataLockStaticMap.remove(stringKey);
        }
    }

    protected void releaseWriterResource(final Long taskId, final Long fileFormatId) throws DataStreamException {
        String stringKey = genStringKey(taskId, fileFormatId);
        if (bodyDataWriteIntoFileLockStaticMap.containsKey(stringKey)) {
            bodyDataWriteIntoFileLockStaticMap.remove(stringKey);
        }

        if(dataWriteIntoFileLineSumStaticMap.containsKey(stringKey)){
            dataWriteIntoFileLineSumStaticMap.remove(stringKey);
        }

        if(dataWriteIntoFileLineFieldSumStaticMap.containsKey(stringKey)){
            dataWriteIntoFileLineFieldSumStaticMap.remove(stringKey);
        }
    }

    protected void mergeAndSumFieldValues(String stringKey, Integer dataObjectSize, Map<String, Long> dataSumFieldMap){
        // 如果key不存在，直接放入newValue；如果存在，则使用 Integer.sum(oldValue, newValue) 进行累加
        dataWriteIntoFileLineSumStaticMap.merge(stringKey, dataObjectSize, Integer::sum);

        if (!CollectionUtils.isEmpty(dataSumFieldMap)) {
            if (dataWriteIntoFileLineFieldSumStaticMap.containsKey(stringKey)) {
                for (Map.Entry<String, Long> entry : dataSumFieldMap.entrySet()) {
                    dataWriteIntoFileLineFieldSumStaticMap.get(stringKey).merge(entry.getKey(), entry.getValue(), Long::sum);
                }
            } else {
                dataWriteIntoFileLineFieldSumStaticMap.put(stringKey, dataSumFieldMap);
            }
        }
    }
}
