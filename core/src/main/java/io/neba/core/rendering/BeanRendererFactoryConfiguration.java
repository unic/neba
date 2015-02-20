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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * The sole purpose of this class is to provide service component runtime (SCR) metadata
 * enabling service configuration via the OSGi console. The metadata
 * is generated from the SCR annotations of this class by the maven-scr-plugin
 * during build time.
 * <br />
 * The generated metadata is used in the blueprint configuration
 * of this module to configure the {@link BeanRendererFactoryImpl}.
 * 
 * @author Olaf Otto
 */
//CHECKSTYLE:OFF (Checkstyle does not understand annotation)
@Service(BeanRendererFactoryConfiguration.class)
@Component(label = "NEBA Bean Renderer configuration",
           immediate = false,
           description = "Provides configurations for the bean renderer factory.",
           metatype = true,
           name = BeanRendererFactoryConfiguration.PID)
@Properties({
    @Property(name = "service.vendor", value = "neba.io")
})
//CHECKSTYLE:ON
public class BeanRendererFactoryConfiguration {
    private static final String PROPERTY_RENDERERS = "renderers";
    private static final String PROPERTY_TEMPLATECACHE_LIFESPAN = "templateCacheLifespanInSeconds";
    public static final String PID = "io.neba.core.rendering.BeanRendererFactoryConfiguration";
    
    @Property(name = PROPERTY_RENDERERS, 
             label = "Bean renderers", 
             description = "List of renderer names and repository paths. Each entry is of the form " +
                           "name:/path/in/repository. A renderer may then be referenced by its name " +
                           "and will use the repository path to resolve its views.",
             cardinality = Integer.MAX_VALUE)
    private String [] renderers;
    
    @Property(name = PROPERTY_TEMPLATECACHE_LIFESPAN,
             label = "Template cache lifespan",
             description = "In seconds. Defines the amount of time after which an entry in template cache expires. " +
             		       "Example: with \"10\", a cached template will be invalidated after 10 seconds. " +
             		       "Setting this to a high value on a production system is recommended. " +
             		       "Setting this value to a value <= 0 disables the template cache, which " +
             		       "is recommended for development systems.",
	         intValue = 60)
    private int templateCacheLifespanInSeconds;
}