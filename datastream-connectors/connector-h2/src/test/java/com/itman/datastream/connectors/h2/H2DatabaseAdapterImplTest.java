package com.itman.datastream.connectors.h2;

import com.itman.datastream.common.entity.TableColumnEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对 H2 方言 SQL 生成的契约测试（smoke）。
 * 验证 makeSqlSelectColumns / makeSqlInsertRow 等纯 SQL 生成方法的输出，
 * 作为后续 golden master 快照测试的基础。
 */
public class H2DatabaseAdapterImplTest {

    private final H2DatabaseAdapterImpl adapter = new H2DatabaseAdapterImpl();

    @Test
    public void makeSqlSelectColumns_withoutConditionAndOrderBy() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName("id").keyFlag(true).build(),
                TableColumnEntity.builder().columnName("name").keyFlag(false).build()
        );

        String sql = adapter.makeSqlSelectColumns("user_table", null, columns, false);

        assertThat(sql).isEqualTo("select id,name from user_table");
    }

    @Test
    public void makeSqlSelectColumns_withConditionAndOrderBy() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName("id").keyFlag(true).build(),
                TableColumnEntity.builder().columnName("name").keyFlag(false).build()
        );

        String sql = adapter.makeSqlSelectColumns("user_table", "id > 10", columns, true);

        assertThat(sql).isEqualTo("select id,name from user_table where id > 10 order by id");
    }

    @Test
    public void makeSqlInsertRow_formatsStringAndNull() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName("name").build(),
                TableColumnEntity.builder().columnName("age").build()
        );

        Map<String, Object> row = new HashMap<>();
        row.put("name", "O'Brien");
        // age 缺失 -> null

        String insertRow = adapter.makeSqlInsertRow(100L, columns, row);

        assertThat(insertRow).isEqualTo("( 'O''Brien',null)");
    }
}
