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

package io.neba.core.resourcemodels.mapping;

import io.neba.api.annotations.PostMapping;
import io.neba.api.annotations.PreMapping;
import io.neba.core.resourcemodels.metadata.MethodMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invokes {@link io.neba.core.resourcemodels.metadata.MethodMetaData#isPreMappingCallback pre-mapping methods}
 * before and the {@link io.neba.core.resourcemodels.metadata.MethodMetaData#isPostMappingCallback post-mapping methods}
 * after the {@link io.neba.api.annotations.ResourceModel}'s mapping is complete.
 *
 * @author Olaf Otto
 */
@Service
public class ModelProcessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public <T> void processAfterMapping(ResourceModelMetaData metaData, T model) {
        for (MethodMetaData methodMetaData : metaData.getPostMappingMethods()) {
            Method method = methodMetaData.getMethod();
            method.setAccessible(true);
            try {
                method.invoke(model);
            } catch (InvocationTargetException | SecurityException e) {
                logger.error("Unable to invoke the @" + PostMapping.class.getSimpleName() + " method " + method + ".", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("It must not be illegal to access " + method + ".", e);
            }
        }
    }

    public <T> void processBeforeMapping(ResourceModelMetaData metaData, T model) {
        for (MethodMetaData methodMetaData : metaData.getPreMappingMethods()) {
            Method method = methodMetaData.getMethod();
            method.setAccessible(true);
            try {
                method.invoke(model);
            } catch (InvocationTargetException | SecurityException e) {
                logger.error("Unable to invoke the @" + PreMapping.class.getSimpleName() + " method " + method + ".", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("It must not be illegal to access " + method + ".", e);
            }
        }
    }
}
