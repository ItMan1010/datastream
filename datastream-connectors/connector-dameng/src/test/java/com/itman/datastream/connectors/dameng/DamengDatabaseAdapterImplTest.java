package com.itman.datastream.connectors.dameng;

import com.itman.datastream.common.constant.DataBaseEnum;
import com.itman.datastream.common.constant.DataStreamConstant;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 达梦连接器契约测试（chooseDS / 数据类型 / 主键与外键方言分支）。
 */
public class DamengDatabaseAdapterImplTest {

    private final DamengDatabaseAdapterImpl adapter = new DamengDatabaseAdapterImpl();

    @Test
    public void chooseDS_matchesDamengTypeOnly() {
        assertThat(adapter.chooseDS(DataStreamConstant.DATA_SOURCE_TYPE_DAMENG)).isTrue();
        assertThat(adapter.chooseDS(DataStreamConstant.DATA_SOURCE_TYPE_MYSQL)).isFalse();
        assertThat(adapter.chooseDS(DataStreamConstant.DATA_SOURCE_TYPE_ORACLE)).isFalse();
    }

    @Test
    public void getDataBaseType_isDameng() {
        assertThat(adapter.getDataBaseType()).isEqualTo(DataBaseEnum.DAMENG);
    }

    @Test
    public void makeSqlKeyColumn_usesExplicitConstraintName() {
        // 达梦/Oracle 主键约束必须显式命名
        assertThat(adapter.makeSqlKeyColumn("t_user", null, "id"))
                .isEqualTo("alter table t_user add constraint pk_t_user primary key (id)");
    }

    @Test
    public void makeSqlForeignKey_omitsOnUpdate() {
        // 达梦/Oracle 不支持 ON UPDATE
        String sql = adapter.makeSqlForeignKey("t_child", "fk_child", "pid", "t_parent", "id", "CASCADE", "CASCADE");
        assertThat(sql).isEqualTo("alter table t_child add constraint fk_child foreign key (pid) references t_parent(id) on delete CASCADE");
    }
}