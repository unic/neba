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

package io.neba.api.tags.tags;

import io.neba.api.rendering.BeanRenderer;
import io.neba.api.rendering.BeanRendererFactory;
import io.neba.api.tags.RenderTag;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.fest.assertions.MapAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Map;

import static org.apache.sling.api.scripting.SlingBindings.SLING;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.MapAssert.entry;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class RenderTagTest {
    private static final String NO_NAMESPACE = null;

    @Mock
    private PageContext pageContext;
    @Mock
    private JspWriter writer;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private SlingBindings bindings;
    @Mock
    private SlingScriptHelper helper;
    @Mock
    private BeanRendererFactory factory;
    @Mock
    private BeanRenderer renderer;

    private Map<String, Object> context;
    private Object object;
    private String viewHint;
    private String renderedObject;

    @InjectMocks
    private RenderTag testee;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        when(this.pageContext.getRequest()).thenReturn(this.request);
        when(this.pageContext.getOut()).thenReturn(this.writer);
        when(this.request.getAttribute(eq(SlingBindings.class.getName()))).thenReturn(this.bindings);
        when(this.bindings.get(eq(SLING))).thenReturn(this.helper);
        when(this.helper.getService(eq(BeanRendererFactory.class))).thenReturn(this.factory);
        when(this.factory.get(anyString())).thenReturn(this.renderer);
        doAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                context = (Map<String, Object>) invocation.getArguments()[2];
                return null;
            }
        }).when(this.renderer).render(anyObject(), anyString(), isA(Map.class));
    }

    @Test
    public void testNullObject() throws Exception {
        withDynamicAttribute("test", "test");
        doEndTag();
        verifyRendererIsNotInvokedWithCustomContext();
    }
    
    @Test
    public void testBeanRendererIsCalledWithDynamicAttributes() throws Exception {
        withObject(new Object());
        withDynamicAttribute("test", "test");
        doEndTag();
        verifyRendererIsInvokedWithCustomContext();
        assertContextIncludes("test", "test");
    }

    @Test
    public void testOutputIntoPage() throws Exception {
        withObject(new Object());
        withRenderedObject("Rendered object");
        doEndTag();

        verifyRenderedObjectIsWrittenToJspWriter();
    }

    @Test
    public void testOutputIntoVariable() throws Exception {
        withObject(new Object());
        withRenderedObject("Rendered object");
        withVariableName("variableName");

        doEndTag();

        verifyRenderedObjectIsNotWriitenToJspWriter();
        verifyRenderedObjectIsStoredAsVariableWithName("variableName");
    }

    @Test
    public void testViewHintSpecification() throws Exception {
        withObject(new Object());
        withRenderedObject("Rendered object");
        withViewHint("hint");

        doEndTag();

        verifyViewHintIsPassedToBeanRenderer();
        verifyRenderedObjectIsWrittenToJspWriter();
    }

    private void withViewHint(String hint) {
        this.viewHint = hint;
        this.testee.setViewHint(hint);
    }

    private void verifyRenderedObjectIsNotWriitenToJspWriter() throws IOException {
        verify(this.writer, never()).write(anyString());
    }

    private void verifyRenderedObjectIsWrittenToJspWriter() throws IOException {
        verify(this.writer).write(eq(this.renderedObject));
    }

    @SuppressWarnings("unchecked")
    private void withRenderedObject(String renderedObject) {
        this.renderedObject = renderedObject;
        doReturn(renderedObject).when(this.renderer).render(same(this.object), anyString(), isA(Map.class));
    }

    @SuppressWarnings("unchecked")
    private void verifyViewHintIsPassedToBeanRenderer() {
        verify(this.renderer).render(eq(this.object), eq(this.viewHint), isA(Map.class));
    }

    private void verifyRenderedObjectIsStoredAsVariableWithName(String variableName) {
        verify(this.pageContext).setAttribute(eq(variableName), eq(this.renderedObject));
    }

    private void withVariableName(String variableName) {
        this.testee.setVar(variableName);
    }

    private void withObject(Object object) {
        this.object = object;
        this.testee.setObject(object);
    }

    private MapAssert assertContextIncludes(String value, String key) {
        return assertThat(this.context).includes(entry(key, value));
    }

    @SuppressWarnings("unchecked")
    private String verifyRendererIsInvokedWithCustomContext() {
        return verify(this.renderer, times(1)).render(anyObject(), anyString(), isA(Map.class));
    }
    @SuppressWarnings("unchecked")
    private String verifyRendererIsNotInvokedWithCustomContext() {
        return verify(this.renderer, times(0)).render(anyObject(), anyString(), isA(Map.class));
    }

    private int doEndTag() throws JspException {
        return this.testee.doEndTag();
    }

    private void withDynamicAttribute(final String attributeName, final String value) throws JspException {
        this.testee.setDynamicAttribute(NO_NAMESPACE, attributeName, value);
    }
}
