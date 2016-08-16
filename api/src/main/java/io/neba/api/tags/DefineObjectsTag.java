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

import io.neba.api.resourcemodels.ResourceModelProvider;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import tldgen.Tag;
import tldgen.TagAttribute;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import static io.neba.api.Constants.MODEL;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.sling.api.scripting.SlingBindings.SLING;

/**
 * @author Olaf Otto
 */
@Tag(description = "Adds the most specific @ResourceModel" +
                   " for the current resource under the key \"m\", if such a model exists.")
public final class DefineObjectsTag extends TagSupport {
    private static final long serialVersionUID = 3746304163438347809L;

    private boolean includeGenericBaseTypes = false;
    private String modelBeanName;

    @Override
    public int doEndTag() throws JspException {
        provideMostSpecificResourceModel();
        return EVAL_PAGE;
    }

    @TagAttribute(description = "Whether to include models mapping to " +
            "generic base types such as \"nt:unstructured\" or " +
            "\"nt:base\". Defaults to false. Has no effect if a " +
            "modelName is provided.",
            required = false, runtimeValueAllowed = true)
    public void setIncludeGenericBaseTypes(boolean includeGenericBaseTypes) {
        this.includeGenericBaseTypes = includeGenericBaseTypes;
    }

    @TagAttribute(description = "The explicit bean name of the resource model that shall " +
            "be provided for the resource. The targeted resource model must still be " +
            "declared for a resource type compatible with the resource's type. The searched " +
            "models will always include generic base models, regardless of whether " +
            "includeGenericBaseTypes is false.",
            required = false, runtimeValueAllowed = true)
    public void setUseModelNamed(String name) {
        this.modelBeanName = name;
    }

    private void provideMostSpecificResourceModel() {
        SlingScriptHelper scriptHelper = getScriptHelper();
        ResourceModelProvider modelProvider = scriptHelper.getService(ResourceModelProvider.class);

        if (modelProvider == null) {
            // Can be the case if called before / after provider lifetime, e.g.
            // when the application context is stopped. Fail fast.
            throw new IllegalStateException("The " + ResourceModelProvider.class.getSimpleName() + " must not be null." +
                                            " Is this tag used while the NEBA core is not started?");
        }

        Resource resource = getResource();
        Object model;
        if (!isBlank(this.modelBeanName)) {
            model = modelProvider.resolveMostSpecificModelWithBeanName(resource, this.modelBeanName);
        } else if (this.includeGenericBaseTypes) {
            model = modelProvider.resolveMostSpecificModelIncludingModelsForBaseTypes(resource);
        } else {
            model = modelProvider.resolveMostSpecificModel(resource);
        }

        if (model != null) {
            this.pageContext.setAttribute(MODEL, model);
        }
    }

    private Resource getResource() {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) this.pageContext.getRequest();
        return slingRequest.getResource();
    }

    protected SlingBindings getBindings() {
        return (SlingBindings) this.pageContext.getRequest().getAttribute(SlingBindings.class.getName());
    }

    protected SlingScriptHelper getScriptHelper() {
        SlingBindings bindings = getBindings();

        if (bindings == null) {
            throw new IllegalStateException("No " + SlingBindings.class.getName() +
                    " was found in the request, got null.");
        }

        SlingScriptHelper scriptHelper = (SlingScriptHelper) bindings.get(SLING);

        if (scriptHelper == null) {
            throw new IllegalStateException("No " + SlingScriptHelper.class.getName() +
                    " was found in the sling bindings, got null.");
        }
        return scriptHelper;
    }
}
