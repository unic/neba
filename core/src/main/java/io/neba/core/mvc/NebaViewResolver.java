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

package io.neba.core.mvc;

import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;

import java.util.Locale;

/**
 * Supports "redirect:" and "forward:" {@link View} resolution from view name.
 *
 * @author Olaf Otto
 */
public class NebaViewResolver implements ViewResolver {
    public static final String REDIRECT_URL_PREFIX = "redirect:";
    public static final String FORWARD_URL_PREFIX = "forward:";

    /**
     * Resolves a {@link View} from the provided view name.
     *
     * @param viewName must not be <code>null</code>.
     */
    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (viewName == null) {
            throw new IllegalArgumentException("Method argument viewName must not be null.");
        }

        if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
            String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
            return new SlingRedirectView(redirectUrl, true, true);
        }
        if (viewName.startsWith(FORWARD_URL_PREFIX)) {
            String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
            return new InternalResourceView(forwardUrl);
        }
        return null;
    }
}
