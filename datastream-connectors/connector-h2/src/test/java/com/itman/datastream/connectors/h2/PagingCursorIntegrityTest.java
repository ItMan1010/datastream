package com.itman.datastream.connectors.h2;

import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_DATA_PART;
import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_LIMIT_PAG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分页/分段游标「不重不漏」契约测试（对应 engine-data-integrity 3.2）。
 * 使用 H2 内存库真实执行 SQL，验证 offset 分页与主键游标分段两种策略均完整覆盖源数据。
 */
public class PagingCursorIntegrityTest extends AbstractH2Test {

    private final H2DatabaseAdapterImpl adapter = new H2DatabaseAdapterImpl();

    private Connection prepareSourceTable() throws Exception {
        Connection conn = newConnection();
        execute(conn, "drop table if exists t_user");
        execute(conn, "create table t_user(id int primary key, name varchar(50))");
        for (int i = 1; i <= 10; i++) {
            execute(conn, "insert into t_user(id, name) values(" + i + ", 'u" + i + "')");
        }
        return conn;
    }

    private List<Integer> queryIds(Connection conn, String sql) throws Exception {
        List<Integer> ids = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        }
        return ids;
    }

    @Test
    public void pagingByLimit_coversAllRowsWithoutDupOrMiss() throws Exception {
        try (Connection conn = prepareSourceTable()) {
            List<Integer> collected = new ArrayList<>();
            int pageSize = 2;
            for (int offset = 0; offset < 10; offset += pageSize) {
                String sql = adapter.makeSqlSelectByPage(
                        LOAD_STRATEGY_BY_LIMIT_PAG,
                        "select id,name from t_user", null, String.valueOf(offset), null, "id", pageSize);
                collected.addAll(queryIds(conn, sql));
            }

            assertThat(collected).hasSize(10);
            assertThat(collected).doesNotHaveDuplicates();
            assertThat(collected).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }

    @Test
    public void pagingByDataPart_coversAllRowsWithoutDupOrMiss() throws Exception {
        try (Connection conn = prepareSourceTable()) {
            List<Integer> collected = new ArrayList<>();
            int pageSize = 2;
            String beginKey = "1";
            String tableMinKeyValue = "1";

            while (true) {
                String sql = adapter.makeSqlSelectByPage(
                        LOAD_STRATEGY_BY_DATA_PART,
                        "select id,name from t_user", null, beginKey, tableMinKeyValue, "id", pageSize);
                List<Integer> pageIds = queryIds(conn, sql);
                if (pageIds.isEmpty()) {
                    break;
                }
                collected.addAll(pageIds);
                beginKey = String.valueOf(pageIds.get(pageIds.size() - 1));
                if (pageIds.size() < pageSize) {
                    break;
                }
            }

            assertThat(collected).hasSize(10);
            assertThat(collected).doesNotHaveDuplicates();
            assertThat(collected).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }
}
