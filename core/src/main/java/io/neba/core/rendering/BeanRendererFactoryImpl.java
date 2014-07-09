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
import io.neba.api.rendering.BeanRendererFactory;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.scripting.api.BindingsValuesProvider;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static io.neba.api.Constants.DEFAULT_RENDERER_NAME;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.velocity.runtime.RuntimeConstants.INPUT_ENCODING;
import static org.apache.velocity.runtime.RuntimeConstants.OUTPUT_ENCODING;
import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER;
import static org.apache.velocity.runtime.RuntimeConstants.RUNTIME_LOG_LOGSYSTEM;
import static org.apache.velocity.runtime.RuntimeConstants.VM_LIBRARY_AUTORELOAD;
import static org.apache.velocity.runtime.RuntimeConstants.VM_PERM_INLINE_LOCAL;
import static org.springframework.util.Assert.notNull;

/**
 * Creates a {@link BeanRenderer} using a {@link VelocityEngine} to render the
 * bean.
 * 
 * @author Olaf Otto
 */
public class BeanRendererFactoryImpl implements BeanRendererFactory {
    private static final String RESOURCELOADER_NAME = "jcr";
    private final Pattern pathPattern = Pattern.compile("/[A-z\\-0-9/]+");
    private final Map<String, BeanRenderer> rendererMap = new ConcurrentHashMap<String, BeanRenderer>();

    @Inject
    private ResourceLoader resourceLoader;
    private List<BindingsValuesProvider> bindingsValuesProviders = new ArrayList<BindingsValuesProvider>();

    private VelocityEngine engine;
    private String encoding = "UTF-8";
    private int templateCacheLifespanInSeconds = 60;
    private List<String> renderers = new ArrayList<String>();
    private boolean initialized;

    @Override
    public BeanRenderer get(String rendererName) {
        notNull(rendererName, "Method argument rendererName must not be null.");
        initTemplateEngine();
        return this.rendererMap.get(rendererName);
    }

    @Override
    public BeanRenderer getDefault() {
        return get(DEFAULT_RENDERER_NAME);
    }

    private String getModificationInterval() {
        return Integer.toString(getTemplateCacheLifespanInSeconds());
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setRenderers(String[] renderers) {
        if (renderers != null) {
            this.renderers = Arrays.asList(renderers);
        }
    }

    /**
     * This replaces {@link javax.annotation.PostConstruct} since it is invoked as soon as the
     * service configuration framework initializes.
     */
    public void refresh(Map<?, ?> properties) throws LoginException {
        prepareTemplateEngine();
        // we created a new template engine and will refresh the renderers;
        // they must thus initialize the engine again when they are obtained.
        this.initialized = false;
        rebuildBeanRenderers();
    }

    private void prepareTemplateEngine() throws LoginException {
        VelocityEngine engine = new VelocityEngine();
        engine.setProperty(RESOURCE_LOADER, RESOURCELOADER_NAME);
        engine.setProperty(RESOURCELOADER_NAME + "." + RESOURCE_LOADER + ".instance", this.resourceLoader);

        if (isTemplateCacheEnabled()) {
            engine.setProperty(RESOURCELOADER_NAME + "." + RESOURCE_LOADER + ".cache", "true");
            engine.setProperty(RESOURCELOADER_NAME + "." + RESOURCE_LOADER + ".modificationCheckInterval", 
            	getModificationInterval());
        } else {
            engine.setProperty(RESOURCELOADER_NAME + "." + RESOURCE_LOADER + ".cache", "false");
            // VM library autoreload is a development feature that only works if
            // the cache is off.
            engine.setProperty(VM_LIBRARY_AUTORELOAD, "true");
        }

        // Do not make macros declared in individual templates globally
        // available. This avoids
        // macro name collisions and caching issues.
        engine.setProperty(VM_PERM_INLINE_LOCAL, "true");

        engine.setProperty(INPUT_ENCODING, this.encoding);
        engine.setProperty(OUTPUT_ENCODING, this.encoding);
        engine.setProperty(RUNTIME_LOG_LOGSYSTEM + ".log4j.logger", "");

        this.engine = engine;
    }

    private boolean isTemplateCacheEnabled() {
        return this.templateCacheLifespanInSeconds > 0;
    }

    /**
     * No need for the overhead of synchronization.
     * {@link VelocityEngine#init()} is synchronized and invoking it twice is
     * safe.
     */
    private void initTemplateEngine() {
        if (!this.initialized) {
            this.engine.init();
            this.initialized = true;
        }
    }

    private void rebuildBeanRenderers() {
        clearRenderers();
        for (String rendererConfig : this.renderers) {
            String[] rendererSettings = split(rendererConfig, ':');
            checkConfigurationFormat(rendererConfig, rendererSettings);
            BeanRenderer renderer = new BeanRendererImpl(rendererSettings[1], this.engine, 
            	this.bindingsValuesProviders);
            this.rendererMap.put(rendererSettings[0], renderer);
        }
    }

    private void clearRenderers() {
        this.rendererMap.clear();
    }

    private void checkConfigurationFormat(String rendererConfig, String[] rendererSettings) {
        String formatAdvice = " should be of the form name:/path/in/repository";
        if (rendererSettings.length != 2) {
            throw new IllegalArgumentException("Illegal renderer configuration: " + rendererConfig + formatAdvice);
        }
        if (isBlank(rendererSettings[0])) {
            throw new IllegalArgumentException("Illegal blank renderer name in renderer configuration " + 
            		rendererConfig + formatAdvice);
        }
        if (isBlank(rendererSettings[1])) {
            throw new IllegalArgumentException("Illegal blank path in renderer configuration " + 
            		rendererConfig + formatAdvice);
        }
        if (!isValidPathSyntax(rendererSettings[1])) {
            throw new IllegalArgumentException("Illegal path '" + rendererSettings[1] + 
            	"' in renderer configuration " + rendererConfig + formatAdvice);
        }
    }

    private boolean isValidPathSyntax(String path) {
        return this.pathPattern.matcher(path).matches();
    }

    public List<BindingsValuesProvider> getBindingsValuesProviders() {
        return bindingsValuesProviders;
    }

    public void setBindingsValuesProviders(List<BindingsValuesProvider> bindingsValuesProviders) {
        this.bindingsValuesProviders = bindingsValuesProviders;
    }

    public void add(BindingsValuesProvider provider) {
        this.bindingsValuesProviders.add(provider);
    }

    public void remove(BindingsValuesProvider provider) {
        this.bindingsValuesProviders.remove(provider);
    }

    public int getTemplateCacheLifespanInSeconds() {
        return templateCacheLifespanInSeconds;
    }

    public void setTemplateCacheLifespanInSeconds(int templateCacheLifespanInSeconds) {
        this.templateCacheLifespanInSeconds = templateCacheLifespanInSeconds;
    }

}
