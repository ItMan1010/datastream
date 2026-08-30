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
package com.itman.datastream.connectors.dameng.dao;

import com.itman.datastream.common.entity.TableInfoEntity;
import com.itman.datastream.connectors.dameng.DamengTableColumnsEntity;
import com.itman.datastream.connectors.dameng.DamengIndexEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 达梦表元数据 DAO，基于达梦提供的 Oracle 兼容系统视图。
 */
@Mapper
public interface DamengTableMetaDao {

    @Select("<script> select c.column_name, c.data_type, c.data_length, c.data_precision, c.data_scale, c.nullable, c.data_default, cc.comments from all_tab_columns c left join all_col_comments cc on cc.owner = c.owner and cc.table_name = c.table_name and cc.column_name = c.column_name where c.table_name = #{tableName} and c.owner = #{tableSchema} order by c.column_id </script>")
    List<DamengTableColumnsEntity> getDamengTableColumns(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    @Select("<script> select acc.column_name from all_cons_columns acc join all_constraints ac on acc.owner = ac.owner and acc.constraint_name = ac.constraint_name where ac.table_name = #{tableName} and acc.owner = #{tableSchema} and ac.constraint_type = 'P' and ac.owner = #{tableSchema} </script>")
    List<String> getDamengPrimaryKeys(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    @Select("<script> select i.index_name, c.column_name, i.uniqueness from all_indexes i join all_ind_columns c on i.index_name = c.index_name and i.table_owner = c.table_owner where i.table_name = #{tableName} and i.table_owner = #{tableSchema} order by i.index_name, c.column_position </script>")
    List<DamengIndexEntity> getDamengIndexes(@Param("tableSchema") String tableSchema, @Param("tableName") String tableName);

    @Select("<script> select t.table_name as tableName, 'TABLE' as tableType, c.comments as tableComment, t.owner as schemaName, 0 as rowCount, round(s.bytes / 1024 / 1024, 2) as tableSize from all_tables t left join all_tab_comments c on t.owner = c.owner and t.table_name = c.table_name left join user_segments s on t.table_name = s.segment_name where t.owner = upper(#{tableSchema}) order by t.table_name </script>")
    List<TableInfoEntity> getDamengTableInfo(@Param("tableSchema") String tableSchema);
}