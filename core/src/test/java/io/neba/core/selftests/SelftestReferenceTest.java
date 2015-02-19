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

package io.neba.core.selftests;

import io.neba.api.annotations.SelfTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.type.MethodMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SelftestReferenceTest {
	
	/**
	 * Test interface with one function.
	 */
    public interface TestBean {
        void selftestMethod();
    }

    @Mock
    private BeanFactory factory;
    @Mock
    private MethodMetadata methodMetadata;
    @Mock
    private SelfTest selfTest;
    @Mock
    private Bundle bundle;

    private String methodName;
    private TestBean bean;
    private long bundleId = 123L;
    private Map<String, Object> metadataAttributes;
    private String beanName = "testBean";

    private SelftestReference testee;

    @Before
    public void prepare() throws Exception {
        this.methodName = "selfTestMethod";
        this.metadataAttributes = new HashMap<String, Object>();

        when(this.bundle.getBundleId()).thenReturn(this.bundleId);
        when(this.methodMetadata.getMethodName()).thenReturn(this.methodName);
        when(this.methodMetadata.getAnnotationAttributes(eq(SelfTest.class.getName()))).thenReturn(this.metadataAttributes);
    }
    
    @Test
    public void testHashCodeAndEqualsWithMatchingReferences() throws Exception {
        SelftestReference one = new SelftestReference(this.factory, this.beanName, this.methodMetadata, this.bundle);
        SelftestReference two = new SelftestReference(this.factory, this.beanName, this.methodMetadata, this.bundle);
        
        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isEqualTo(two);
        assertThat(two).isEqualTo(one);
    }

    @Test
    public void testHashCodeAndEqualsWithDifferentTestMethodName() throws Exception {
        SelftestReference one = new SelftestReference(this.factory, this.beanName, this.methodMetadata, this.bundle);
        SelftestReference two = new SelftestReference(this.factory, this.beanName, this.selfTest, "otherTestMethod", this.bundle);

        assertThat(one.hashCode()).isNotSameAs(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);

        assertThat(one.hashCode()).isNotSameAs(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
    }

    @Test
    public void testRetrievalOfSelftestMetadata() throws Exception {
        withMetadata("value", "selftest description");
        withMetadata("success", "selftest success description");
        withMetadata("failure", "selftest failure description");
        
        createSelftestReferenceUsingMethodMetadata();
        
        assertDescriptionIs("selftest description");
        assertSuccessDescriptionIs("selftest success description");
        assertFailureDescriptionIs("selftest failure description");
        assertBundleIdIs(this.bundleId);
    }
    
    @Test
    public void testRetrievalOfSelftestMetadataFromAnnotation() throws Exception {
        withMethodName("testMethod");
        withSelftestDescription("selftest description");
        withSelftestSuccessDescription("selftest success description");
        withSelftestFailureDescription("selftest failure description");
        
        createSelftestReferenceUsingSelftestInstance();

        assertDescriptionIs("selftest description");
        assertSuccessDescriptionIs("selftest success description");
        assertFailureDescriptionIs("selftest failure description");
        assertBundleIdIs(this.bundleId);
    }
    
    @Test
    public void testSelftestExecution() throws Exception {
        mockTestBean();
        withMethodName("selftestMethod");
        createSelftestReferenceUsingSelftestInstance();
        executeSelftest();
        verifySelftestWasExecuted();
    }
    
    @Test
    public void testSelftestIdCreation() throws Exception {
        withMethodName("testmethod");
        createSelftestReferenceUsingSelftestInstance();
        assertThat(this.testee.getId(), is("/123/testbean/testmethod"));
    }

    private void verifySelftestWasExecuted() {
        verify(this.bean, times(1)).selftestMethod();
    }

    private void executeSelftest() {
        this.testee.execute();
    }

    private void mockTestBean() {
        this.bean = mock(TestBean.class);
        when(this.factory.getBean(eq(this.beanName))).thenReturn(this.bean);
    }

    private void withSelftestFailureDescription(String value) {
        when(this.selfTest.failure()).thenReturn(value);
    }

    private void withSelftestSuccessDescription(String value) {
        when(this.selfTest.success()).thenReturn(value);
    }

    private void withSelftestDescription(String value) {
        when(this.selfTest.value()).thenReturn(value);
    }

    private void withMethodName(String string) {
        this.methodName = string;
    }

    private void createSelftestReferenceUsingSelftestInstance() {
        this.testee = new SelftestReference(this.factory, this.beanName, this.selfTest, this.methodName, this.bundle);
    }

    private void withMetadata(String attributeName, String value) {
        this.metadataAttributes.put(attributeName, value);
    }

    private void assertBundleIdIs(long bundleId) {
        assertThat(this.testee.getBundleId()).isSameAs(bundleId);
    }

    private void assertFailureDescriptionIs(String string) {
        assertThat(this.testee.getFailure()).isEqualTo(string);
    }

    private void assertSuccessDescriptionIs(String string) {
        assertThat(this.testee.getSuccess()).isEqualTo(string);
    }

    private void assertDescriptionIs(String string) {
        assertThat(this.testee.getDescription()).isEqualTo(string);
    }

    private void createSelftestReferenceUsingMethodMetadata() {
        this.testee = new SelftestReference(this.factory, this.beanName, this.methodMetadata, this.bundle);
    }
}
