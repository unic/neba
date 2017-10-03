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

import org.junit.Test;


import static io.neba.core.util.StringUtil.append;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class StringUtilTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullValueForAppend() throws Exception {
        append(null, new String[]{});
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullValueForAppendTo() throws Exception {
        append("", null);
    }

    @Test
    public void testNullValuesInAppendToRemainNull() throws Exception {
        assertThat(append("", new String[]{null, null}))
                   .containsOnly(null, null);
    }

    @Test
    public void testAppending() throws Exception {
        assertThat(append("/test", new String[]{"/one", "", "/two"}))
                .isEqualTo(new String[]{"/one/test", "/test", "/two/test"});

    }
}