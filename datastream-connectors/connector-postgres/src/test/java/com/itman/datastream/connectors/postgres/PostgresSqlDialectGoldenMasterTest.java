package com.itman.datastream.connectors.postgres;

import org.junit.Test;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_LIMIT_PAG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL 方言 SQL 生成 golden master 快照（锁定方言输出，变化需显式更新断言=批准）。
 */
public class PostgresSqlDialectGoldenMasterTest {

    private final PostgresDatabaseAdapterImpl adapter = new PostgresDatabaseAdapterImpl();

    @Test
    public void makeSqlLimit_usesPostgresLimitOffsetSyntax() {
        assertThat(adapter.makeSqlLimit(4, 2)).isEqualTo("limit 2 offset 4");
    }

    @Test
    public void makeSqlSelectByPage_limitDialect() {
        String sql = adapter.makeSqlSelectByPage(
                LOAD_STRATEGY_BY_LIMIT_PAG,
                "select id,name from t_user", null, "4", null, "id", 2);
        assertThat(sql).isEqualTo("select id,name from t_user  limit 2 offset 4");
    }

    @Test
    public void makeSqlIfNull_usesCoalesce() {
        assertThat(adapter.makeSqlIfNull()).isEqualTo("coalesce");
    }

    @Test
    public void makeSqlSystemDate_usesNow() {
        assertThat(adapter.makeSqlSystemDate()).isEqualTo("now()");
    }

    @Test
    public void makeSqlIntervalDay_usesPostgresInterval() {
        assertThat(adapter.makeSqlIntervalDay(3)).isEqualTo("interval '3 day'");
    }

    @Test
    public void makeSqlComment_usesCommentOnColumn() {
        assertThat(adapter.makeSqlComment("t_user", "name", "用户名"))
                .isEqualTo("comment on column t_user.name is '用户名';\n");
    }
}
