package com.itman.datastream.connectors.dameng;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeNoException;

/**
 * 达梦表元数据集成测试：直接验证 DamengTableMetaDao 所用 Oracle 兼容视图 SQL
 * 在真实 DM8 上能正确返回 NUMBER 精度/小数位、主键与表列表。
 */
public class DamengMetadataIntegrationTest {

    private static Connection connection;

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
    public void allTabColumns_reportsNumberPrecisionAndScale() throws Exception {
        String sql = "select c.column_name, c.data_type, c.data_length, c.data_precision, c.data_scale "
                + "from all_tab_columns c where c.table_name = 'DM_TEST_TABLE' and c.owner = 'SYSDBA' order by c.column_id";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, String> types = new HashMap<>();
            Map<String, String> precision = new HashMap<>();
            Map<String, String> scale = new HashMap<>();
            while (rs.next()) {
                String name = rs.getString("column_name").toLowerCase();
                types.put(name, rs.getString("data_type").toLowerCase());
                if (rs.getString("data_precision") != null) {
                    precision.put(name, rs.getString("data_precision"));
                }
                if (rs.getString("data_scale") != null) {
                    scale.put(name, rs.getString("data_scale"));
                }
            }

            assertThat(types.get("decimal_num")).isEqualTo("number");
            assertThat(precision.get("decimal_num")).isEqualTo("10");
            assertThat(scale.get("decimal_num")).isEqualTo("2");
            assertThat(types.get("high_prec_num")).isEqualTo("number");
            assertThat(precision.get("high_prec_num")).isEqualTo("38");
            assertThat(scale.get("high_prec_num")).isEqualTo("10");
            assertThat(types.get("varchar_col")).isEqualTo("varchar2");
            assertThat(types.get("clob_col")).isEqualTo("clob");
            assertThat(types.get("blob_col")).isEqualTo("blob");
        }
    }

    @Test
    public void primaryKeyQuery_returnsIdColumn() throws Exception {
        String sql = "select acc.column_name from all_cons_columns acc "
                + "join all_constraints ac on acc.owner = ac.owner and acc.constraint_name = ac.constraint_name "
                + "where ac.table_name = 'DM_TEST_TABLE' and acc.owner = 'SYSDBA' and ac.constraint_type = 'P' and ac.owner = 'SYSDBA'";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> pk = new ArrayList<>();
            while (rs.next()) {
                pk.add(rs.getString("column_name").toLowerCase());
            }
            assertThat(pk).containsExactly("id");
        }
    }

    @Test
    public void tableInfoQuery_returnsDmTestTable() throws Exception {
        String sql = "select t.table_name as tableName from all_tables t where t.owner = upper('SYSDBA') order by t.table_name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString("tableName").toLowerCase());
            }
            assertThat(tables).contains("dm_test_table");
        }
    }
}