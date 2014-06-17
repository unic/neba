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
import io.neba.core.blueprint.EventhandlingBarrier;
import io.neba.core.blueprint.ReferenceConsistencyChecker;
import org.eclipse.gemini.blueprint.service.importer.ImportedOsgiServiceProxy;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static io.neba.core.util.BundleUtil.displayNameOf;
import static org.springframework.beans.factory.BeanFactoryUtils.isFactoryDereference;

/**
 * Detects beans that have methods annotated with {@link SelfTest}. <br />
 * Considers all beans defined in an application context unless they are OSGi
 * references, i.e. from a foreign bundle.
 * 
 * @author Olaf Otto
 */
@Service
public class SelftestRegistrar {
    private static final long EVERY_30_SECONDS = 30 * 1000;
    private final Collection<SelftestReference> selftestReferences = new LinkedHashSet<SelftestReference>();
    private final String selftestAnnotationName = SelfTest.class.getName();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Inject
    private ReferenceConsistencyChecker consistencyChecker;

    public void registerSelftests(ConfigurableListableBeanFactory factory, Bundle bundle) {
        String[] beanNames = BeanFactoryUtils.beanNamesIncludingAncestors(factory);
        for (String beanName : beanNames) {
            if (factory.containsBeanDefinition(beanName) && !isInternal(beanName)) {
                findSelftests(factory, beanName, bundle);
            }
        }
    }

    public List<SelftestReference> getSelftestReferences() {
        return new ArrayList<SelftestReference>(this.selftestReferences);
    }

    @Scheduled(fixedRate = EVERY_30_SECONDS)
    public void removeInvalidReferences() {
        if (EventhandlingBarrier.tryBegin()) {
            try {
                this.logger.debug("Checking for references to beans from inactive bundles...");
                for (Iterator<SelftestReference> it = this.selftestReferences.iterator(); it.hasNext(); ) {
                    final SelftestReference reference = it.next();
                    if (!this.consistencyChecker.isValid(reference)) {
                        this.logger.info("Reference to " + reference + " is invalid, removing.");
                        it.remove();
                    }
                }
                this.logger.debug("Completed checking for references to beans from inactive bundles.");
            } finally {
                EventhandlingBarrier.end();
            }
        }
    }
    
    private void findSelftests(final ConfigurableListableBeanFactory factory, String beanName, Bundle bundle) {
        BeanDefinition definition = factory.getBeanDefinition(beanName);
        if (isOsgiServiceReference(factory, beanName)) {
            this.logger.info("Skipping bean " + beanName + " from bundle " + displayNameOf(bundle) + ", it is an osgi service reference.");
        } else if (definition instanceof AnnotatedBeanDefinition) {
            findSelftestUsingBeanDefinition(factory, beanName, bundle, definition);
        } else {
            findSelftestUsingReflection(factory, beanName, bundle);
        }
    }

    /**
     * A bean may be the representation of an OSGi service provided by a
     * different bundle - in this case we must not check it for selftests, as
     * this is done in the service's source bundle.
     */
    private boolean isOsgiServiceReference(BeanFactory factory, String beanName) {
        Class<?> beanType = factory.getType(beanName);
        return beanType != null && ImportedOsgiServiceProxy.class.isAssignableFrom(beanType);
    }

    /**
     * If a bean was detected by classpath scanning, i.e. is annotated (e.g.
     * with {@link org.springframework.stereotype.Component}), the bean
     * definition already contains metadata for all bean annotations. It is thus
     * more efficient to use this metadata than using reflection.
     */
    private void findSelftestUsingBeanDefinition(ConfigurableListableBeanFactory factory, 
    		String beanName, Bundle bundle, BeanDefinition definition) {
        AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) definition;
        AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
        if (isSelftestingBean(metadata)) {
            for (MethodMetadata selftestMethodMetadata : getSelfTestMethods(metadata)) {
                final long bundleId = bundle.getBundleId();
                this.selftestReferences.add(new SelftestReference(factory, beanName, selftestMethodMetadata, bundleId));
            }
        }
    }

    /**
     * In case no annotation metadata exists, find selftests by checking all
     * methods for the {@link SelfTest} annotation.
     */
    private void findSelftestUsingReflection(final ConfigurableListableBeanFactory factory, final String beanName, final Bundle bundle) {
        Class<?> beanType = factory.getType(beanName);
        if (beanType != null) {
            beanType = unproxy(beanType);
            ReflectionUtils.doWithMethods(beanType, new MethodCallback() {
                @Override
                public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
                    SelfTest selfTest = AnnotationUtils.findAnnotation(method, SelfTest.class);
                    if (selfTest != null) {
                        String methodName = method.getName();
                        long bundleId = bundle.getBundleId();
                        selftestReferences.add(new SelftestReference(factory, beanName, selfTest, methodName, bundleId));
                    }
                }
            });
        }
    }

    /**
     * Certain prefixes mark bean definitions as "internal", i.e. definitions of
     * factory-internal service beans or automatically generated infrastructure
     * bean definitions.
     */
    private boolean isInternal(String beanName) {
        return isFactoryDereference(beanName) || beanName.startsWith("scopedTarget.");
    }

    /**
     * Since proxies may implement a type's signature but not include a type's
     * annotations, we need to unproxy types before scanning for annotations.
     */
    private Class<?> unproxy(Class<?> beanType) {
        Class<?> unproxiedType = beanType;
        if (ClassUtils.isCglibProxyClass(beanType)) {
            // It is a dynamic subclass re-implementing the same methods.
            unproxiedType = beanType.getSuperclass();
        }
        return unproxiedType;
    }

    private Set<MethodMetadata> getSelfTestMethods(AnnotationMetadata metadata) {
        return metadata.getAnnotatedMethods(this.selftestAnnotationName);
    }

    private boolean isSelftestingBean(AnnotationMetadata metadata) {
        return metadata.hasAnnotatedMethods(this.selftestAnnotationName);
    }

    public void unregister(Bundle bundle) {
        removeSelftests(bundle);
    }

    private synchronized void removeSelftests(Bundle bundle) {
        this.logger.info("Removing bundle " + displayNameOf(bundle) + " from the selftest registry...");
        Iterator<SelftestReference> i = this.selftestReferences.iterator();
        while (i.hasNext()) {
            if (i.next().getBundleId() == bundle.getBundleId()) {
                i.remove();
            }
        }
        this.logger.info("Bundle " + displayNameOf(bundle) + " was removed from the selftest registry.");
    }

    public void setConsistencyChecker(ReferenceConsistencyChecker consistencyChecker) {
        this.consistencyChecker = consistencyChecker;
    }
}