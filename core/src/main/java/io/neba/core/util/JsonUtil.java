/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.core.util;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


import static org.apache.commons.lang3.ClassUtils.wrapperToPrimitive;

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
