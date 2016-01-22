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

import io.neba.api.rendering.BeanRenderer;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.sling.api.resource.LoginException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.apache.commons.lang.StringUtils.trim;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class BeanRendererFactoryImplTest {
    /**
     * @author Olaf Otto
     */
    public static class TestObject {
		public String getTestString() {
            return "Hello, world.";
        }
    }

    /**
     * We need an actual {@link ResourceLoader} implementation since
     * the velocity engine requires some basic functionality (such as the detection of caching state)
     * to exist in the resource loader. We test against this functionality.
     *  
     * @author Olaf Otto
     */
    private class TestResourceLoader extends ResourceLoader {
        @Override
        public void init(ExtendedProperties configuration) {
        }

        @Override
        public InputStream getResourceStream(String source) throws ResourceNotFoundException {
            return getClass().getClassLoader().getResourceAsStream(getResourceReturnedByResourceLoader());
        }

        @Override
        public boolean isSourceModified(Resource resource) {
            return false;
        }

        @Override
        public long getLastModified(Resource resource) {
            return 0;
        }
        
        @Override
        public boolean resourceExists(String resourceName) {
            return resourceName.contains("TestObject");
        }
    }
        
    private BeanRenderer renderer;
    private Object testObject;
    private String renderedObject;
    private ResourceLoader resourceLoader;
    private String resourceReturnedByResourceLoader;

    private BeanRendererFactoryImpl testee;
    

    @Before
    public void prepareFactory() throws Exception {
        createFactory();
        mockResourceLoader();
        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestObject.vlt");
        refreshFactory();
    }

    @Test
    public void testCachingIsEnabledWhenTemplateCacheIntervalIsGreaterZero() throws Exception {
        withTestObject(new TestObject());
        withTemplateLivespan(10);
        refreshFactory();
        createRenderer();

        renderTestObject();
        assertRenderedObjectIs("Hello, world.");

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestObject2.vlt");

        renderTestObject();
        assertRenderedObjectIs("Hello, world.");
    }

    @Test
    public void testCachingIsDisabledWhenTemplateCacheIntervalZeroOrLess() throws Exception {
        withTestObject(new TestObject());
        withTemplateLivespan(0);
        refreshFactory();
        createRenderer();

        renderTestObject();
        assertRenderedObjectIs("Hello, world.");

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestObject2.vlt");

        renderTestObject();
        assertRenderedObjectIs("two");
    }

    @Test
    public void testMacrosAreNotCachedWhenCacheIsDisabled() throws Exception {
        withTestObject(new TestObject());
        withTemplateLivespan(0);
        refreshFactory();
        createRenderer();

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro1.vlt");
        renderTestObject();
        assertRenderedObjectIs("macro1");

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro2.vlt");
        renderTestObject();
        assertRenderedObjectIs("macro2");
    }

    @Test
    public void testMacrosAreCachedWhenCacheIsEnabled() throws Exception {
        withTestObject(new TestObject());
        withTemplateLivespan(10);
        refreshFactory();
        createRenderer();

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro1.vlt");
        renderTestObject();
        assertRenderedObjectIs("macro1");

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro2.vlt");
        renderTestObject();
        assertRenderedObjectIs("macro1");
    }

    @Test
    public void testMacrosAreTemplateLocal() throws Exception {
        withTestObject(new TestObject());
        withTemplateLivespan(0);
        refreshFactory();
        createRenderer();

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro1.vlt");
        renderTestObject();
        assertRenderedObjectIs("macro1");

        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestMacro3.vlt");
        renderTestObject();
        assertRenderedObjectIs("#test()");
    }

    @Test
    public void testBeanRendererCreation() throws Exception {
        createRenderer();
        assertRendererWasCreated();
    }
    
    @Test
    public void testRendering() throws Exception {
        createRenderer();
        withTestObject(new TestObject());
        renderTestObject();
        assertRenderedObjectIs("Hello, world.");
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPathConfiguration() throws Exception {
        createResourceLoaderWithConfiguration("junit:/junit/test", "junit/junit/test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationWithBlankName() throws Exception {
        createResourceLoaderWithConfiguration(" :/junit/junit/test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConfigurationWithBlankPath() throws Exception {
        createResourceLoaderWithConfiguration("name: ");
    }

    @Test
    public void testDefaultRendererRetrieval() throws Exception {
        getDefaultRenderer();
        assertRendererIsNull();

        prepareFactoryWithDefaultRenderer();
        getDefaultRenderer();
        assertRendererWasCreated();
    }

    @Test
    public void testRemovalOfNullProviderDoesNotCauseException() throws Exception {
        this.testee.unbind(null);
    }

    private void assertRendererIsNull() {
        assertThat(this.renderer).isNull();
    }

    private void getDefaultRenderer() {
        this.renderer = this.testee.getDefault();
    }

    private void createResourceLoaderWithConfiguration(String... config) throws Exception {
        this.testee = new BeanRendererFactoryImpl();
        this.testee.setResourceLoader(this.resourceLoader);
        this.testee.setRenderers(config);
        refreshFactory();
    }
    
    private void assertRenderedObjectIs(String expected) {
        assertThat(this.renderedObject).isEqualTo(expected);
    }

    private void renderTestObject() {
        this.renderedObject = trim(this.renderer.render(this.testObject));
    }

    private void withTestObject(TestObject object) {
        this.testObject = object;
    }

    private void assertRendererWasCreated() {
        assertThat(this.renderer).isNotNull();
    }

    private void createRenderer() {
        this.renderer = this.testee.get("junit");
    }

    private void createFactory() {
        this.testee = new BeanRendererFactoryImpl();
        this.testee.setRenderers(new String[]{"junit:/junit/test"});
    }

    private void prepareFactoryWithDefaultRenderer() throws Exception {
        createFactoryWithDefaultRenderer();
        mockResourceLoader();
        withResourceReturnedByResourceLoader("io/neba/core/rendering/TestObject.vlt");
        refreshFactory();
    }

    private void createFactoryWithDefaultRenderer() {
        this.testee = new BeanRendererFactoryImpl();
        this.testee.setRenderers(new String[]{"default:/junit/test"});
    }

    private void refreshFactory() throws LoginException {
        this.testee.refresh(null);
    }

    private void mockResourceLoader() {
        this.resourceLoader = new TestResourceLoader();
        this.testee.setResourceLoader(this.resourceLoader);
    }

    private String getResourceReturnedByResourceLoader() {
        return resourceReturnedByResourceLoader;
    }

    private void withResourceReturnedByResourceLoader(String resourceReturnedByResourceLoader) {
        this.resourceReturnedByResourceLoader = resourceReturnedByResourceLoader;
    }

    private void withTemplateLivespan(final int templateCacheLivespanInSeconds) {
        this.testee.setTemplateCacheLifespanInSeconds(templateCacheLivespanInSeconds);
    }
}
