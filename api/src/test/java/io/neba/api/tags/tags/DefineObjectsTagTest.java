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

package io.neba.api.tags.tags;

import io.neba.api.Constants;
import io.neba.api.services.ResourceModelResolver;
import io.neba.api.tags.DefineObjectsTag;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.Map;

import static org.apache.sling.api.scripting.SlingBindings.SLING;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class DefineObjectsTagTest {
    @Mock
    private PageContext context;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private Resource resource;
    @Mock
    private SlingBindings bindings;
    @Mock
    private SlingScriptHelper sling;
    @Mock
    private ResourceModelResolver resourceModelResolver;

    private int tagExecutionReturnCode;
    private Object model = new Object();
    private String desiredModelName;

    @InjectMocks
    private DefineObjectsTag testee;

    @Before
    public void prepareTag() {
        doReturn(this.resource).when(this.request).getResource();
        doReturn(this.request).when(this.context).getRequest();
        doReturn(this.sling).when(this.bindings).get(eq(SLING));

        doReturn(this.resourceModelResolver).when(this.sling).getService(eq(ResourceModelResolver.class));
        doReturn(this.model).when(this.resourceModelResolver).resolveMostSpecificModel(eq(this.resource));
        doReturn(this.model).when(this.resourceModelResolver).resolveMostSpecificModelIncludingModelsForBaseTypes(eq(this.resource));
        doReturn(this.model).when(this.resourceModelResolver).resolveMostSpecificModelWithName(eq(this.resource), anyString());

        withBindings(this.bindings);
        this.testee.setPageContext(this.context);
    }

    @Test
    public void testCopyingOfBindingsIntoPageContext() throws Exception {
        withBinding("foo", "bar");
        withBinding("foo2", "bar2");

        executeTag();

        verifyAllBindingsAreCopiedIntoPageContext();
        assertTagEvaluationReturnsContinueWithPage();
    }
    
    @Test
    public void testResolutionOfGenericModelWithBaseTypesEnabled() throws Exception {
        enableBaseTypeSupport();
        executeTag();
        verifyResourceIsAdaptedToMostSpecificModelIncludingBaseTypes();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    @Test
    public void testResolutionOfGenericModelWithoutBaseTypes() throws Exception {
        executeTag();
        verifyResourceIsAdaptedToMostSpecificModel();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    @Test
    public void testResolutionOfGenericModelWithExplicitModelModelName() throws Exception {
        withDesiredModelNamed("name");
        executeTag();
        verifyResourceIsAdaptedToMostSpecificModelWithProvidedModelName();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    @Test
    public void testResolutionOfGenericModelWithEmptyModelModelName() throws Exception {
        withDesiredModelNamed(" ");
        executeTag();
        verifyResourceIsAdaptedToMostSpecificModel();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfMissingProvider() throws Exception {
        withMissingProviderService();
        executeTag();
    }

    @Test
    public void testUserDefinedVariableNameOverridesDefaultVariableName() throws Exception {
        withUserDefinedVariableName("userDefined");
        executeTag();
        verifyGenericModelIsAddedToPageContextWithVariableName("userDefined");
    }

    @Test
    public void testFallbackToDefaultVariableNameIfUserDefinedVariableNameIsNull() throws Exception {
        withUserDefinedVariableName(null);
        executeTag();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    @Test
    public void testFallbackToDefaultVariableNameIfUserDefinedVariableNameIsEmpty() throws Exception {
        withUserDefinedVariableName("");
        executeTag();
        verifyGenericModelIsAddedToPageContextWithDefaultVariableName();
    }

    private void withUserDefinedVariableName(String variableName) {
        this.testee.setVar(variableName);
    }

    private void withMissingProviderService() {
        doReturn(null).when(this.sling).getService(eq(ResourceModelResolver.class));
    }

    private void verifyResourceIsAdaptedToMostSpecificModelWithProvidedModelName() {
        verify(this.resourceModelResolver).resolveMostSpecificModelWithName(eq(this.resource), eq(this.desiredModelName));
    }

    private void withDesiredModelNamed(String name) {
        this.desiredModelName = name;
        this.testee.setUseModelNamed(name);
    }

    private void verifyResourceIsAdaptedToMostSpecificModelIncludingBaseTypes() {
        verify(this.resourceModelResolver).resolveMostSpecificModelIncludingModelsForBaseTypes(eq(this.resource));
    }

    private void verifyResourceIsAdaptedToMostSpecificModel() {
        verify(this.resourceModelResolver).resolveMostSpecificModel(eq(this.resource));
    }

    private void enableBaseTypeSupport() {
        this.testee.setIncludeGenericBaseTypes(true);
    }

    private void verifyGenericModelIsAddedToPageContextWithDefaultVariableName() {
        verify(this.context).setAttribute(eq(Constants.MODEL), eq(this.model));
    }

    private void verifyGenericModelIsAddedToPageContextWithVariableName(String variableName) {
        verify(this.context).setAttribute(eq(variableName), eq(this.model));
    }

    private void withBindings(SlingBindings bindings) {
        when(this.request.getAttribute(eq(SlingBindings.class.getName()))).thenReturn(bindings);
    }

    private void assertTagEvaluationReturnsContinueWithPage() {
        assertThat(this.tagExecutionReturnCode, is(TagSupport.EVAL_PAGE));
    }

    private void verifyAllBindingsAreCopiedIntoPageContext() {
        for (Map.Entry<String, Object> e : this.bindings.entrySet()) {
            verify(this.context, times(1)).setAttribute(eq(e.getKey()), eq(e.getValue()));
        }
    }

    private void executeTag() throws JspException {
        this.tagExecutionReturnCode = this.testee.doEndTag();
    }

    private void withBinding(String key, String value) {
        this.bindings.put(key, value);
    }
}
