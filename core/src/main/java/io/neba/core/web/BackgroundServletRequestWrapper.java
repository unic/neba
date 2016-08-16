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
package io.neba.core.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

/**
 * Wraps {@link org.apache.sling.bgservlets.BackgroundHttpServletRequest background requests} to
 * prevent exceptions when clients attempt to obtain an existing session, i.e. when
 * {@link HttpServletRequest#getSession(boolean)} is invoked with <code>false</code>. In this case,
 * clients must expect a null return value and should handle this case gracefully.
 *
 * @author Olaf Otto
 */
public class BackgroundServletRequestWrapper extends HttpServletRequestWrapper {
    public BackgroundServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    /**
     * {@inheritDoc}
     *
     * @throws UnsupportedOperationException if <code>create</code> is <code>true</code> as
     * background requests do not support sessions.
     */
    @Override
    public HttpSession getSession(boolean create) {
        if (create) {
            throw new UnsupportedOperationException("Cannot create session: This is a background servlet request.");
        }
        return null;
    }
}
