package com.itman.datastream.connectors.dameng;

import com.itman.datastream.common.constant.DataBaseEnum;
import org.junit.Test;

import static com.itman.datastream.common.constant.DataStreamConstant.LOAD_STRATEGY_BY_LIMIT_PAG;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 达梦方言 SQL 生成 golden master 快照（锁定方言输出，变化需显式更新断言=批准）。
 */
public class DamengSqlDialectGoldenMasterTest {

    private final DamengDatabaseAdapterImpl adapter = new DamengDatabaseAdapterImpl();

    @Test
    public void makeSqlLimit_usesDamengOffsetCountSyntax() {
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
    public void stringToDate_usesToDate() {
        assertThat(adapter.stringToDate("20240101120000"))
                .isEqualTo("to_date('20240101120000','yyyymmddhh24miss')");
    }

    @Test
    public void dateToString_usesToChar() {
        assertThat(adapter.dateToString("date_col"))
                .isEqualTo("to_char(date_col,'yyyymmddhh24miss') date_col");
    }

    @Test
    public void makeSqlSystemDate_usesSysdate() {
        assertThat(adapter.makeSqlSystemDate()).isEqualTo("sysdate");
    }

    @Test
    public void makeSqlIfNull_usesNvl() {
        assertThat(adapter.makeSqlIfNull()).isEqualTo("nvl");
    }

    @Test
    public void makeSqlSequence_usesNextvalFromDual() {
        assertThat(adapter.makeSqlSequence("seq_x"))
                .isEqualTo("select seq_x.nextval from dual");
    }

    @Test
    public void makeSqlValidationQuery_usesSelectOneFromDual() {
        assertThat(adapter.makeSqlValidationQuery()).isEqualTo("select 1 from dual");
    }

    @Test
    public void makeSqlMinMax_usesQuotedAliases() {
        assertThat(adapter.makeSqlMinMax("id", "t_user"))
                .isEqualTo("select max(id) as \"max_value\", min(id) as \"min_value\" from t_user");
    }

    @Test
    public void makeSqlComment_usesCommentOnColumn() {
        assertThat(adapter.makeSqlComment("t_user", "name", "测试"))
                .isEqualTo("comment on column t_user.name is '测试';\n");
    }

    @Test
    public void getDriverClass_isDmDriver() {
        assertThat(adapter.getDriverClass()).isEqualTo("dm.jdbc.driver.DmDriver");
    }
}