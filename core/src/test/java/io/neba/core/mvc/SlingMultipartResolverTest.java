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

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author christoph.huber
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingMultipartResolverTest {
    @Mock
    private SlingHttpServletRequest request;
    
    @InjectMocks
    private SlingMultipartResolver testee;
    
    @Test
    public void testIsMultipartFalse() {
        withRequest("GET");
        assertThat(testee.isMultipart(request)).isFalse();
    } 
    
    @Test
    public void testIsMultipartTrue() {
        withRequest("POST");
        withContentType("multipart/...");
        assertThat(testee.isMultipart(request)).isTrue();
    }   

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMultipartWithNullValue() {
    	testee.resolveMultipart(null);
    }
    
    private void withRequest(String method) {
        when(request.getMethod()).thenReturn(method);
    }
    
    private void withContentType(String contentType) {
        when(request.getContentType()).thenReturn(contentType);        
    }
}
