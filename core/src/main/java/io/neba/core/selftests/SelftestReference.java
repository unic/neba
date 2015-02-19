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
import io.neba.core.util.OsgiBeanSource;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * References a {@link SelfTest}, i.e. a method of a bean
 * in a {@link BeanFactory} that was annotated with {@link SelfTest}.
 * <br />
 * Provides {@link #execute() execution}, i.e. invocation of the
 * method on the corresponding bean instance. 
 * 
 * @author Olaf Otto
 */
public class SelftestReference extends OsgiBeanSource<Object> {
    private final String success;
    private final String failure;
    private final String description;
    private final String methodName;
    private final Pattern nonAsciiCharacters = Pattern.compile("[^A-z]");
    private final int hashCode;

    public SelftestReference(BeanFactory factory, String beanName, MethodMetadata methodMetadata, Bundle bundle) {
        super(beanName, factory, bundle);
        Map<String, Object> annotationAttributes = methodMetadata.getAnnotationAttributes(SelfTest.class.getName());
        this.success = (String) annotationAttributes.get("success");
        this.failure = (String) annotationAttributes.get("failure");
        this.description = (String) annotationAttributes.get("value");
        this.methodName = methodMetadata.getMethodName();
        this.hashCode = new HashCodeBuilder().append(beanName).append(bundle.getBundleId()).append(this.methodName).toHashCode();
    }
    
    public SelftestReference(BeanFactory factory, String beanName, SelfTest selfTest, String methodName, Bundle bundle) {
        super(beanName, factory, bundle);
        
        this.success = selfTest.success();
        this.failure = selfTest.failure();
        this.description = selfTest.value();
        this.methodName = methodName;
        this.hashCode = new HashCodeBuilder().append(beanName).append(bundle.getBundleId()).append(methodName).toHashCode();
    }

    public void execute() {
        Object bean = getBean();
        Method method = ReflectionUtils.findMethod(bean.getClass(), this.methodName);
        ReflectionUtils.invokeMethod(method, bean);
    }

    @Override
    public String toString() {
        return this.methodName + " on " + this.getBeanName() + " in bundle " + this.getBundleId();
    }

    /**
     * @return a unique id for the referenced selftest. 
     *         The id is a path consisting of the source bundle id, bean name
     *         and method name of the selftest. Never <code>null</code>.
     */
    public String getId() {
        return "/" + getBundleId() + "/" + toIdentifierPart(this.getBeanName()) + "/" + toIdentifierPart(this.methodName);
    }
    
    private String toIdentifierPart(String s) {
        String part = s.toLowerCase(Locale.GERMAN);
        return this.nonAsciiCharacters.matcher(part).replaceAll("-");
    }
    
    public String getSuccess() {
        return success;
    }

    public String getFailure() {
        return failure;
    }

    public String getDescription() {
        return description;
    }
    
    @Override
    public int hashCode() {
        return this.hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return ((SelftestReference) obj).methodName.equals(this.methodName);
    }
}

