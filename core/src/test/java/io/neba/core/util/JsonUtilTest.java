package io.neba.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;


import static io.neba.core.util.JsonUtil.toJson;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf OTto
 */
public class JsonUtilTest {
    @Test(expected = IllegalArgumentException.class)
    public void testNullCollectionIsNotTolerated() throws Exception {
        toJson((Collection<?>) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullMapIsNotTolerated() throws Exception {
        toJson((Map) null);
    }

    @Test
    public void testJsonGeneration() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("first", null);
        map.put("second", 1);
        map.put("second", true);

        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("first", "value \"with quotes\"");
        Collection<?> collection = asList("one", 2, false, "four", nested);
        map.put("collection", collection);

        map.put("emptyMap", new HashMap<>());
        map.put("emptyCollection", new ArrayList<>());

        assertThat(toJson(map)).isEqualTo(
                "{" +
                        "\"first\":\"\"," +
                        "\"second\":true," +
                        "\"collection\":[" +
                            "\"one\"," +
                            "2," +
                            "false," +
                            "\"four\"," +
                            "{" +
                                "\"first\":\"value \\\"with quotes\\\"\"" +
                            "}" +
                        "]," +
                        "\"emptyMap\":{}," +
                        "\"emptyCollection\":[]" +
                "}");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfUnknownValue() throws Exception {
        List<Long[]> l = new ArrayList<>();
        l.add(new Long[]{1L, 2L, 3L});
        toJson(l);
    }
}