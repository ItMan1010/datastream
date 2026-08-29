package com.itman.datastream.connectors.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * H2 内存库测试基类：为契约测试提供轻量、可复现的数据库执行环境。
 * 使用 DB_CLOSE_DELAY=-1 让内存库在 JVM 生命周期内保持存活，供多个连接复用。
 */
public abstract class AbstractH2Test {

    protected static final String H2_URL = "jdbc:h2:mem:datastream_test;DB_CLOSE_DELAY=-1";

    protected Connection newConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, "sa", "");
    }

    protected void execute(Connection conn, String sql) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    protected void dropTableIfExists(Connection conn, String tableName) throws SQLException {
        execute(conn, "drop table if exists " + tableName.toLowerCase());
    }

    protected long countRows(Connection conn, String tableName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("select count(1) from " + tableName.toLowerCase())) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
