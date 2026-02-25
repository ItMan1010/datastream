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

import com.itman.datastream.admin.service.IFileService;
import com.itman.datastream.common.api.DataSourceFactory;
import com.itman.datastream.common.api.IDatabaseAdapter;
import com.itman.datastream.common.api.IFileApi;
import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.FileTypeEnum;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.common.utils.FTPUtils;
import com.itman.datastream.common.utils.FileUtils;
import com.itman.datastream.engine.dao.DataStreamDao;
import com.itman.datastream.engine.dao.FileDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.OPER_SELECT_FILE_FORMAT_BY_ID_ERROR;
import static com.itman.datastream.common.utils.CommUtils.genPageRow;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements IFileService {
    private final FileDao fileDao;
    private final DataStreamDao dataStreamDao;
    private final DataSourceFactory dataSourceFactory;
    private final DataStreamConfig dataStreamConfig;
    private final List<IFileApi> fileList;


    private IDatabaseAdapter getDataBaseObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    private FileFormatEntity makeFileFormat(Long fileFormatId) throws DataStreamException {
        //获取文件定义
        FileFormatEntity fileFormat = fileDao.selectFileFormat(fileFormatId);
        Optional.ofNullable(fileFormat).orElseThrow(() -> new DataStreamException(OPER_SELECT_FILE_FORMAT_BY_ID_ERROR));

        //获取文件过滤规则
        fileFormat.setFileFilterList(fileDao.selectFileFilter(fileFormatId));
        if (!CollectionUtils.isEmpty(fileFormat.getFileFilterList())) {
            Map<Integer, List<FileFilterEntity>> fileFilterMap = new HashMap<>();
            fileFormat.getFileFilterList().forEach(iterator -> fileFilterMap.computeIfAbsent(iterator.getSymbolGroup(), key -> new ArrayList<>()).add(iterator));
            fileFormat.setFileFilterMap(fileFilterMap);
        }

        //获取文件体
        makeFileFormatBody(fileFormat);

        //获取文件特殊行
        makeFileFormatSpecial(fileFormat);

        return fileFormat;
    }


    private void makeFileFormatSpecial(FileFormatEntity fileFormat) throws DataStreamException {
        //get file line
        List<FileSpecialEntity> fileSpecialList = fileDao.selectFileSpecialByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileSpecialList)) {
            //get file headline fields
            for (FileSpecialEntity iterator : fileSpecialList) {
                List<FileFieldEntity> fileFieldList = fileDao.selectFileField(FILE_LINE_SPECIAL, iterator.getFileSpecialId());
                iterator.setFileFieldList(fileFieldList);
            }

            //just only one head
            fileFormat.setFileSpecialList(fileSpecialList);

            //初始化用于数量累加字段
            fileFormat.setBodyHaveSumFieldMap(new HashMap<>());
            Set<String> sumFieldNameSet = new HashSet<>();
            fileSpecialList.forEach(x -> {
                x.getFileFieldList().forEach(y -> {
                    if (!StringUtils.isEmpty(y.getSumFieldName())) {
                        sumFieldNameSet.add(y.getSumFieldName().toUpperCase());
                    }
                });
            });

            if (!CollectionUtils.isEmpty(sumFieldNameSet) && !CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
                fileFormat.getFileBody().getFileFieldList().forEach(x -> {
                    if (sumFieldNameSet.contains(x.getFieldName().toUpperCase())) {
                        fileFormat.getBodyHaveSumFieldMap().put(x.getFileFieldId(), 0L);
                    }
                });
            }
        }
    }

    private void makeFileFormatBody(FileFormatEntity fileFormat) throws DataStreamException {
        //get file body
        List<FileBodyEntity> fileBodyList = fileDao.selectFileBodyByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileBodyList)) {
            //get file body line fields
            for (FileBodyEntity iterator : fileBodyList) {
                List<FileFieldEntity> fileFieldList = fileDao.selectFileField(FILE_LINE_BODY, iterator.getFileBodyId());
                iterator.setFileFieldList(fileFieldList);
            }

            //just only one body
            fileFormat.setFileBody(fileBodyList.get(0));
        }
    }


    public FileFormatEntity makeFileObject(Long fileObjectId) throws DataStreamException {
        return (FileFormatEntity) makeFileFormat(fileObjectId);
    }

    private IDatabaseAdapter geMetaDbObject() throws DataStreamException {
        return dataSourceFactory.matchDataBase(dataStreamConfig.getMetaDbBaseType());
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void createFileFormat(FileFormatEntity fileFormat) throws DataStreamException {
        fileFormat.setFileFormatId(dataStreamDao.querySequence(SEQ_FILE_FORMAT_ID));
        fileFormat.setOnLineFlag(COMMON_STATE_OFFLINE);
        fileDao.insertFileFormat(fileFormat, geMetaDbObject().makeSqlSystemDate());

        fileFormat.getFileBody().setFileFormatId(fileFormat.getFileFormatId());
        fileFormat.getFileBody().setFileBodyId(dataStreamDao.querySequence(SEQ_FILE_BODY_ID));
        fileDao.insertFileBody(fileFormat.getFileBody(), geMetaDbObject().makeSqlSystemDate());

//        if (CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
//            throw new DataStreamException("createFileInstance004", " fileFieldList is null ! ");
//        }

        modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
                if (!iterator.getSplitFlag().equals(-1)) {
                    iterator.setFileFormatId(fileFormat.getFileFormatId());
                    iterator.setFileSpecialId(dataStreamDao.querySequence(SEQ_FILE_SPECIAL_ID));
                    //这个和下面顺序不能颠倒，该函数需要下面插入需要字段标识值
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);

                    fileDao.insertFileSpecial(iterator, geMetaDbObject().makeSqlSystemDate());
                }
            }
        }
    }

    void modifyFileFieldList(Integer belongFlag, Object fileFieldObject) throws DataStreamException {
        List<FileFieldEntity> fileFieldList = new ArrayList<>();
        Long fileFormatId = null;
        Long belongId = null;
        if (belongFlag.equals(FILE_LINE_SPECIAL)) {
            FileSpecialEntity fileLineSpecial = (FileSpecialEntity) fileFieldObject;
            fileFieldList = fileLineSpecial.getFileFieldList();
            fileFormatId = fileLineSpecial.getFileFormatId();
            belongId = fileLineSpecial.getFileSpecialId();
        } else if (belongFlag.equals(FILE_LINE_BODY)) {
            FileBodyEntity fileLineBody = (FileBodyEntity) fileFieldObject;
            fileFieldList = fileLineBody.getFileFieldList();
            fileFormatId = fileLineBody.getFileFormatId();
            belongId = fileLineBody.getFileBodyId();
        }

        modifyFileFieldList(belongFlag, belongId, fileFormatId, fileFieldList);
    }

    void modifyFileFieldList(Integer belongFlag, Long belongId, Long fileFormatId, List<FileFieldEntity> fileFieldList) throws DataStreamException {

        deleteFileFieldList(belongFlag, belongId, fileFieldList);

        if (!CollectionUtils.isEmpty(fileFieldList)) {
            List<FileFieldEntity> fileFieldListInsert = new ArrayList<>();
            List<FileFieldEntity> fileFieldListUpdate = new ArrayList<>();

            getFileFieldListInsertAndUpdate(belongFlag, belongId, fileFormatId, fileFieldList, fileFieldListInsert, fileFieldListUpdate);

            if (!CollectionUtils.isEmpty(fileFieldListInsert)) {
                fileDao.insertFileFieldList(fileFieldListInsert, geMetaDbObject().makeSqlSystemDate());
            }

            if (!CollectionUtils.isEmpty(fileFieldListUpdate)) {
                for (FileFieldEntity iterator : fileFieldListUpdate) {
                    fileDao.updateFileField(iterator);
                }
            }
        }
    }

    private void getFileFieldListInsertAndUpdate(Integer belongFlag, Long belongId, Long fileFormatId, List<FileFieldEntity> fileFieldList, List<FileFieldEntity> fileFieldListInsert, List<FileFieldEntity> fileFieldListUpdate) throws DataStreamException {
        List<Integer> positionList = new ArrayList<>();
        for (FileFieldEntity iterator : fileFieldList) {
            //在前端也校验一下
            if (positionList.contains(iterator.getPosition())) {
                throw new DataStreamException("modifyFileFieldList001", "位移字段有重复,请确认");
            }

            positionList.add(iterator.getPosition());

            iterator.setFileFormatId(fileFormatId);
            iterator.setBelongFlag(belongFlag);
            iterator.setBelongId(belongId);

            if (iterator.getFileFieldId() == null || iterator.getFileFieldId().equals(-1L)) {
                //新增字段
                iterator.setFileFieldId(dataStreamDao.querySequence(SEQ_FILE_FIELD_ID));
                iterator.setPosition(iterator.getPosition());
                fileFieldListInsert.add(iterator);
            } else if (iterator.getFileFieldId() > 0L) {
                //更新
                fileFieldListUpdate.add(iterator);
            }
        }

    }


    void deleteFileFieldList(Integer belongFlag, Long belongId, List<FileFieldEntity> fileFieldList) throws DataStreamException {
        //先增加校验字段记录那些是要删除的
        List<FileFieldEntity> fileFieldListDelete = new ArrayList<>();
        List<FileFieldEntity> fileFieldListCompared = fileDao.selectFileField(belongFlag, belongId);
        for (FileFieldEntity iterator : fileFieldListCompared) {
            if (fileFieldList == null || fileFieldList.stream().filter(x -> x.getFileFieldId() != null && x.getFileFieldId().equals(iterator.getFileFieldId())).collect(Collectors.toList()).size() == 0) {
                fileFieldListDelete.add(iterator);
            }
        }

        for (FileFieldEntity iterator : fileFieldListDelete) {
            fileDao.deleteFileField(iterator.getFileFieldId());
        }
    }


    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void modifyFileInstance(FileFormatEntity fileFormat) throws DataStreamException {
        fileDao.updateFile(fileFormat);

        fileFormat.getFileBody().setFileFormatId(fileFormat.getFileFormatId());
        if (fileFormat.getFileBody().getFileBodyId().equals(-1L)) {
            //body新增
            fileFormat.getFileBody().setFileBodyId(dataStreamDao.querySequence(SEQ_FILE_BODY_ID));
            modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());
            fileDao.insertFileBody(fileFormat.getFileBody(), geMetaDbObject().makeSqlSystemDate());
        } else {
            //body修改
            fileDao.updateFileBody(fileFormat.getFileBody());
            modifyFileFieldList(FILE_LINE_BODY, fileFormat.getFileBody());
        }

        //特殊行判断删除操作
        modifyFileInstanceDelete(fileFormat);

        modifyFileInstanceUpdateOrInsert(fileFormat);
    }

    public void modifyFileInstanceDelete(FileFormatEntity fileFormat) throws DataStreamException {
        //特殊行判断删除操作
        List<FileSpecialEntity> fileLineSpecialListDelete = new ArrayList<>();
        List<FileSpecialEntity> fileLineSpecialListCompared = fileDao.selectFileSpecialByFileFormatId(fileFormat.getFileFormatId());
        if (!CollectionUtils.isEmpty(fileLineSpecialListCompared)) {
            for (FileSpecialEntity iterator : fileLineSpecialListCompared) {
                if (fileFormat.getFileSpecialList() == null ||
                        fileFormat.getFileSpecialList().stream().filter(x->x.getFileSpecialId()!=null).
                                filter(x -> x.getFileSpecialId().equals(iterator.getFileSpecialId())).collect(Collectors.toList()).size() == 0) {
                    fileLineSpecialListDelete.add(iterator);
                }
            }
        }

        //特殊行delete操作
        if (!CollectionUtils.isEmpty(fileLineSpecialListDelete)) {
            //先删除特殊关联字段信息
            for (FileSpecialEntity iterator : fileLineSpecialListDelete) {
                if (!CollectionUtils.isEmpty(iterator.getFileFieldList())) {
                    iterator.getFileFieldList().forEach(x -> {
                        if (x.getFileFieldId() > 0L) {
                            fileDao.deleteFileField(x.getFileFieldId());
                        }
                    });
                }

                //删除特殊行
                fileDao.deleteFileSpecialById(iterator.getFileSpecialId());
            }
        }
    }

    public void modifyFileInstanceUpdateOrInsert(FileFormatEntity fileFormat) throws DataStreamException {
        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
                iterator.setFileFormatId(fileFormat.getFileFormatId());
                if (iterator.getFileSpecialId() == null || iterator.getFileSpecialId().equals(-1L)) {
                    iterator.setFileSpecialId(dataStreamDao.querySequence(SEQ_FILE_SPECIAL_ID));
                    //这个和下面顺序不能颠倒，该函数需要下面插入需要字段标识值
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);
                    fileDao.insertFileSpecial(iterator, geMetaDbObject().makeSqlSystemDate());
                } else {
                    fileDao.updateFileSpecial(iterator);
                    modifyFileFieldList(FILE_LINE_SPECIAL, iterator);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void deleteFileInstance(Long fileFormatId) throws DataStreamException {
        fileDao.deleteFileFieldByFileFormatId(fileFormatId);
        fileDao.deleteFileFilterByFileFormatId(fileFormatId);
        fileDao.deleteFileBodyByFileFormatId(fileFormatId);
        fileDao.deleteFileSpecialByFileFormatId(fileFormatId);
        fileDao.deleteFileFormatById(fileFormatId);
    }

    @Override
    @Transactional(rollbackFor = DataStreamException.class)
    public void copyFileInstance(Long fileFormatId) throws DataStreamException {
        FileFormatEntity fileFormat = makeFileObject(fileFormatId);
        fileFormat.setFileNameFormat("copy_" + fileFormat.getFileNameFormat());
        if (!CollectionUtils.isEmpty(fileFormat.getFileBody().getFileFieldList())) {
            fileFormat.getFileBody().getFileFieldList().forEach(x -> x.setFileFieldId(-1L));
        }

        if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            fileFormat.getFileSpecialList().forEach(x -> {
                if (!CollectionUtils.isEmpty(x.getFileFieldList())) {
                    x.getFileFieldList().forEach(y -> y.setFileFieldId(-1L));
                }
            });
        }
        createFileFormat(fileFormat);
    }

    @Override
    public Integer selectFileFormatCount(Integer queryFlag, String queryValue) throws DataStreamException {
        queryValue = queryFlag.equals(FILE_FORMAT_QUERY_FLAG_FILE_NAME) ? "%" + queryValue + "%" : queryValue;
        return fileDao.selectFileFormatCount(queryFlag, queryValue);
    }

    @Override
    public List<FileFormatEntity> selectFileFormatByPage(Integer queryFlag, String queryValue, Integer page, Integer count) throws DataStreamException {
        queryValue = queryFlag.equals(FILE_FORMAT_QUERY_FLAG_FILE_NAME) ? "%" + queryValue + "%" : queryValue;
        return fileDao.selectFileFormatByPage(queryFlag, queryValue, getDataBaseObject().makeSqlLimit(genPageRow(page, count), count));
    }


    @Override
    public void checkFileFormat(Long fileFormatId) throws DataStreamException {
        if (fileFormatId == null || fileFormatId == -1L) {
            throw new DataStreamException("checkFileFormat001", "请先保存再做校验!");
        }

        FileFormatEntity fileFormat = makeFileObject(fileFormatId);

        doBusinessCheck(fileFormat);
    }

    private void doBusinessCheck(final FileFormatEntity fileFormat) throws DataStreamException {
        List<String> fileNameList = getFileNameList(fileFormat);
        for (String fileName : fileNameList) {
            fileList.stream().filter(x -> x.chooseFile(FileTypeEnum.of(fileFormat.getFileType()))).findFirst().
                    orElseThrow(() -> new DataStreamException("doBusinessCheck", "根据对象标识匹配对象失败")).
                    checkFileLineData(fileFormat.getLocalPath(), fileName, fileFormat);
        }
    }


    /**
     * 根据文件定义获取文件名称集合
     * 如果文件名称定义是正则表达式，可以获取多个文件
     *
     * @param fileFormat
     * @return List<String> 文件名称集合
     */
    private List<String> getFileNameList(FileFormatEntity fileFormat) {
        List<String> fileNameList = new ArrayList<>();

        switch (fileFormat.getFileNameType()) {
            case FILE_NAME_TYPE_FIX:
                fileNameList.add(fileFormat.getFileNameFormat());
                break;
            case FILE_NAME_TYPE_PATTERN:
                Pattern pattern = Pattern.compile(fileFormat.getFileNameFormat());
                List<String> pathFileNameList = FileUtils.getFiles(fileFormat.getLocalPath());
                pathFileNameList.forEach(fileName -> {
                    if (pattern.matcher(fileName).matches()) {
                        fileNameList.add(fileName);
                    }
                });
                break;
            case FILE_NAME_TYPE_EXTEND:
                //todo 待扩展
                break;
            default:
                break;
        }

        return fileNameList;
    }


    @Override
    public List<String> testFileFtp(FileFormatEntity fileFormat) throws DataStreamException {
        return FTPUtils.getFileList(fileFormat.getFtpHost(),
                                                    Integer.parseInt(fileFormat.getFtpPort()),
                                                    fileFormat.getFtpUser(),
                                                    fileFormat.getFtpPasswd(),
                                                    fileFormat.getFtpType(),
                                                    fileFormat.getFtpPath());
    }

    @Override
    public void updateFileFormatOnLineFlagById(Long fileFormatId, Integer onLineFlag) throws DataStreamException {
        fileDao.updateFileFormatOnLineFlagById(fileFormatId, onLineFlag);
    }
}
