package org.lambda.example;
import org.junit.jupiter.api.Test;
import org.lambda.framework.common.util.sample.JsonUtil;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
class JsonCompatibilityTest {
    record Entry(String name, LocalDateTime createdAt) {}
    @Test void recordAndDateRoundTrip() {
        var source = new Entry("测试", LocalDateTime.of(2026, 11, 7, 12, 30));
        assertEquals(source, JsonUtil.stringToObj(JsonUtil.objToString(source), Entry.class).orElseThrow());
    }
    @Test void unknownFieldsRemainCompatible() {
        assertEquals("test", JsonUtil.stringToObj("{\"name\":\"test\",\"createdAt\":null,\"extra\":true}", Entry.class).orElseThrow().name());
    }
}
