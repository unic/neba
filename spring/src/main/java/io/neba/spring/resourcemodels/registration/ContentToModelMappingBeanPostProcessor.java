/*
  Copyright 2013 the original author or authors.
  <p>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.neba.spring.resourcemodels.registration;

import io.neba.api.spi.ResourceModelFactory.ContentToModelMappingCallback;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.Nonnull;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Performs content-to-model mapping for {@link ModelDefinition known spring resource model beans}
 * using a thread-local stack of {@link #push(ContentToModelMappingCallback) registered callbacks}.
 *
 * @see SpringModelRegistrar
 */
class ContentToModelMappingBeanPostProcessor implements BeanPostProcessor {
    private final ThreadLocal<Deque<ContentToModelMappingCallback<Object>>> mappingCallbackThreadLocal = ThreadLocal.withInitial(LinkedList::new);
    private final Set<String> knownNebaModelBeanNames;

    <T extends ModelDefinition<Object>> ContentToModelMappingBeanPostProcessor(List<T> definitions) {
        final Set<String> knownNebaModelBeanNames = new HashSet<>();
        definitions.forEach(definition -> knownNebaModelBeanNames.add(definition.getName()));
        this.knownNebaModelBeanNames = knownNebaModelBeanNames;
    }

    @Override
    public final Object postProcessBeforeInitialization(@Nonnull Object bean, String beanName) throws BeansException {
        if (!knownNebaModelBeanNames.contains(beanName)) {
            return null;
        }
        ContentToModelMappingCallback<Object> contentToModelMappingCallback = mappingCallbackThreadLocal.get().peek();
        if (contentToModelMappingCallback == null) {
            return null;
        }

        return contentToModelMappingCallback.map(bean);
    }

    @Override
    public final Object postProcessAfterInitialization(@Nonnull Object bean, String beanName) throws BeansException {
        return null;
    }

    void push(ContentToModelMappingCallback<Object> callback) {
        mappingCallbackThreadLocal.get().addFirst(callback);
    }

    void pop() {
        Queue<ContentToModelMappingCallback<Object>> stack = mappingCallbackThreadLocal.get();
        stack.remove();
        if (stack.isEmpty()) {
            mappingCallbackThreadLocal.remove();
        }
    }
}
