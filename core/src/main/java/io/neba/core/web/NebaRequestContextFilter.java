/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.web;

import org.apache.sling.bgservlets.BackgroundHttpServletRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.RequestContextFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.util.ClassUtils.isPresent;

/**
 * A modified {@link RequestContextFilter} wrapping
 * {@link org.apache.sling.bgservlets.BackgroundHttpServletRequest sling background requests}
 * using the {@link BackgroundServletRequestWrapper} to prevent
 * {@link UnsupportedOperationException unsupported operation exceptions} when
 * {@link ServletRequestAttributes#requestCompleted()} attempts to access the
 * {@link HttpServletRequest#getSession() session}, which is unsupported by
 * {@link BackgroundHttpServletRequest background requests}.
 *
 * @author Olaf Otto
 * @see RequestContextFilter
 */
public class NebaRequestContextFilter extends RequestContextFilter {
    private static final boolean IS_BGSERVLETS_PRESENT = isPresent(
            "org.apache.sling.bgservlets.BackgroundHttpServletRequest",
            NebaRequestContextFilter.class.getClassLoader());

    private boolean threadContextInheritable = false;

    /**
     * Set whether to expose the LocaleContext and RequestAttributes as inheritable
     * for child threads (using an {@link java.lang.InheritableThreadLocal}).
     * <p>Default is "false", to avoid side effects on spawned background threads.
     * Switch this to "true" to enable inheritance for custom child threads which
     * are spawned during request processing and only used for this request
     * (that is, ending after their initial task, without reuse of the thread).
     * <p><b>WARNING:</b> Do not use inheritance for child threads if you are
     * accessing a thread pool which is configured to potentially add new threads
     * on demand (e.g. a JDK {@link java.util.concurrent.ThreadPoolExecutor}),
     * since this will expose the inherited context to such a pooled thread.
     */
    public void setThreadContextInheritable(boolean threadContextInheritable) {
        this.threadContextInheritable = threadContextInheritable;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {


        ServletRequestAttributes attributes = createServletRequestAttributes(request);
        initContextHolders(request, attributes);

        try {
            filterChain.doFilter(request, response);
        } finally {
            resetContextHolders();
            if (logger.isDebugEnabled()) {
                logger.debug("Cleared thread-bound request context: " + request);
            }
            attributes.requestCompleted();
        }
    }

    private ServletRequestAttributes createServletRequestAttributes(HttpServletRequest request) {
        if (IS_BGSERVLETS_PRESENT && request instanceof BackgroundHttpServletRequest) {
            return new ServletRequestAttributes(new BackgroundServletRequestWrapper(request));
        }

        return new ServletRequestAttributes(request);
    }

    private void initContextHolders(HttpServletRequest request, ServletRequestAttributes requestAttributes) {
        LocaleContextHolder.setLocale(request.getLocale(), this.threadContextInheritable);
        RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
        if (logger.isDebugEnabled()) {
            logger.debug("Bound request context to thread: " + request);
        }
    }

    private void resetContextHolders() {
        LocaleContextHolder.resetLocaleContext();
        RequestContextHolder.resetRequestAttributes();
    }
}
