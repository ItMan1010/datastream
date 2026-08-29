package com.itman.datastream.connectors.mysql;

import org.junit.Test;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_LIMIT_PAG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL 方言 SQL 生成 golden master 快照（锁定方言输出，变化需显式更新断言=批准）。
 */
public class MysqlSqlDialectGoldenMasterTest {

    private final MysqlDatabaseAdapterImpl adapter = new MysqlDatabaseAdapterImpl();

    @Test
    public void makeSqlLimit_usesMysqlOffsetCountSyntax() {
        assertThat(adapter.makeSqlLimit(4, 2)).isEqualTo("limit 4,2");
    }

    @Test
    public void makeSqlSelectByPage_limitDialect() {
        String sql = adapter.makeSqlSelectByPage(
                LOAD_STRATEGY_BY_LIMIT_PAG,
                "select id,name from t_user", null, "4", null, "id", 2);
        assertThat(sql).isEqualTo("select id,name from t_user  limit 4,2");
    }

    @Test
    public void makeSqlIfNull_usesMysqlFunction() {
        assertThat(adapter.makeSqlIfNull()).isEqualTo("ifnull");
    }

    @Test
    public void makeSqlSystemDate_usesSysdate() {
        assertThat(adapter.makeSqlSystemDate()).isEqualTo("sysdate()");
    }

    @Test
    public void makeSqlIntervalDay_usesMysqlInterval() {
        assertThat(adapter.makeSqlIntervalDay(3)).isEqualTo("interval 3 day");
    }

    @Test
    public void stringToDate_usesStrToDate() {
        assertThat(adapter.stringToDate("20240101120000"))
                .isEqualTo("str_to_date('20240101120000','%Y%m%d%H%i%s%f')");
    }
}
