/*
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
*/

package io.neba.core.resourcemodels.registration;

import io.neba.core.util.OsgiModelSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;


import static io.neba.core.util.BundleUtil.displayNameOf;
import static io.neba.core.util.ClassHierarchyIterator.hierarchyOf;
import static io.neba.core.util.JsonUtil.toJson;
import static java.lang.Character.isUpperCase;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.sling.api.resource.ResourceUtil.isNonExistingResource;
import static org.apache.sling.api.resource.ResourceUtil.isSyntheticResource;
import static org.apache.sling.api.resource.ResourceUtil.resourceTypeToPath;

/**
 * Shows a table with all detected type -&gt; model mappings in the felix console.
 *
 * @author Olaf Otto
 */
@Service(Servlet.class)
@Component
@Properties({
        @Property(name = "felix.webconsole.label", value = ModelRegistryConsolePlugin.LABEL),
        @Property(name = "service.description", value="Provides a felix console plugin listing all registered @ResourceModel's."),
        @Property(name = "service.vendor", value="neba.io")
})
public class ModelRegistryConsolePlugin extends AbstractWebConsolePlugin {
    static final String LABEL = "modelregistry";
    private static final String PREFIX_STATIC = "/static";
    private static final long serialVersionUID = -8676958166611686979L;
    private static final String API_PATH = "/api";
    private static final String API_FILTER = "/filter";
    private static final String API_RESOURCES = "/resources";
    private static final String API_COMPONENTICON = "/componenticon";
    private static final String API_MODELTYPES = "/modeltypes";
    private static final String PARAM_TYPENAME = "modelTypeName";
    private static final String PARAM_PATH = "path";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private ModelRegistry registry;

    @SuppressWarnings("unused")
    public String getCategory() {
        return "NEBA";
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getTitle() {
        return "Model registry";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String suffix = substringAfter(req.getRequestURI(), req.getServletPath() + "/" + getLabel());
        if (!isBlank(suffix) && suffix.startsWith(API_PATH)) {
            handleApiCall(suffix.substring(API_PATH.length()), req, res);
            return;
        }
        super.doGet(req, res);
    }


    private void handleApiCall(String apiIdentifier, HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (apiIdentifier.startsWith(API_COMPONENTICON)) {
            spoolComponentIcon(res, apiIdentifier);
            return;
        }

        res.setContentType("application/json;charset=UTF-8");

        if (apiIdentifier.startsWith(API_FILTER)) {
            provideFilteredModelRegistryView(req, res);
            return;
        }

        if (apiIdentifier.startsWith(API_RESOURCES)) {
            provideMatchingResourcePaths(req, res);
            return;
        }

        if (apiIdentifier.startsWith(API_MODELTYPES)) {
            provideAllModelTypes(res);
        }
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        writeHeadnavigation(res);

        PrintWriter writer = res.getWriter();
        writeScriptIncludes(res);

        writer.write("<table id=\"plugin_table\" class=\"nicetable tablesorter noauto\">");
        writer.write("<thead><tr><th>Type</th><th>Model type</th><th>Model name</th><th>Source bundle</th></tr></thead>");
        writer.write("<tbody>");
        for (Entry<String, Collection<OsgiModelSource<?>>> entry : this.registry.getTypeMappings().entrySet()) {
            for (OsgiModelSource<?> source : entry.getValue()) {
                String sourceBundleName = displayNameOf(source.getBundle());

                writer.write("<tr data-modeltype=\"" + source.getModelType().getName() + "\">");
                String resourceType = buildCrxDeLinkToResourceType(req, entry.getKey());
                writer.write("<td>" + resourceType + "</td>");
                writer.write("<td>" + source.getModelType().getName() + "</td>");
                writer.write("<td>" + source.getModelName() + "</td>");
                writer.write("<td><a href=\"bundles/" + source.getBundleId() + "\" " +
                        "title=\"" + sourceBundleName + "\">" + source.getBundleId() + "</a></td>");
                writer.write("</tr>");
            }
        }
        writer.write("</tbody>");
        writer.write("</table>");
    }

    private String buildCrxDeLinkToResourceType(HttpServletRequest request, String type) {
        ResourceResolver resolver = getResourceResolver();
        try {
            String path = resourceTypeToPath(type);
            Resource resource = null;
            Resource iconResource = null;
            for (String searchPath : resolver.getSearchPath()) {
                resource = resolver.getResource(searchPath + path);
                if (resource != null && !isNonExistingResource(resource) && !isSyntheticResource(resource)) {
                    iconResource = resource.getChild("icon.png");
                    break;
                }
            }
            return resource != null ? "<a href=\"" + request.getContextPath() +
                    "/crx/de/#" + resource.getPath() + "\" " +
                    "class=\"crxdelink\">"
                    + "<img class=\"componentIcon\" src=\""
                    + getLabel() + API_PATH + API_COMPONENTICON + (iconResource == null ? "" : resource.getPath())
                    + "\"/>"
                    + type + "</a>" :
                    "<span class=\"unresolved\">" + type + "</span>";
        } finally {
            resolver.close();
        }
    }

    private void provideAllModelTypes(HttpServletResponse res) throws IOException {
        Set<String> typeNames = new HashSet<>();
        for (OsgiModelSource<?> source : this.registry.getModelSources()) {
            for (Class<?> type : hierarchyOf(source.getModelType())) {
                if (type == Object.class) {
                    continue;
                }
                typeNames.add(type.getName());
            }
        }

        res.getWriter().write(toJson(typeNames));
    }

    private void provideMatchingResourcePaths(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String path = req.getParameter(PARAM_PATH);
        if (isEmpty(path) || path.charAt(0) != '/') {
            return;
        }

        ResourceResolver resolver = getResourceResolver();
        try {
            int idx = path.lastIndexOf('/');
            Resource parent;
            String prefix = "";
            if (idx < 1) {
                parent = resolver.getResource("/");
                prefix = path.substring(1);
            } else {
                parent = resolver.getResource(path.substring(0, idx));
                if (idx < path.length() - 1) {
                    prefix = path.substring(idx + 1);
                }
            }

            if (parent == null) {
                return;
            }

            Collection<String> resourcePaths = new LinkedList<>();
            Iterator<Resource> children = parent.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                if (prefix.isEmpty() || child.getName().startsWith(prefix)) {
                    resourcePaths.add(child.getPath());
                }
            }
            res.getWriter().write(toJson(resourcePaths));
        } finally {
            resolver.close();
        }
    }

