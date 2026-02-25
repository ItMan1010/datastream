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
package com.itman.datastream.connectors.oracle.dao;

import com.itman.datastream.common.entity.OracleTableColumnsEntity;
import com.itman.datastream.common.entity.TableInfoEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OracleTableMetaDao {

    @Select("<script> select column_name, data_type, data_length\n" +
            "        from all_tab_columns\n" +
            "        where table_name = #{tableName}\n" +
            "          and owner = #{tableSchema}\n" +
            "        order by column_id </script>")
    List<OracleTableColumnsEntity> getOracleTableColumns(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    @Select("<script> select acc.column_name\n" +
            "        from all_cons_columns acc\n" +
            "                 join all_constraints ac\n" +
            "                      on acc.owner = ac.owner\n" +
            "                          and acc.constraint_name = ac.constraint_name\n" +
            "        where ac.table_name = #{tableName}\n" +
            "          and acc.owner = #{tableSchema}\n" +
            "          and ac.constraint_type = 'P'\n" +
            "          and ac.owner = #{tableSchema} </script>")
    List<String> getOraclePrimaryKeys(String tableSchema, String tableName);


    @Select("<script>    select\n" +
            "            t.table_name as tableName,\n" +
            "            'TABLE' as tableType,\n" +
            "            c.comments as tableComment,\n" +
            "            t.owner as schemaName,\n" +
            "            0 as rowCount,\n" +
            "            round(s.bytes / 1024 / 1024, 2) as tableSize\n" +
            "        from all_tables t\n" +
            "        left join all_tab_comments c\n" +
            "          on t.owner = c.owner\n" +
            "          and t.table_name = c.table_name\n" +
            "        left join user_segments s\n" +
            "          on t.table_name = s.segment_name\n" +
            "        where t.owner = upper(#{tableSchema})\n" +
            "        order by t.table_name </script>")
    List<TableInfoEntity> getOracleTableInfo(String tableSchema);
}
