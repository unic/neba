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

package io.neba.core.rendering;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.reflect.core.Reflection.method;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import io.neba.api.Constants;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class BeanRendererImplTest {
    /**
     * @author Olaf Otto
     */
    public interface TestInterface { }
    /**
     * @author Olaf Otto
     */
    public static class TestObject implements TestInterface {}
    
    @Mock
    private VelocityEngine engine;
    
    private Object object;
    private String renderedObject;
    private Template template;
    private LinkedList<BindingsValuesProvider> bindingsValuesProviers;
    private HashMap<String, Object> additionalContext;
    private Context mergedContext;

    private BeanRendererImpl testee;

    @Before
    public void prepareBeanRenderer() {
        String repositoryPath = "/junit/test/";
        this.bindingsValuesProviers = new LinkedList<BindingsValuesProvider>();
        this.testee = new BeanRendererImpl(repositoryPath, this.engine, this.bindingsValuesProviers);
        this.additionalContext = new HashMap<String, Object>();
    }

    @Test
    public void testRenderingWithoutView() throws Exception {
        withObject(new TestObject());

        renderObject();
        assertBeanRendererLooksforTemplate("/BeanRendererImplTest$TestObject.vlt");
        assertBeanRendererLooksforTemplate("/BeanRendererImplTest$TestInterface.vlt");
        assertBeanRendererLooksforTemplate("/Object.vlt");
        assertObjectWasNotRendered();
        
        renderObjectWithViewHint("junit");
        assertBeanRendererLooksforTemplate("/BeanRendererImplTest$TestObject-junit.vlt");
        assertBeanRendererLooksforTemplate("/BeanRendererImplTest$TestInterface-junit.vlt");
        assertBeanRendererLooksforTemplate("/Object-junit.vlt");
        assertObjectWasNotRendered();
    }
    
    @Test
    public void testRenderingWithFallbackView() {
        withObject(new TestObject());

        withExistingTemplate("/Object.vlt");
        renderObject();
        verifyVelocityTemplateIsMerged();

        withExistingTemplate("/Object-junit.vlt");
        renderObjectWithViewHint("junit");
        verifyVelocityTemplateIsMerged();
    }

    @Test
    public void testRenderingWithMostSpecificView() throws Exception {
        withObject(new TestObject());
        withExistingTemplate("/BeanRendererImplTest$TestObject.vlt");
        renderObject();
        verifyVelocityTemplateIsMerged();

        withExistingTemplate("/BeanRendererImplTest$TestObject-junit.vlt");
        renderObjectWithViewHint("junit");
        verifyVelocityTemplateIsMerged();
    }
    
    @Test
    public void testBindingsValuesProviderUsage() throws Exception {
        addBindingsValuesProvider(mock(BindingsValuesProvider.class));
        addBindingsValuesProvider(mock(BindingsValuesProvider.class));
        
        withExistingTemplate("/BeanRendererImplTest$TestObject.vlt");
        withObject(new TestObject());
        renderObject();
       
        verifyAllBindingsValuesProvidersAddBindings();
    }
    
    @Test
    public void testAdditionalContextElements() throws Exception {
        withObject(new TestObject());
        withExistingTemplate("/BeanRendererImplTest$TestObject.vlt");
        withAdditionalContextElements("junit", "test");
        renderObjectWithContext();
        assertThat(getFromMergedContext("junit")).isEqualTo("test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCollidingAdditionalContextElements() throws Exception {
        withObject(new TestObject());
        withExistingTemplate("/BeanRendererImplTest$TestObject.vlt");
        withAdditionalContextElements(Constants.RENDERER, "test");
        renderObjectWithContext();
    }

    private Object getFromMergedContext(final String string) {
        return method("get")
              .withReturnType(Object.class)
              .withParameterTypes(String.class)
              .in(this.mergedContext)
              .invoke(string);
    }

    private void withAdditionalContextElements(final String key, final String value) {
        this.additionalContext.put(key, value);
    }

    private void verifyAllBindingsValuesProvidersAddBindings() {
        for (BindingsValuesProvider provider : this.bindingsValuesProviers) {
            verify(provider, times(1)).addBindings(isA(VelocityBindings.class));
        }
    }

    private void addBindingsValuesProvider(BindingsValuesProvider provider) {
        this.bindingsValuesProviers.add(provider);
    }
    
    private void verifyVelocityTemplateIsMerged() {
        verify(this.template, times(1)).merge(isA(Context.class), isA(Writer.class));
    }

    private void withExistingTemplate(String viewSuffix) {
        this.template = mock(Template.class);
        when(this.engine.resourceExists(endsWith(viewSuffix))).thenReturn(true);
        when(this.engine.getTemplate(endsWith(viewSuffix))).thenReturn(template);
        doAnswer(new Answer<String>() {
            public String answer(InvocationOnMock invocation) throws Throwable {
                mergedContext = (Context) invocation.getArguments()[0];
                return null;
            }
        }).when(this.template).merge(isA(Context.class), isA(Writer.class));

    }

    private void renderObjectWithViewHint(String viewHint) {
        this.testee.render(this.object, viewHint);
    }

    private void assertBeanRendererLooksforTemplate(String viewSuffix) {
        verify(this.engine, times(1)).resourceExists(endsWith(viewSuffix));
    }

    private void assertObjectWasNotRendered() {
        assertThat(this.renderedObject).isNull();
    }

    private void renderObject() {
        this.renderedObject = this.testee.render(this.object);
    }

    private void renderObjectWithContext() {
        this.renderedObject = this.testee.render(this.object, this.additionalContext);
    }

    private void withObject(Object object) {
        this.object = object;
    }
}
