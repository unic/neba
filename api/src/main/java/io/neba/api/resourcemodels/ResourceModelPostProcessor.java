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

package io.neba.api.resourcemodels;

import org.apache.sling.api.resource.Resource;
import org.springframework.beans.factory.BeanFactory;

/**
 * Lifecycle callback for
 * {@link io.neba.api.annotations.ResourceModel resource models}. <br />
 * 
 * {@link #processBeforeMapping(Object, Resource, BeanFactory)} is invoked after
 * the bean was created and initialized by the {@link BeanFactory} but before
 * all resource properties are mapped. All spring lifecycle callbacks where
 * already called at this point. <br />
 * 
 * {@link #processAfterMapping(Object, Resource, BeanFactory)} is invoked after
 * the bean was created and initialized by the {@link BeanFactory} and all
 * resource properties are mapped. All spring lifecycle callbacks where already
 * called at this point. <br />
 * 
 * OSGi services providing this interface are automatically detected by the core
 * and apply to all resource models. There are no guarantees concerning the
 * order in which these post processors are invoked; if a specific order is
 * required, implementing a {@link ResourceModelPostProcessor} that delegates to
 * other post processors in the desired order is advised.
 * 
 * @author Olaf Otto
 */
public interface ResourceModelPostProcessor {
    /**
     * Lifecycle callback invoked before the resource properties are mapped onto
     * the {@link io.neba.api.annotations.ResourceModel}.
     * 
     * @param resourceModel is never <code>null</code>.
     * @param resource is never <code>null</code>.
     * @param factory is never <code>null</code>.
     * @return a new resource model overriding the provided resourceModel, or
     *         <code>null</code> if the resource model is not to be changed.
     */
    <T> T processBeforeMapping(T resourceModel, Resource resource, BeanFactory factory);

    /**
     * Lifecycle callback invoked after the resource properties are mapped onto
     * the {@link io.neba.api.annotations.ResourceModel}.
     * 
     * @param resourceModel is never <code>null</code>.
     * @param resource is never <code>null</code>.
     * @param factory is never <code>null</code>.
     * @return a new resource model overriding the provided resourceModel, or
     *         <code>null</code> if the resource model is not to be changed.
     */
    <T> T processAfterMapping(T resourceModel, Resource resource, BeanFactory factory);
}
