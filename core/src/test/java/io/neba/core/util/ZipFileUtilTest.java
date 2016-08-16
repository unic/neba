/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static io.neba.core.util.ZipFileUtil.toZipFileEntryName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ZipFileUtilTest {
    @Mock
    private File file;

    @Test(expected = IllegalArgumentException.class)
    public void testNullArgumentsAreNotTolerated() throws Exception {
        toZipFileEntryName(null);
    }

    @Test
    public void testNormalizedPathsAreNotChanged() throws Exception {
        withFilePath("/some/path");
        assertZipFileEntryNameIs("some/path");
    }

    @Test
    public void testNetworkIdentifiersAreRemoved() throws Exception {
        withFilePath("//share/some/path");
        assertZipFileEntryNameIs("share/some/path");
    }

    @Test
    public void testWindowsFilePathsAreRepresentedAsUnixPaths() throws Exception {
        withFilePath("D:\\share\\windows\\path");
        assertZipFileEntryNameIs("D/share/windows/path");
    }

    private void assertZipFileEntryNameIs(String expected) {
        assertThat(toZipFileEntryName(file)).isEqualTo(expected);
    }

    private void withFilePath(String path) {
        when(file.getAbsolutePath()).thenReturn(path);
    }
}