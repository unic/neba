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
package io.neba.api.spi;

import javax.annotation.Nonnull;

/**
 * OSGi Services with this interface are automatically detected by NEBA and are used to prepare model instances
 * potentially affected by AOP (proxies, enhanced types) for injection. For instance, if a custom {@link ResourceModelFactory} or
 * {@link ResourceModelPostProcessor} service is published that may yield AOP proxies, a suitable {@link AopSupport} service must be published
 * that un-proxies the corresponding model instances such that they can be used for field injection.
 */
public interface AopSupport {
    /**
     * @param model never <code>null</code>. A model instance potentially affected by AOP (e.g., a proxy or enhanced type).
     * @return a view of the model instance suitable for field injection. Never <code>null</code>. The returned model is exclusively
     *         used for field injection, i.e. never directly returned when {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapting to}
     *         or {@link io.neba.api.services.ResourceModelResolver resolving} a model.
     */
    @Nonnull
    Object prepareForFieldInjection(@Nonnull Object model);
}
