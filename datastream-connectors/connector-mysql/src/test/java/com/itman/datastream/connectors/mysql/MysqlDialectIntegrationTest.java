package com.itman.datastream.connectors.mysql;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

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
 * MySQL 方言集成测试（Testcontainers 真实 MySQL）。
 * Docker 不可用或镜像拉取失败时自动跳过（降级为仅 H2 契约测试）。
 */
public class MysqlDialectIntegrationTest {

    private static MySQLContainer<?> mysql;
    private final MysqlDatabaseAdapterImpl adapter = new MysqlDatabaseAdapterImpl();

    @BeforeClass
    public static void startContainer() {
        try {
            mysql = new MySQLContainer<>("mysql:8.0");
            mysql.start();
        } catch (Throwable e) {
            assumeNoException("Docker/MySQL unavailable, skipping integration test", e);
        }
    }

    @AfterClass
    public static void stopContainer() {
        if (mysql != null) {
            mysql.stop();
        }
    }

    @Test
    public void pagingByLimit_coversAllRowsOnRealMysql() throws Exception {
        try (Connection conn = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("drop table if exists t_user");
            stmt.execute("create table t_user(id int primary key, name varchar(50))");
            for (int i = 1; i <= 10; i++) {
                stmt.execute("insert into t_user values(" + i + ", 'u" + i + "')");
            }

            List<Integer> collected = new ArrayList<>();
            int pageSize = 2;
            for (int offset = 0; offset < 10; offset += pageSize) {
                String sql = adapter.makeSqlSelectByPage(
                        LOAD_STRATEGY_BY_LIMIT_PAG,
                        "select id,name from t_user", null, String.valueOf(offset), null, "id", pageSize);
                try (ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        collected.add(rs.getInt("id"));
                    }
                }
            }

            assertThat(collected).hasSize(10);
            assertThat(collected).doesNotHaveDuplicates();
            assertThat(collected).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }
    }
}
