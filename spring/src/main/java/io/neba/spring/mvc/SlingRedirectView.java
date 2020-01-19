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
package io.neba.spring.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Does not {@link javax.servlet.http.HttpServletResponse#encodeRedirectURL(String) encode}
 * the target URL: Sling is RESTful, i.e. requests should be stateless. Also, an appended session URL
 * is not part of the Sling URL spec. Lastly, sling incorrectly encodes external URLs, resulting in
 * "http(s)://" to be encoded as "_http(s)_".
 *
 * @author Olaf Otto
 */
public class SlingRedirectView extends RedirectView {

    public SlingRedirectView(String url, boolean contextRelative, boolean http10Compatible) {
        super(url, contextRelative, http10Compatible);
    }

    @Override
    protected void sendRedirect(@Nonnull HttpServletRequest request,
                                HttpServletResponse response,
                                @Nonnull String targetUrl, boolean http10Compatible) throws IOException {
        if (http10Compatible) {
            // Send status code 302 by default.
            response.sendRedirect(targetUrl);
        } else {
            HttpStatus statusCode = getHttp11StatusCode(request, response, targetUrl);
            response.setStatus(statusCode.value());
            response.setHeader("Location", targetUrl);
        }
    }
}
