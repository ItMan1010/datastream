package com.itman.datastream.connectors.dameng;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_LIMIT_PAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;

/**
 * 达梦方言集成测试（连接本机真实 DM8 容器 localhost:5236）。
 * 达梦不可达时自动跳过（assumeNoException），不影响常规构建。
 */
public class DamengDialectIntegrationTest {

    private static Connection connection;
    private final DamengDatabaseAdapterImpl adapter = new DamengDatabaseAdapterImpl();

    @BeforeClass
    public static void connect() {
        try {
            Class.forName("dm.jdbc.driver.DmDriver");
            connection = DriverManager.getConnection("jdbc:dm://localhost:5236", "SYSDBA", "SYSDBA");
        } catch (Throwable e) {
            assumeNoException("达梦 DM8 不可达，跳过集成测试", e);
        }
    }

    @AfterClass
    public static void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    public void pagingByLimit_coversAllRowsOnRealDameng() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("drop table if exists t_user_dm");
            stmt.execute("create table t_user_dm(id int primary key, name varchar2(50))");
            for (int i = 1; i <= 10; i++) {
                stmt.execute("insert into t_user_dm values(" + i + ", 'u" + i + "')");
            }

            List<Integer> collected = new ArrayList<>();
            int pageSize = 2;
            for (int offset = 0; offset < 10; offset += pageSize) {
                String sql = adapter.makeSqlSelectByPage(
                        LOAD_STRATEGY_BY_LIMIT_PAG,
                        "select id,name from t_user_dm", null, String.valueOf(offset), null, "id", pageSize);
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        collected.add(rs.getInt("id"));
                    }
                }
            }

            assertThat(collected).hasSize(10);
            assertThat(collected).doesNotHaveDuplicates();
            assertThat(collected).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            stmt.execute("drop table if exists t_user_dm");
        }
    }
}