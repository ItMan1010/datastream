package com.itman.datastream.admin.support;

import com.itman.datastream.common.entity.TableColumnEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.itman.datastream.common.constant.DataStreamConstant.SQL_FORMAT_HINT_DATANODE;
import static com.itman.datastream.common.constant.DataStreamConstant.TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DataMoveSqlSupport 纯逻辑契约测试（对应字段映射保序/取值契约）。
 */
public class DataMoveSqlSupportTest {

    @Test
    public void makeDataSelectCountSql_withoutConditionAndHint() {
        String sql = DataMoveSqlSupport.makeDataSelectCountSql("user_table", null, null, null);
        assertThat(sql).isEqualTo("select count(1) from user_table");
    }

    @Test
    public void makeDataSelectCountSql_withCondition() {
        String sql = DataMoveSqlSupport.makeDataSelectCountSql("user_table", "id > 1", null, null);
        assertThat(sql).isEqualTo("select count(1) from user_table where id > 1");
    }

    @Test
    public void makeDataSelectCountSql_withDataNodeHint() {
        String sql = DataMoveSqlSupport.makeDataSelectCountSql("user_table", null, "dn1", null);
        assertThat(sql).isEqualTo(String.format(SQL_FORMAT_HINT_DATANODE, "dn1") + "select count(1) from user_table");
    }

    @Test
    public void makeInsertRowObject_preservesColumnOrderAndValues() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName("id").build(),
                TableColumnEntity.builder().columnName("name").build(),
                TableColumnEntity.builder().columnName("age").build()
        );

        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("name", "zhangsan");

        List<Object> result = DataMoveSqlSupport.makeInsertRowObject(100L, columns, row);

        // 保序 + 保真：id、name 按列顺序取值，age 缺失为 null
        assertThat(result).containsExactly(1, "zhangsan", null);
    }

    @Test
    public void makeInsertRowObject_fillsTaskIdForExtensionColumn() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName(TARGET_TABLE_ADD_COLUMNS_MOVE_TASK_ID).build()
        );

        Map<String, Object> row = new HashMap<>();

        List<Object> result = DataMoveSqlSupport.makeInsertRowObject(999L, columns, row);

        assertThat(result).containsExactly(999L);
    }
}
