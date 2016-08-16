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
package io.neba.core.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletContext;
import java.lang.reflect.Method;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.join;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class WebApplicationContextAdapterTest {
    @Mock
    private ServletContext servletContext;

    private InvocationOnMock lastInvocationOnWrappedContext;

    private WebApplicationContextAdapter testee;

    @Before
    public void setUp() throws Exception {
        ApplicationContext applicationContext = mock(ApplicationContext.class, new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                lastInvocationOnWrappedContext = invocation;
                return null;
            }
        });

        this.testee = new WebApplicationContextAdapter(applicationContext, this.servletContext);
    }

    @Test
    public void testAllWrappedMethodsDelegateToTheirWrappedEquivalents() throws Exception {
        for (Method method : ApplicationContext.class.getMethods()) {
            method.invoke(this.testee, mockArgs(method));
            if (lastInvocationOnWrappedContext == null ||
                    !signatureOf(lastInvocationOnWrappedContext.getMethod()).equals(signatureOf(method))) {
                fail("The adapter should have delegated the method '" + signatureOf(method) + "' to the wrapped instance, but did not.");
            }
        }
    }

    @Test
    public void testServletContextRetrieval() throws Exception {
        assertThat(this.testee.getServletContext()).isSameAs(this.servletContext);
    }

    private String signatureOf(Method m) {
        return  m.getName() + "(" + join(m.getParameterTypes(), ", ") + ")";
    }

    private Object[] mockArgs(Method method) {
        return stream(method.getParameterTypes()).map(type -> {
                    if (!type.isPrimitive()) {
                        return null;
                    }
                    if (type == boolean.class) {
                        return false;
                    }
                    throw new AssertionError("Unable to mock type " + type + ".");
                }).toArray();
    }
}