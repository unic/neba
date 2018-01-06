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

package io.neba.api.tags;

import io.neba.api.services.ResourceModelResolver;
import javax.annotation.CheckForNull;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import tldgen.Tag;
import tldgen.TagAttribute;


import static io.neba.api.Constants.MODEL;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.sling.api.scripting.SlingBindings.SLING;

/**
 * @author Olaf Otto
 */
@Tag(description = "Adds the most specific @ResourceModel" +
                   " for the current resource under the key \"m\", if such a model exists.")
public final class DefineObjectsTag extends TagSupport {
    private static final long serialVersionUID = 3746304163438347809L;

    private boolean includeGenericBaseTypes = false;
    private String modelName;
    private String var;

    @Override
    public int doEndTag() throws JspException {
        provideMostSpecificResourceModel();
        return EVAL_PAGE;
    }

    @TagAttribute(description = "Whether to include models mapping to " +
            "generic base types such as \"nt:unstructured\" or " +
            "\"nt:base\". Defaults to false. Has no effect if a " +
            "modelName is provided.",
            runtimeValueAllowed = true)
    public void setIncludeGenericBaseTypes(boolean includeGenericBaseTypes) {
        this.includeGenericBaseTypes = includeGenericBaseTypes;
    }

    @TagAttribute(description = "The explicit model name of the resource model that shall " +
            "be provided for the resource. The targeted resource model must still be " +
            "declared for a resource type compatible with the resource's type. The searched " +
            "models will always include generic base models, regardless of whether " +
            "includeGenericBaseTypes is false.",
            runtimeValueAllowed = true)
    public void setUseModelNamed(@CheckForNull String name) {
        this.modelName = name;
    }

    @TagAttribute(description = "The variable name to publish the model under. " +
            "Defaults to the default model name 'm' if not defined, null or empty."  ,
            runtimeValueAllowed = true)
    public void setVar(@CheckForNull String var) {
        this.var = var;
    }

    private void provideMostSpecificResourceModel() {
        SlingScriptHelper scriptHelper = getScriptHelper();
        ResourceModelResolver modelProvider = scriptHelper.getService(ResourceModelResolver.class);

        if (modelProvider == null) {
            // Can be the case if called before / after provider lifetime, e.g.
            // when the application context is stopped. Fail fast.
            throw new IllegalStateException("The " + ResourceModelResolver.class.getSimpleName() + " must not be null." +
                                            " Is this tag used while the NEBA core is not started?");
        }

        Resource resource = getResource();
        Object model;
        if (!isBlank(this.modelName)) {
            model = modelProvider.resolveMostSpecificModelWithName(resource, this.modelName);
        } else if (this.includeGenericBaseTypes) {
            model = modelProvider.resolveMostSpecificModelIncludingModelsForBaseTypes(resource);
        } else {
            model = modelProvider.resolveMostSpecificModel(resource);
        }

        if (model != null) {
            String variableName = isBlank(this.var) ? MODEL : this.var;
            this.pageContext.setAttribute(variableName, model);
        }
    }

    private Resource getResource() {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) this.pageContext.getRequest();
        return slingRequest.getResource();
    }

    private SlingBindings getBindings() {
        return (SlingBindings) this.pageContext.getRequest().getAttribute(SlingBindings.class.getName());
    }

    private SlingScriptHelper getScriptHelper() {
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
