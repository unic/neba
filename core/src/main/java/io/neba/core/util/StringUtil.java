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

/**
 * @author Olaf Otto
 */
public class StringUtil {
    /**
     * Appends the given String to the given array of Strings.
     *
     * @param append must not be <code>null</code>.
     * @param appendTo must not be <code>null</code>.
     *
     * @return a new array representing the concatenation.
     */
    public static String[] append(String append, String[] appendTo) {
        if (append == null) {
            throw new IllegalArgumentException("Method argument append must not be null.");
        }
        if (appendTo == null) {
            throw new IllegalArgumentException("Method argument to must not be null.");
        }

        String[] result = new String[appendTo.length];
        for (int i = 0; i  < appendTo.length; ++i) {
            result[i] = appendTo[i] == null ? null : appendTo[i] + append;
        }

        return result;
    }
    private StringUtil() {}
}
