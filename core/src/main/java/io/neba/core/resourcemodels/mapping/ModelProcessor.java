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

package io.neba.core.resourcemodels.mapping;

import io.neba.api.annotations.AfterMapping;
import io.neba.core.resourcemodels.metadata.MethodMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static io.neba.core.util.ReflectionUtil.makeAccessible;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Invokes the {@link ResourceModelMetaData#getAfterMappingMethods() post-mapping methods}
 * after the {@link io.neba.api.annotations.ResourceModel}'s mapping is complete.
 *
 * @author Olaf Otto
 */
@Component(service = ModelProcessor.class)
public class ModelProcessor {
    private final Logger logger = getLogger(getClass());

    /**
     * @param metaData must not be <code>null</code>.
     * @param model    must not be <code>null</code>.
     */
    <T> void processAfterMapping(ResourceModelMetaData metaData, T model) {
        if (metaData == null) {
            throw new IllegalArgumentException("Method argument metaData must not be null.");
        }
        if (model == null) {
            throw new IllegalArgumentException("Method argument model must not be null.");
        }

        for (MethodMetaData methodMetaData : metaData.getAfterMappingMethods()) {
            Method method = methodMetaData.getMethod();
            makeAccessible(method);
            try {
                method.invoke(model);
            } catch (InvocationTargetException | SecurityException e) {
                logger.error("Unable to invoke the @" + AfterMapping.class.getSimpleName() + " method " + method + ".", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("It must not be illegal to access " + method + ".", e);
            }
        }
    }
}
