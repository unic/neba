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

import java.util.Arrays;

import static org.apache.commons.lang.StringUtils.join;

/**
 * A key based on multiple values, e.g. for use as a key
 * in a {@link java.util.HashMap}.
 * 
 * @author Olaf Otto
 */
public class Key {
    private static final int CONSTANT = 97;
    private static final int NULL_HASH_CODE = "null".hashCode();
    private int hashCode = 29;
    private final Object[] contents;

    /**
     * @param contents can be <code>null</code> and can contain <code>null</code> elements.
     */
    public Key(Object... contents) {
        this.contents = contents;
        if (contents != null && contents.length != 0) {
            for (Object content : contents) {
                hashCode = hashCode * CONSTANT + (content == null ? NULL_HASH_CODE : content.hashCode());
            }
        } else {
            hashCode =  hashCode * CONSTANT + NULL_HASH_CODE;
        }
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        return obj == this || obj != null && obj.getClass() == getClass() 
        								  && Arrays.equals(this.contents, ((Key) obj).contents);
    }

    @Override
    public String toString() {
        return "Key {" + join(this.contents, ", ") + '}';
    }
}
