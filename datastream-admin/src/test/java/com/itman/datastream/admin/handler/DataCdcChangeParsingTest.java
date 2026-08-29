package com.itman.datastream.admin.handler;

import com.itman.datastream.common.entity.ChangeDataEntity;
import com.itman.datastream.common.entity.DebeziumFiledEntity;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CDC 变更事件解析契约测试（对应 engine-data-integrity 3.5 增量数据正确性）。
 * 锁定 Debezium op（r/c/u/d）到 DataStream 处理类型的映射，以及 after/before/source 解析。
 */
public class DataCdcChangeParsingTest {

    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    public void getHandleType_mapsDebeziumOpToHandleType() {
        assertThat(DataCdcHandler.getHandleType(map("op", "r"))).isEqualTo("read");
        assertThat(DataCdcHandler.getHandleType(map("op", "c"))).isEqualTo("insert");
        assertThat(DataCdcHandler.getHandleType(map("op", "u"))).isEqualTo("update");
        assertThat(DataCdcHandler.getHandleType(map("op", "d"))).isEqualTo("delete");
        assertThat(DataCdcHandler.getHandleType(map("op", "x"))).isEqualTo("none");
        assertThat(DataCdcHandler.getHandleType(new HashMap<>())).isEqualTo("none");
    }

    @Test
    public void getChangeData_parsesAfterBeforeSource() {
        Map<String, Object> after = map("id", 1, "name", "x");
        Map<String, Object> before = map("id", 1, "name", "y");
        Map<String, Object> source = map("ts_ms", 123L);

        Map<String, Object> payload = new HashMap<>();
        payload.put("after", after);
        payload.put("before", before);
        payload.put("source", source);

        ChangeDataEntity change = DataCdcHandler.getChangeData(payload);

        assertThat(change.getAfter()).isEqualTo(after);
        assertThat(change.getBefore()).isEqualTo(before);
        assertThat(change.getSource()).isEqualTo(source);
    }

    @Test
    public void changeAfterFields_parsesAfterFieldDefinitions() {
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> beforeField = map("field", "before", "fields", new ArrayList<Map<String, Object>>());
        Map<String, Object> idField = map("field", "id", "optional", false, "type", "int32", "name", "id");
        Map<String, Object> nameField = map("field", "name", "optional", true, "type", "string", "name", "name");
        List<Map<String, Object>> afterFields = new ArrayList<>();
        afterFields.add(idField);
        afterFields.add(nameField);
        Map<String, Object> afterField = map("field", "after", "fields", afterFields);
        fields.add(beforeField);
        fields.add(afterField);

        Map<String, DebeziumFiledEntity> result = DataCdcHandler.changeAfterFields(map("fields", fields));

        assertThat(result).containsKeys("id", "name");
        assertThat(result.get("id").getType()).isEqualTo("int32");
        assertThat(result.get("name").getOptional()).isTrue();
    }
}
