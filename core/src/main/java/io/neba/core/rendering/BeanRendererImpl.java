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

import io.neba.api.Constants;
import io.neba.api.rendering.BeanRenderer;
import io.neba.core.util.FastStringWriter;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.Writer;
import java.util.List;
import java.util.Map;

import static io.neba.core.util.ClassHierarchyIterator.hierarchyOf;
import static org.apache.commons.lang.StringUtils.endsWith;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.util.Assert.notNull;

/**
 * Renders an object using the provided view repository path, the object's
 * {@link io.neba.core.util.ClassHierarchyIterator class hierarchy} and a provided viewHint to
 * build possible template names for the object. Supports adding additional elements to the 
 * rendering context.
 * 
 * <p>
 * Example: For a class io.neba.MyModel implementing the interface
 * io.neba.MyInterface, the renderer will attempt to resolve the following
 * templates (in this particular order according to the
 * {@link io.neba.core.util.ClassHierarchyIterator}), if the repositoryPath is "/apps/views/" and
 * the view hint is null:
 * </p>
 * 
 * <ol>
 * <li>/apps/views/io/neba/MyModel.vlt</li>
 * <li>/apps/views/io/neba/MyInterface.vlt</li>
 * <li>/java/lang/Object.vlt</li>
 * </ol>
 * 
 * <p>
 * The first view found is used to render the object.
 * </p>
 * 
 * <p>
 * Likewise, when rendering with a viewhint, e.g. "teaser", the following views
 * would be resolved:
 * </p>
 * 
 * <ol>
 * <li>/apps/views/io/neba/MyModel-teaser.vlt</li>
 * <li>/apps/views/io/neba/MyInterface-teaser.vlt</li>
 * <li>/java/lang/Object-teaser.vlt</li>
 * </ol>
 * 
 * TODO: Performance optimization: Caching of failed lookups and hierarchy iteration.
 * 
 * @author Olaf Otto
 */
public class BeanRendererImpl implements BeanRenderer {
    private final String repositoryPath;
    private final VelocityEngine engine;
    private final List<BindingsValuesProvider> bindingsValuesProviders;

    public BeanRendererImpl(String repositoryPath, VelocityEngine engine, List<BindingsValuesProvider> bindingsValuesProviders) {
        notNull(repositoryPath, "The view repository root path must not be null.");
        notNull(engine, "The velocity engine must not be null.");

        this.repositoryPath = normalize(repositoryPath);
        this.engine = engine;
        this.bindingsValuesProviders = bindingsValuesProviders;
    }

    private String normalize(String repositoryPath) {
        if (!endsWith(repositoryPath, "/")) {
            return repositoryPath + "/";
        }
        return repositoryPath;
    }

    @Override
    public String render(Object bean, String viewHint, Map<String, Object> additionalContextElements) {
        notNull(bean, "Method argument bean must not be null.");
        String renderedObject = null;
        for (Class<?> type : hierarchyOf(bean.getClass())) {
            String templatePath = createTemplateName(viewHint, type);
            if (this.engine.resourceExists(templatePath)) {
                renderedObject = renderInternal(bean, templatePath, additionalContextElements);
                break;
            }
        }
        return renderedObject;
    }

    public String renderInternal(Object object, String templatePath, Map<String, Object> additionalContextElements) {
        final Writer writer = new FastStringWriter();
        final VelocityBindings bindings = prepareBindings(object);
        merge(additionalContextElements, bindings);
        final VelocityContext context = new VelocityContext(bindings);

        String renderedObject;
        try {
            this.engine.getTemplate(templatePath).merge(context, writer);
        } catch (Exception e) {
            throw new RuntimeException("Unable to render " + object + " with template " + templatePath + ".", e);
        }
        renderedObject = writer.toString();

        return renderedObject;
    }

    private VelocityBindings prepareBindings(Object model) {
        VelocityBindings bindings = new VelocityBindings();
        for (BindingsValuesProvider provider : this.bindingsValuesProviders) {
            provider.addBindings(bindings);
        }
        bindings.put(Constants.RENDERER, this);
        bindings.put(Constants.MODEL, model);
        return bindings;
    }

    public String createTemplateName(String viewHint, Class<?> type) {
        StringBuilder templatePathBuilder = new StringBuilder(128);
        templatePathBuilder.append(this.repositoryPath).append(type.getName().replace('.', '/'));
        if (!isBlank(viewHint)) {
            templatePathBuilder.append('-').append(viewHint);
        }
        templatePathBuilder.append(".vlt");
        return templatePathBuilder.toString();
    }

    private void merge(Map<String, Object> context, final VelocityBindings bindings) {
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                final String key = entry.getKey();
                if (bindings.containsKey(key)) {
                    throw new IllegalArgumentException("The bindings already contain the key '" + key + "' (a " + bindings.get(key) + ").");
                } else {
                    bindings.put(key, entry.getValue());
                }
            }
        }
    }

    @Override
    public String render(Object bean) {
        return render(bean, null, null);
    }

    @Override
    public String render(Object bean, Map<String, Object> context) {
        return render(bean, null, context);
    }

    @Override
    public String render(Object bean, String viewHint) {
        return render(bean, viewHint, null);
    }
}
