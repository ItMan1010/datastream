package com.itman.datastream.connectors.postgres;

import com.itman.datastream.common.entity.TableColumnEntity;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试示范（对应任务 6.2，git commit 57d90b0）。
 * 缺陷：MySQL 迁移 PostgreSQL 时，二进制/几何字段以 MySQL 的 0x... 十六进制字面量写入 bytea 列报错。
 * 修复：PostgreSQL 方言改为使用 decode('...', 'hex')。
 */
public class PostgresBinaryFieldRegressionTest {

    private final PostgresDatabaseAdapterImpl adapter = new PostgresDatabaseAdapterImpl();

    @Test
    public void binaryField_usesDecodeHexNotHexLiteral() {
        List<TableColumnEntity> columns = Arrays.asList(
                TableColumnEntity.builder().columnName("bin_col").build()
        );
        Map<String, Object> row = new HashMap<>();
        row.put("bin_col", new byte[]{0x01, 0x02, 0x03});

        String insertRow = adapter.makeSqlInsertRow(1L, columns, row);

        // 必须使用 decode('...','hex')，而不是 MySQL 的 0x... 字面量
        assertThat(insertRow).isEqualTo("( decode('010203', 'hex'))");
        assertThat(insertRow).doesNotContain("0x");
    }
}