    @SuppressWarnings("deprecation")
    private ResourceResolver getResourceResolver() {
        try {
            // A resource resolver with administrative privileges is required here. Registering a non-admin system account
            // would require administrative privileges, thus beating the point of non using the admin account.
            return this.resourceResolverFactory.getAdministrativeResourceResolver(null);
        } catch (LoginException e) {
            throw new IllegalStateException(e);
        }
    }

    private void provideFilteredModelRegistryView(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String modelTypePrefix = req.getParameter(PARAM_TYPENAME);
        String resourcePath = req.getParameter(PARAM_PATH);

        Collection<OsgiModelSource<?>> types;
        if (isEmpty(resourcePath)) {
            types = this.registry.getModelSources();
        } else {
            types = resolveModelTypesFor(resourcePath);
        }

        Set<String> matchingModelTypeNames = new HashSet<>(64);
        String typeNameCandidate = substringAfterLast(modelTypePrefix, ".");

        boolean exactMatch = !isEmpty(typeNameCandidate) && isUpperCase(typeNameCandidate.charAt(0));

        for (OsgiModelSource<?> source : types) {
            if (modelTypePrefix == null) {
                matchingModelTypeNames.add(source.getModelType().getName());
            } else {
                for (Class<?> type : hierarchyOf(source.getModelType())) {
                    String typeName = type.getName();
                    if ((exactMatch ? typeName.equals(modelTypePrefix) : typeName.startsWith(modelTypePrefix))) {
                        matchingModelTypeNames.add(source.getModelType().getName());
                        break;
                    }
                }
            }
        }

        res.getWriter().write(toJson(matchingModelTypeNames));
    }

    private Collection<OsgiModelSource<?>> resolveModelTypesFor(String resourcePath) {
        Collection<OsgiModelSource<?>> types = new ArrayList<>(64);

        if (!isEmpty(resourcePath)) {
            ResourceResolver resolver = getResourceResolver();
            try {
                Resource resource = resolver.getResource(resourcePath);
                if (resource == null) {
                    return types;
                }
                Collection<LookupResult> lookupResults = this.registry.lookupAllModels(resource);
                if (lookupResults == null) {
                    return types;
                }
                types.addAll(lookupResults.stream().map(LookupResult::getSource).collect(Collectors.toList()));
            } finally {
                resolver.close();
            }
        }
        return types;
    }

    public URL getResource(String path) {
        String internalPath = substringAfter(path, "/" + getLabel());
        if (startsWith(internalPath, PREFIX_STATIC)) {
            return getClass().getResource("/META-INF/consoleplugin/modelregistry" + internalPath);
        }
        return null;
    }

    private void writeScriptIncludes(HttpServletResponse response) throws IOException {
        response.getWriter().write("<script src=\"" + getLabel() + "/static/script.js\"></script>");
    }

    private void writeHeadnavigation(HttpServletResponse response) throws IOException {
        String template = readTemplateFile("/META-INF/consoleplugin/modelregistry/templates/head.html");
        response.getWriter().printf(template, getNumberOfModels());
    }

    private Object getNumberOfModels() {
        return this.registry.getModelSources().size();
    }

    private void spoolComponentIcon(HttpServletResponse response, String suffix) throws IOException {
        response.setContentType("image/png");
        String iconPath = suffix.substring(API_COMPONENTICON.length());
        if (iconPath.isEmpty()) {
            InputStream in = getClass().getResourceAsStream("/META-INF/consoleplugin/modelregistry/static/noicon.png");
            try {
                copy(in, response.getOutputStream());
            } finally {
                closeQuietly(in);
            }
            return;
        }

        ResourceResolver resolver = getResourceResolver();
        InputStream in = null;
        try {
            Resource componentIcon = resolver.getResource(iconPath + "/icon.png");
            if (componentIcon != null) {
                in = componentIcon.adaptTo(InputStream.class);
                if (in != null) {
                    copy(in, response.getOutputStream());
                }
            }
        } finally {
            resolver.close();
            closeQuietly(in);
        }
    }
}
