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
package io.neba.core.resourcemodels.views.json;

import io.neba.api.services.ResourceModelResolver;
import io.neba.core.resourcemodels.mapping.NestedMappingSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;

import static io.neba.core.util.BundleUtil.displayNameOf;
import static java.util.regex.Pattern.compile;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

@Component(
        property = {
                "sling.servlet.extensions=json"
        },
        service = Servlet.class
)
@Designate(ocd = JsonViewServlets.Configuration.class, factory = true)
public class JsonViewServlets extends SlingAllMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonViewServlets.class);
    private static final Pattern EXPECTED_MODEL_NAME = compile("[A-z0-9_\\-#]+");
    private static final long serialVersionUID = -7762218328479266916L;

    @Reference
    private ResourceModelResolver modelResolver;

    @Reference
    private NestedMappingSupport nestedMappingSupport;

    private Jackson2ModelSerializer serializer;
    private Configuration configuration;

    @Activate
    protected void activate(@Nonnull ComponentContext context, @Nonnull Configuration configuration) {
        this.configuration = configuration;
        // Jackson is an optional dependency.
        try {
            Class<?> generatorClass = getClass().getClassLoader().loadClass("com.fasterxml.jackson.core.JsonGenerator");
            LOGGER.info("Found JSON generator from {}. JSON views are enabled.", generatorClass.getClassLoader());
            this.serializer = new Jackson2ModelSerializer(nestedMappingSupport::getRecordedMappings, configuration.jacksonSettings(), configuration.addTypeAttribute());
        } catch (ClassNotFoundException e) {
            LOGGER.info("JSON views will not be available since Jackson2 cannot be found from bundle {}. Jackson is an optional dependency. " +
                    "To use the NEBA model to JSON mapping, install at least the jackson-core " +
                    "and jackson-databind bundles.", displayNameOf(context.getUsingBundle()));
        }
    }

    /**
     * The expected pattern is
     * /some/resource/path.[general json view selector].[optional model name selector].json
     */
    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws IOException {
        if (this.serializer == null) {
            response.sendError(SC_SERVICE_UNAVAILABLE, "The JSON view service is not available.");
            LOGGER.warn("A client tried to call the JSON view servlet, but the service is unavailable because either jackson-core or jackson-databind are missing from this bundle's classpath. Responding with HTTP 503 Service unavailable.");
            return;
        }

        String etag = null;
        if (configuration.generateEtag()) {
            etag = "W/\"" + request.getResource().getResourceMetadata().getModificationTime() + "-" + request.getResource().getPath() + '"';
            Enumeration<String> clientEtags = request.getHeaders("If-None-Match");
            if (clientEtags.hasMoreElements()) {
                while (clientEtags.hasMoreElements()) {
                    if (etag.equals(clientEtags.nextElement())) {
                        response.setStatus(SC_NOT_MODIFIED);
                        return;
                    }
                }
            }
        }

        nestedMappingSupport.beginRecordingMappings();
        try {
            String[] selectors = request.getRequestPathInfo().getSelectors();
            Object model;

            if (selectors.length == 1) {
                model = modelResolver.resolveMostSpecificModel(request.getResource());
                if (model == null) {
                    response.sendError(SC_NOT_FOUND, "No model could be resolved for resource " + request.getResource().getPath());
                    return;
                }
            } else if (selectors.length == 2) {
                String modelName = selectors[1];

                if (!EXPECTED_MODEL_NAME.matcher(modelName).matches()) {
                    // XSS security check: Since we echo the model name if no model was found and the model name is user input, we must make sure
                    // only to echo or record input matching a specific format.
                    response.sendError(SC_BAD_REQUEST, "Invalid model name. The model name must match the pattern " + EXPECTED_MODEL_NAME.pattern());
                    return;
                }

                model = modelResolver.resolveMostSpecificModelWithName(request.getResource(), modelName);
                if (model == null) {
                    response.sendError(SC_NOT_FOUND, "No model with name " + modelName + " could be resolved for resource " + request.getResource().getPath());
                    return;
                }
            } else {
                response.sendError(SC_BAD_REQUEST, "Invalid selectors. The expected format is <json servlet selector>[.<optional model name>]");
                return;
            }

            response.setContentType("application/json");
            response.setHeader("Cache-Control", configuration.cacheControlHeader());
            if (etag != null) {
                response.setHeader("Etag", etag);
            }
            response.setCharacterEncoding(this.configuration.encoding());
            serializer.serialize(response.getWriter(), model);
        } finally {
            nestedMappingSupport.endRecordingMappings();
        }
    }

    @Override
    public void init() {
        LOGGER.info("Servlet instance started");
    }

    @Override
    public void destroy() {
        LOGGER.info("Servlet instance stopped");
    }

    @ObjectClassDefinition(
            name = "NEBA model JSON view servlet",
            description =
                    "Renders resources as JSON using a NEBA model." +
                            "The used model is either the most specific NEBA model for the requested resource's type or a model " +
                            "with the name specified in the selectors, provided that model is for a compatible resource type. " +
                            "The JSON view can thus be resolved using </resource/path>.<one of the configured selectors>[.<optional specific model name>].json")
    public @interface Configuration {
        @AttributeDefinition(
                name = "Encoding",
                description = "The encoding to use when serializing models to JSON. The JSON specification explicitly defines the encodings that can be used, thus this must be one of http://www.ietf.org/rfc/rfc4627.txt.",
                options = {
                        @Option(label = "UTF-8", value = "UTF-8"),
                        @Option(label = "UTF-16BE", value = "UTF-16BE"),
                        @Option(label = "UTF-16LE", value = "UTF-16LE"),
                        @Option(label = "UTF-32BE", value = "UTF-32BE"),
                        @Option(label = "UTF-32LE", value = "UTF-32LE")
                }
        )
        String encoding() default "UTF-8";

        @AttributeDefinition(
                name = "Servlet selectors",
                description = "The selectors this servlet is listening for. Note that 'model' is the default used by the Apache Sling Exporter Framework.")
        @SuppressWarnings("unused")
        String[] sling_servlet_selectors() default "model";

        @AttributeDefinition(
                name = "Resource types",
                description =
                        "If specified, this servlet will only serve JSON views for resource with one of the given types. " +
                                "By default, this servlet will serve requests for all resources by registering itself " +
                                "as the default servlet for the configured selector(s). Defaults to sling/servlet/default. " +
                                "Note that primary note types, such as nt:unstructured, are not supported by Sling Servlets.")
        @SuppressWarnings("unused")
        String[] sling_servlet_resourceTypes() default "sling/servlet/default";

        @AttributeDefinition(
                name = "Jackson features",
                description = "Enable or disable serialization or module features using the " +
                        "respective enumeration names and a boolean flag, " +
                        "for instance SerializationFeature.INDENT_OUTPUT=false or MapperFeature.SORT_PROPERTIES_ALPHABETICALLY=true.")
        String[] jacksonSettings() default "SerializationFeature.WRITE_DATES_AS_TIMESTAMPS=true";

        @AttributeDefinition(
                name = "Add :type attribute",
                description = "Automatically add the resource type for which the model was resolved in an attribute called ':type' to the generated JSON. " +
                        "This is useful e.g. to determine the frontend components responsible for rendering generated JSON.")
        boolean addTypeAttribute() default false;

        @AttributeDefinition(
                name = "Generate Etag",
                description = "Generate an Etag header based on the path and modification date of the request's resource. " +
                        "Enabling this must be done in combination with a cache-control header that allows caching (see below), " +
                        "e.g. 'private, max-age=86400, must-revalidate', which would allow in-browser caching for 24 hours.")
        boolean generateEtag() default false;

        @AttributeDefinition(
                name = "Cache-Control header",
                description = "Add the following Cache-Control HTTP header to all responses.")
        String cacheControlHeader() default "private, no-cache, no-store, must-revalidate";
    }
}
