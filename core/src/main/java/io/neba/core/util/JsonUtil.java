package io.neba.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;

/**
 * A lightweight utility for converting Maps and Collections to JSON.
 *
 * @author Olaf OTto
 */
public class JsonUtil {
    /**
     * @param collection must not be <code>null</code>.
     */
    public static String toJson(Collection<?> collection) {
        if (collection == null) {
            throw new IllegalArgumentException("Method parameter collection must not be null");
        }

        return collection.stream()
                .map(JsonUtil::toJson)
                .reduce((l, r) -> l + "," + r)
                .map(s -> '[' + s + ']')
                .orElse("[]");
    }

    /**
     * @param map must not be <code>null</code>.
     */
    public static String toJson(Map<?, ?> map) {
        if (map == null) {
            throw new IllegalArgumentException("Method parameter map must not be null");
        }

        Set<? extends Map.Entry<?, ?>> entries = map.entrySet();
        return entries
                .stream()
                .map(e -> toJson(e.getKey()) + ':' + toJson(e.getValue()))
                .reduce((l, r) -> l + ',' + r)
                .map(s -> '{' + s + '}')
                .orElse("{}");
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "\"\"";
        }

        if (!value.getClass().isArray() && (value.getClass().isPrimitive() || wrapperToPrimitive(value.getClass()) != null)) {
            return Objects.toString(value);
        }
        if (value instanceof String) {
            return '"' + ((String) value).replaceAll("\"", "\\\\\"") + '"';
        }
        if (value instanceof Collection) {
            return toJson((Collection) value);
        }
        if (value instanceof Map) {
            return toJson((Map<?, ?>) value);
        }
        throw new IllegalArgumentException("Cannot convert value " + value + " to JSON.");
    }

    private JsonUtil() {
    }
}
