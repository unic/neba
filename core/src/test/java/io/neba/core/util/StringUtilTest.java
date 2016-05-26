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