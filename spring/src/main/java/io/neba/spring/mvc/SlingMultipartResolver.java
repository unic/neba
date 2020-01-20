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

import org.apache.sling.api.SlingHttpServletRequest;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

/**
 * @author Olaf Otto
 */
public class SlingMultipartResolver implements MultipartResolver {

    @Override
    public boolean isMultipart(@Nonnull HttpServletRequest request) {
        return request != null && isMultipartContent(request);
    }

    @Override
    @Nonnull
    public MultipartHttpServletRequest resolveMultipart(@Nonnull HttpServletRequest request) throws MultipartException {
        if (request == null) {
            throw new IllegalArgumentException("Method argument request must not be null.");
        }
        if (!(request instanceof SlingHttpServletRequest)) {
            throw new IllegalArgumentException("Method argument request must be a " +
                    SlingHttpServletRequest.class + ", but is a " + request.getClass() + ".");
        }
        return new MultipartSlingHttpServletRequest((SlingHttpServletRequest) request);
    }

    @Override
    public void cleanupMultipart(@Nonnull MultipartHttpServletRequest request) {
        // This is done by sling.
    }

    private static boolean isMultipartContent(HttpServletRequest request) {
        return equalsIgnoreCase(request.getMethod(), "POST") &&
                startsWithIgnoreCase(request.getContentType(), "multipart/");
    }
}
