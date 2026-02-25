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
package com.itman.datastream.admin.controller;

import com.itman.datastream.admin.controller.domain.request.*;
import com.itman.datastream.admin.controller.domain.response.*;
import com.itman.datastream.admin.service.IFileService;
import com.itman.datastream.common.constant.FileTypeEnum;
import com.itman.datastream.common.entity.*;
import com.itman.datastream.common.errcode.DataStreamException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

import static com.itman.datastream.common.constant.DataStreamConstant.*;
import static com.itman.datastream.common.constant.FileTypeEnum.EXCEL;
import static com.itman.datastream.common.errcode.DataStreamErrorCode.*;
import static com.itman.datastream.common.utils.ChangeNameUtils.*;

@Slf4j
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileFormatController {
    private final IFileService fileService;

    @PostMapping(path = "/addFileFormat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AddFileFormatResponse> addFileFormat(@RequestBody AddFileFormatRequest addFileFormatRequest) {
        AddFileFormatResponse addFileFormatResponse = new AddFileFormatResponse();
        try {
            FileFormatEntity fileFormat = addFileFormatRequest.getFileFormat();
            //特殊行校验
            fileFormatSaveCheck(fileFormat);

            fileService.createFileFormat(fileFormat);
        } catch (DataStreamException aie) {
            addFileFormatResponse.setErrorCode(aie.getErrCode());
            addFileFormatResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            addFileFormatResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            addFileFormatResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(addFileFormatResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/modifyFileFormat", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ModifyFileFormatResponse> modifyFileFormat(@RequestBody ModifyFileFormatRequest modifyFileFormatRequest) {
        ModifyFileFormatResponse modifyFileFormatResponse = new ModifyFileFormatResponse();
        try {
            FileFormatEntity fileFormat = modifyFileFormatRequest.getFileFormat();

            checkFileStateOff(fileFormat.getFileFormatId());
            //特殊行校验
            fileFormatSaveCheck(fileFormat);

            fileService.modifyFileInstance(fileFormat);
        } catch (DataStreamException aie) {
            modifyFileFormatResponse.setErrorCode(aie.getErrCode());
            modifyFileFormatResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            modifyFileFormatResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            modifyFileFormatResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(modifyFileFormatResponse, HttpStatus.OK);
    }

    private void fileFormatSaveCheck(FileFormatEntity fileFormat) throws DataStreamException {
        //校验特殊行存在情况下
//        if (FileTypeEnum.of(fileFormat.getFileType()).equals(EXCEL)) {
//            String fileNamSuffixes = fileFormat.getFileNameFormat().substring(fileFormat.getFileNameFormat().lastIndexOf(".") + 1);
//            if (!fileNamSuffixes.equals("xls") && !fileNamSuffixes.equals("xlsx")) {
//                throw new DataStreamException("filePostOperateSaveCheck001", "请明确excel文件后缀类型xls、xlsx");
//            }
//        }

        if (CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
            return;
        }

        for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
            fileFormatCheckFileSpecial(iterator, fileFormat.getFileBody().getFixBeginLine(), fileFormat.getFileBody().getFileFieldList());
        }
    }


    private void fileFormatCheckFileSpecial(FileSpecialEntity fileSpecial, Integer fixBeginLine, final List<FileFieldEntity> fileBodyFieldList) throws DataStreamException {
        if (fileSpecial.getSplitFlag() == null || fileSpecial.getSplitFlag() <= 0L) {
            throw new DataStreamException("filePostOperateSaveCheck002", "请配置特殊行属性:分隔符!");
        }

        if (fileSpecial.getFixLinePosition() == null) {
            throw new DataStreamException("filePostOperateSaveCheck003", "请配置特殊行属性:固定行位!");
        }

        if (CollectionUtils.isEmpty(fileSpecial.getFileFieldList())) {
            throw new DataStreamException("filePostOperateSaveCheck004", "请配置特殊行字段明细!");
        }

        if (fileSpecial.getFixLinePosition() >= fixBeginLine) {
            throw new DataStreamException("filePostOperateSaveCheck005", "请配置特殊行固定行位值必须小于行体固定开始行值!");
        }

        Set<Integer> positionSet = new HashSet<>();
        for (FileFieldEntity iterator : fileSpecial.getFileFieldList()) {
            fileFormatSaveCheckFileSpecialField(iterator, positionSet, fileBodyFieldList);
        }

        if (fileSpecial.getFileFieldList().size() != positionSet.size()) {
            throw new DataStreamException("filePostOperateSaveCheck010", "特殊行请合理配置字段占位,同一行不能冲突!");
        }
    }


    private void fileFormatSaveCheckFileSpecialField(FileFieldEntity fileField, Set<Integer> positionSet, final List<FileFieldEntity> fileBodyFieldList) throws DataStreamException {
        if (StringUtils.isEmpty(fileField.getFieldName())) {
            throw new DataStreamException("filePostOperateSaveCheck006", "请配置特殊行字段名称!");
        }

        if (fileField.getPosition() == null) {
            throw new DataStreamException("filePostOperateSaveCheck007", "请配置特殊行字段占位!");
        }

        if (!StringUtils.isEmpty(fileField.getSumFieldName())) {
            //如果特殊行存在汇总字段不为空，则需要去行体明细去配置对应字段名称
            List<FileFieldEntity> fileFieldFilterList = fileBodyFieldList.stream().filter(x -> x.getFieldName().equalsIgnoreCase(fileField.getSumFieldName())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(fileFieldFilterList))
                throw new DataStreamException("filePostOperateSaveCheck008", "特殊行汇总字段匹配不到行体字段!");

            if (!Objects.isNull(fileField.getSumLineFlag()) && fileField.getSumLineFlag().equals(1)) {
                throw new DataStreamException("filePostOperateSaveCheck009", "同一个字段不能即是总行数又是字段数量值累加!");
            }
        }

        positionSet.add(fileField.getPosition());
    }

    @PostMapping(path = "/queryFileRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryFileRowsResponse> queryFileRows(@RequestBody QueryFileRowsRequest queryFileRowsRequest) {
        QueryFileRowsResponse queryFileRowsResponse = new QueryFileRowsResponse();
        try {
            queryFileRowsResponse.setTotal(fileService.selectFileFormatCount(queryFileRowsRequest.getQueryFlag(), queryFileRowsRequest.getQueryValue()));
            if (queryFileRowsResponse.getTotal() > 0) {
                List<FileFormatEntity> fileFormatList = fileService.selectFileFormatByPage(queryFileRowsRequest.getQueryFlag(), queryFileRowsRequest.getQueryValue(), queryFileRowsRequest.getPage(), queryFileRowsRequest.getCount());
                queryFileRowsResponse.setFileFormatList(fileFormatList);
            }
        } catch (DataStreamException aie) {
            queryFileRowsResponse.setErrorCode(aie.getErrCode());
            queryFileRowsResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryFileRowsResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryFileRowsResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryFileRowsResponse, HttpStatus.OK);
    }

    private void checkFileStateOff(Long fileFormatId) throws DataStreamException {
        List<FileFormatEntity> fileFormatList = fileService.selectFileFormatByPage(FILE_FORMAT_QUERY_FLAG_FILE_FORMAT_ID, fileFormatId.toString(), 1, 1);
        if (CollectionUtils.isEmpty(fileFormatList)) {
            throw new DataStreamException(OPER_SELECT_FILE_FORMAT_BY_PAGE_ERROR);
        }

        if (!fileFormatList.get(0).getOnLineFlag().equals(COMMON_STATE_OFFLINE)) {
            throw new DataStreamException(OPER_FILE_FORMAT_STATE_NOT_OFF_ERROR);
        }
    }

    @PostMapping(path = "/operateFileRows", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<OperateFileRowsResponse> operateFileRows(@RequestBody OperateFileRowsRequest operateFileRowsRequest) {
        OperateFileRowsResponse operateFileRowsResponse = new OperateFileRowsResponse();
        try {
            Integer action = operateFileRowsRequest.getAction();
            Long fileFormatId = operateFileRowsRequest.getFileFormatId();

            if (action.equals(DATA_STREAM_ACTION_DELETE)) {
                checkFileStateOff(fileFormatId);

                fileService.deleteFileInstance(fileFormatId);
            } else if (action.equals(DATA_STREAM_ACTION_ONLINE)) {
                //上线
                fileService.updateFileFormatOnLineFlagById(fileFormatId, COMMON_STATE_ONLINE);
            } else if (action.equals(DATA_STREAM_ACTION_OFFLINE)) {
                //todo 下线：先校验，校验关联的任务配置表是否都是下线状态
                fileService.updateFileFormatOnLineFlagById(fileFormatId, COMMON_STATE_OFFLINE);
            } else if (action.equals(DATA_STREAM_ACTION_COPY)) {
                fileService.copyFileInstance(fileFormatId);
            } else if (action.equals(DATA_STREAM_ACTION_CHECK)) {
                fileService.checkFileFormat(fileFormatId);
            } else if (action.equals(DATA_STREAM_ACTION_FTP)) {
                operateFileRowsResponse.setFileNameList(fileService.testFileFtp(operateFileRowsRequest.getFileFormat()));
            }
        } catch (DataStreamException aie) {
            operateFileRowsResponse.setErrorCode(aie.getErrCode());
            operateFileRowsResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            operateFileRowsResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            operateFileRowsResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(operateFileRowsResponse, HttpStatus.OK);
    }

    @PostMapping(path = "/queryFileInfo", produces = MediaType.APPLICATION_JSON_UTF8_VALUE, consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<QueryFileInfoResponse> queryFileInfo(@RequestBody QueryFileInfoRequest queryFileInfoRequest) {
        QueryFileInfoResponse queryFileInfoResponse = new QueryFileInfoResponse();
        try {
            Long fileFormatId = queryFileInfoRequest.getFileFormatId();
            Integer viewFlag = queryFileInfoRequest.getViewFlag();
            queryFileInfoResponse.setFileFormat(queryFileInfo(fileFormatId, viewFlag));

        } catch (DataStreamException aie) {
            queryFileInfoResponse.setErrorCode(aie.getErrCode());
            queryFileInfoResponse.setErrorMsg(aie.getErrMsg());
            log.error("DataStreamException=", aie);
        } catch (Exception e) {
            queryFileInfoResponse.setErrorCode(UNKNOWN_ERROR.getCode());
            queryFileInfoResponse.setErrorMsg(UNKNOWN_ERROR.getMessage());
            log.error("Exception=", e);
        }

        return new ResponseEntity<>(queryFileInfoResponse, HttpStatus.OK);
    }

    public FileFormatEntity queryFileInfo(Long fileFormatId, Integer viewFlag) throws DataStreamException {
        viewFlag = Optional.ofNullable(viewFlag).orElse(VIEW_FLAG_SHOW);

        //新增文件对象流程
        FileFormatEntity fileFormat = null;
        if (!(viewFlag.equals(VIEW_FLAG_EDIT) && fileFormatId.equals(-1L))) {
            fileFormat = fileService.makeFileObject(fileFormatId);
        }

        if (Objects.isNull(fileFormat)) {
            fileFormat = new FileFormatEntity();
            fileFormat.setFileFormatId(-1L);
            fileFormat.setFileBody(new FileBodyEntity());
            fileFormat.getFileBody().setFileBodyId(-1L);
        } else {
            fileFormat.setOnLineFlagName(!Objects.isNull(fileFormat.getOnLineFlag()) ? changeOnLineFlagName(fileFormat.getOnLineFlag()) : null);
            fileFormat.setFileNameTypeName(changeFileNameTypeName(fileFormat.getFileNameType()));
            fileFormat.setFileTypeName(FileTypeEnum.of(fileFormat.getFileType()).getName());
            fileFormat.setFileBakActionName(changeFileBakActionName(fileFormat.getFileBakAction()));
            if (!Objects.isNull(fileFormat.getFileBody())) {
                fileFormat.getFileBody().setSplitFlagName(changeSplitFlagName(fileFormat.getFileBody().getSplitFlag()));
            }

            if (!CollectionUtils.isEmpty(fileFormat.getFileSpecialList())) {
                for (FileSpecialEntity iterator : fileFormat.getFileSpecialList()) {
                    iterator.setSplitFlagName(changeSplitFlagName(iterator.getSplitFlag()));
                }
            }
        }

        return fileFormat;
    }
}
