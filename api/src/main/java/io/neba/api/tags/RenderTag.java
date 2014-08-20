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

package io.neba.api.tags;

import io.neba.api.rendering.BeanRenderer;
import io.neba.api.rendering.BeanRendererFactory;
import org.apache.sling.api.scripting.SlingScriptHelper;
import tldgen.Tag;
import tldgen.TagAttribute;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.DynamicAttributes;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.neba.api.Constants.DEFAULT_RENDERER_NAME;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Renders a bean using a {@link #setRenderer(String) customizable} {@link BeanRenderer}.
 * By default, the renderer with the name {@link io.neba.api.Constants#DEFAULT_RENDERER_NAME} is used.
 * Bean renderers and their names are configurable in the felix configuration console.
 * <p/>
 * <p>
 * Examples:<br />
 * <pre>
 *   &lt;mynamespace:render object="${m}" /&gt;
 *   &lt;mynamespace:render object="${m}" viewHint="teaser"/&gt;
 *   &lt;mynamespace:render object="${m}" viewHint="teaser" renderer="subsite.mydomain.com"/&gt;
 * </pre>
 * <p/>
 * All of the the attributes support expressions, thus the following also works:
 * <p/>
 * <pre>
 *   &lt;mynamespace:render object="${m.someProperty}" viewHint="${m.viewHint}"/&gt;
 * </pre>
 * </p>
 * <p/>
 * Furthermore, this tag supports {@link DynamicAttributes}, i.e. an unlimited number of custom attributes
 * not defined in the tag definition. These custom attributes and their corresponding values
 * are made available as key/value pairs in the rendering context.
 * <p/>
 * <p>
 * Example:
 * <pre>
 *   Tag:
 *   &lt;mynamespace:render object="${m}" cssClasses="teaser left first" /&gt;
 *
 *   In the velocity template, the following then resolves to "teaser left first":
 *   ${cssClasses}
 * </pre>
 * </p>
 *
 * @author Olaf Otto
 */
@Tag(description = "Renders arbitrary objects using a BeanRenderer obtained from the BeanRendererFactory service.")
public final class RenderTag extends TagWithBindings implements DynamicAttributes {
    private static final long serialVersionUID = 1572068336706874633L;
    private final Map<String, Object> dynamicAttributes = new HashMap<String, Object>();
    private String rendererName = DEFAULT_RENDERER_NAME;
    private String viewHint = null;
    private String variableNameOfResult = null;
    private Object object;

    @Override
    public int doEndTag() throws JspException {
        SlingScriptHelper scriptHelper = getScriptHelper();
        BeanRendererFactory factory = scriptHelper.getService(BeanRendererFactory.class);
        if (factory == null) {
            throw new IllegalStateException("No service of type " + BeanRendererFactory.class.getName() +
                    " could be obtained - is the NEBA core bundle present?");
        }
        BeanRenderer renderer = factory.get(this.rendererName);
        if (renderer == null) {
            throw new IllegalArgumentException("No bean renderer with the name '" + rendererName +
                    "' is configured. Please check the configuration of the bean renderer factory " +
                    "in the sling console.");
        }
        String renderedObject = null;
        if (this.object != null) {
            renderedObject = renderer.render(this.object, this.viewHint, this.dynamicAttributes);
        }
        if (renderedObject != null) {
            write(renderedObject);
        }
        return EVAL_PAGE;
    }

    private void write(String renderedObject) {
        if (!isBlank(this.variableNameOfResult)) {
            this.pageContext.setAttribute(this.variableNameOfResult, renderedObject);
        } else {
            try {
                this.pageContext.getOut().write(renderedObject);
            } catch (IOException e) {
                throw new RuntimeException("Unable to render " + this.object + " with view hint " + this.viewHint + ".", e);
            }
        }
    }

    @TagAttribute(
            description = "The renderer name as configured in the sling console. Must not " +
                    "be provided unless a different renderer then \"default\" is used, " +
                    "e.g. for providing different views for the same type in different applications.",
            required = false, runtimeValueAllowed = true)
    public void setRenderer(String rendererName) {
        this.rendererName = rendererName;
    }

    @TagAttribute(
            description = "The object to be rendered - must not be null.",
            required = true, runtimeValueAllowed = true)
    public void setObject(Object object) {
        this.object = object;
    }

    @TagAttribute(
            description = "A view hint that is attached to the view name derived from the objects type." +
                    " This is to create variants of a view." +
                    " Examples: \"small\", \"full\", \"sitemapentry\" or so.",
            required = false, runtimeValueAllowed = true)
    public void setViewHint(String viewHint) {
        this.viewHint = viewHint;
    }

    @Override
    public void setDynamicAttribute(String URI, String attributeName, Object value) throws JspException {
        String localName = attributeName;
        if (URI != null) {
            localName = URI + "." + attributeName;
        }
        this.dynamicAttributes.put(localName, value);
    }

    @TagAttribute(
            description = "If provided and not null or empty, the output of the" +
                    " tag is stored under the given variable name" +
                    " instead of being written to the response.",
            required = false, runtimeValueAllowed = true)
    public void setVar(String variableName) {
        this.variableNameOfResult = variableName;
    }
}
