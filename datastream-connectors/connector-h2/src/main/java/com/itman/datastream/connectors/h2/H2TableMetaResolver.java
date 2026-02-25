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
package com.itman.datastream.connectors.h2;

import com.itman.datastream.common.config.DataStreamConfig;
import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.common.errcode.DataStreamException;
import com.itman.datastream.connectors.common.JdbcTableMetaResolver;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class H2TableMetaResolver extends JdbcTableMetaResolver {
    private H2TableMetaResolver(SqlSessionFactory sqlSessionFactory, DataStreamConfig dataStreamConfig) {
        super(sqlSessionFactory, dataStreamConfig); // 调用父类的构造器
    }

    @Override
    public Boolean chooseDS(final Integer dataSourceType) {
        return dataSourceType.equals(DataStreamConstant.DATA_SOURCE_TYPE_H2);
    }

    public List<TableInfoEntity> getTableInfo(Integer dataSourceType, String schemaName) throws DataStreamException {
        //todo
        return new ArrayList<>();
    }
}

