package com.itman.datastream.connectors.dameng;

import com.itman.datastream.common.constant.DataStreamConstant;
import com.itman.datastream.common.utils.CommUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 达梦 JDBC URL 解析契约测试。
 * <p>达梦 URL（jdbc:dm://host:port）不含 database 路径，须能正确解析出 type=dm，
 * 且 parseSchemaNameJdbcUrl 不抛异常（返回 null，schema 由用户名确定）。</p>
 */
public class DamengUrlParsingTest {

    @Test
    public void parseJdbcUrl_returnsDmForDamengUrl() {
        assertThat(CommUtils.parseJdbcUrl("jdbc:dm://localhost:5236")).isEqualTo("dm");
    }

    @Test
    public void parseSchemaNameJdbcUrl_returnsNullWithoutException() {
        assertThat(CommUtils.parseSchemaNameJdbcUrl("jdbc:dm://localhost:5236")).isNull();
    }

    @Test
    public void parseHostAndPort_returnsHostPort() {
        assertThat(CommUtils.parseHostJdbcUrl("jdbc:dm://localhost:5236")).isEqualTo("localhost");
        assertThat(CommUtils.parsePortJdbcUrl("jdbc:dm://localhost:5236")).isEqualTo("5236");
    }

    @Test
    public void damengIsDataBaseDataSource() {
        assertThat(CommUtils.isDataBaseDataSource(DataStreamConstant.DATA_SOURCE_TYPE_DAMENG)).isTrue();
    }
}