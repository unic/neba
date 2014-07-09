//CHECKSTYLE:OFF
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

package io.neba.core.resourcemodels.caching;

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
 * of this module to configure the {@link RequestScopedResourceModelCache}.

 * @author Olaf Otto
 */
@Service(RequestScopedResourceModelCacheConfiguration.class)
@Component(label = "NEBA request-scoped resource model cache",
		   immediate = false,
		   description = "Provides a request-scoped resource model cache.",
		   metatype = true,
		   name = RequestScopedResourceModelCacheConfiguration.PID)
@Properties({
	@Property(name = "service.vendor", value = "neba.io")
})
public class RequestScopedResourceModelCacheConfiguration {
	public static final String PID = "io.neba.core.resourcemodels.caching.RequestScopedResourceModelCacheConfiguration";

	@Property(
    		label = "Enabled",
    		description = "Activates the request-scoped cache for resource models.",
            boolValue = true)
    public static final String ENABLED = "enabled";

    @Property(
            label = "Safemode",
            description = "In safemode, caching is sensitive to the current page resource and request parameters " +
                          "such as selectors, suffix, extension and the query string. Should @ResourceModels erroneously cache such state, " +
                          "e.g. by initializing the corresponding value once in a @PostMapping method, safemode prevents errors caused " +
                          "when performing subsequent internal changes to the request state (e.g. during forwards and includes). Note that " +
                          "enabling this feature is likely to a significant negative performance impact. It is highly recommended to disable " +
                          "safemode in favor of safe-to-cache @ResourceModels.",
            boolValue = false)
    public static final String SAFEMODE = "safeMode";

    @Property(label = "Enable statistics logging",
            description = "Whether to log statistical information",
            boolValue = false)
    private static final String PROPERTY_ENABLED = "enableStatistics";

    @Property(label = "Restrict logging to requests containing",
              description = "Restrict logging to requests containing the following string (e.g. a path or path fragment)")
    private static final String PROPERTY_RESTRICT_TO_URL_CONTAINING = "restrictStatisticsTo";
}