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

package io.neba.core.mvc;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.request.RequestParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingMultipartFileTest {
    @Mock
    private RequestParameter requestParameter;
    
    private String paramName = "test";
    private ByteArrayInputStream inputStream;
    private File destinationFile;
    private String multipartContent;

    private SlingMultipartFile testee;

    @Before
    public void prepareMultipartFile() {
        this.testee = new SlingMultipartFile(this.paramName, this.requestParameter);
    }

    @After
    public void removeTempFile() {
        FileUtils.deleteQuietly(this.destinationFile);
    }
    
    @Test
    public void testTransferToFile() throws Exception {
        withMultipartFileContents("Junit Test");
        withDestinationFile(File.createTempFile("junt-test", ".tmp"));
        transferMultipartFileToDestinationFile();
        assertDestinationFileContentIsMultiPartContent();
    }
    
    @Test
    public void testTransferToExistingFile() throws Exception {
        withMultipartFileContents("Junit Test");
        withDestinationFile(File.createTempFile("junt-test", ".tmp"));
        createDestinationFile();
        transferMultipartFileToDestinationFile();
        assertDestinationFileContentIsMultiPartContent();
    }

    private void createDestinationFile() throws IOException {
        this.destinationFile.createNewFile();
    }

    private void assertDestinationFileContentIsMultiPartContent() throws IOException {
        String destinationFileContent = IOUtils.toString(new FileReader(this.destinationFile));
        assertThat(destinationFileContent, is(this.multipartContent));
    }

    private void transferMultipartFileToDestinationFile() throws IOException {
        this.testee.transferTo(this.destinationFile);
    }

    private void withDestinationFile(final File destinationFile) {
        this.destinationFile = destinationFile;
    }

    private void withMultipartFileContents(final String string) throws IOException {
        this.multipartContent = string;
        this.inputStream = new ByteArrayInputStream(string.getBytes());
        when(this.requestParameter.getInputStream()).thenReturn(this.inputStream);
    }
}
